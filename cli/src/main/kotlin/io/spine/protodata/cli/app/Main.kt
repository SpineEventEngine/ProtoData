/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.protodata.cli.app

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MutuallyExclusiveGroupException
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.code.proto.FileSet
import io.spine.option.OptionsProvider
import io.spine.protobuf.outerClass
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.config.Configuration
import io.spine.protodata.config.ConfigurationFormat
import io.spine.protodata.cli.ConfigFileParam
import io.spine.protodata.cli.ConfigFormatParam
import io.spine.protodata.cli.ConfigValueParam
import io.spine.protodata.cli.OptionProviderParam
import io.spine.protodata.cli.Parameter
import io.spine.protodata.cli.PluginParam
import io.spine.protodata.cli.RendererParam
import io.spine.protodata.cli.RequestParam
import io.spine.protodata.cli.SourceRootParam
import io.spine.protodata.cli.TargetRootParam
import io.spine.protodata.cli.UserClasspathParam
import io.spine.protodata.renderer.SourceFileSet
import io.spine.tools.code.manifest.Version
import java.io.File
import java.io.File.pathSeparator
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Launches the CLI application.
 *
 * When the application is done, or an unhandled error occurs, exits the process.
 */
public fun main(args: Array<String>): Unit =
    Run(readVersion()).main(args)

private fun readVersion(): String = Version.fromManifestOf(Run::class.java).value

/**
 * The main CLI command which performs the ProtoData code generation tasks.
 *
 * The command accepts class names for the service provider interface implementations via the CLI
 * parameters, such as `--plugin`, `--renderer`, and `--option-provider`, all of which
 * can be repeated parameters, if required.
 *
 * Then, using the classpath of the app and the user classpath supplied via the `--user-classpath`
 * parameter, loads those classes.
 *
 * `Code Generation` context accept Protobuf compiler events, regarding the Protobuf types, listed
 * in the `CodeGeneratorRequest.file_to_generate` as loaded from the `--request` parameter.
 *
 * Finally, the renderers apply required changes to the source set with the root path, supplied in
 * the `--source-root` parameter.
 */
@Suppress("TooManyFunctions") // It is OK for `main` entry point.
internal class Run(version: String) : CliktCommand(
    name = "protodata",
    help = "ProtoData tool helps build better multi-platform code generation." +
            System.lineSeparator() +
            "Version ${version}.",
    epilog = "https://github.com/SpineEventEngine/ProtoData/",
    printHelpOnEmptyArgs = true
) {
    private fun Parameter.toOption(completionCandidates: CompletionCandidates? = null) = option(
        name, shortName,
        help = help,
        completionCandidates = completionCandidates
    )

    private fun NullableOption<Path, Path>.splitPaths() = split(pathSeparator)

    private val plugins: List<String>
            by PluginParam.toOption().multiple()

    private val renderers: List<String>
            by RendererParam.toOption().multiple(default = listOf())

    private val optionProviders: List<String>
            by OptionProviderParam.toOption().multiple()

    private val codegenRequestFile: File
            by RequestParam.toOption().file(
                mustExist = true,
                canBeDir = false,
                canBeSymlink = false,
                mustBeReadable = true
            ).required()

    private val sourceRoots: List<Path>?
            by SourceRootParam.toOption().path(
                canBeFile = false,
                canBeSymlink = false
            ).splitPaths()

    private val targetRoots: List<Path>?
            by TargetRootParam.toOption().path(
                canBeFile = false,
                canBeSymlink = false
            ).splitPaths()

    private val classPath: List<Path>?
            by UserClasspathParam.toOption().path(
                mustExist = true,
                mustBeReadable = true
            ).splitPaths()

    private val configurationFile: Path?
            by ConfigFileParam.toOption().path(
                mustExist = true,
                mustBeReadable = true,
                canBeDir = false,
                canBeSymlink = false
            )

    private val configurationValue: String?
            by ConfigValueParam.toOption()

    private val configurationFormat: String? by ConfigFormatParam.toOption(
        completionCandidates = CompletionCandidates.Fixed(
            ConfigFormatParam.options().toSet()
        )
    )

    override fun run() {
        val sources = createSourceFileSets()
        val plugins = loadPlugins()
        val renderer = loadRenderers()
        val registry = createRegistry()
        val codegenRequest = loadRequest(registry)
        val config = resolveConfig()
        Pipeline(plugins, renderer, sources, codegenRequest, config)()
    }

    private fun resolveConfig(): Configuration? {
        val hasFile = configurationFile != null
        val hasValue = configurationValue != null
        val hasFormat = configurationFormat != null
        if (hasFile && hasValue) {
            throw MutuallyExclusiveGroupException(
                listOf(ConfigFileParam.name, ConfigValueParam.name)
            )
        }
        checkUsage(hasValue == hasFormat) {
            "Options `${ConfigValueParam.name}` and `${ConfigFileParam.name}`" +
                    " must be used together."
        }
        return when {
            hasFile -> Configuration.file(configurationFile!!)
            hasValue -> {
                val format = ConfigurationFormat.valueOf(configurationFormat!!.uppercase())
                Configuration.rawValue(configurationValue!!, format)
            }
            else -> null
        }
    }

    private fun loadRequest(extensions: ExtensionRegistry = ExtensionRegistry.getEmptyRegistry()) =
        codegenRequestFile.inputStream().use {
            CodeGeneratorRequest.parseFrom(it, extensions)
        }

    private fun createRegistry(): ExtensionRegistry {
        val optionsProviders = loadOptions()
        val registry = ExtensionRegistry.newInstance()
        optionsProviders.forEach { it.registerIn(registry) }
        return registry
    }

    private fun createSourceFileSets(): List<SourceFileSet> {
        checkPaths()
        val sources = sourceRoots
        val targets = (targetRoots ?: sources)!!
        return sources
            ?.zip(targets)
            ?.filter { (s, _) -> s.exists() }
            ?.map { (s, t) -> SourceFileSet.from(s, t) }
            ?: targets.oneSetWithNoFiles()
    }

    private fun checkPaths() {
        checkUsage(sourceRoots != null || targetRoots != null) {
            "Either source root or target root or both must be set."
        }
        if (sourceRoots == null) {
            checkUsage(targetRoots!!.size == 1) {
                "When not providing a source directory, only one target directory must be present."
            }
        }
        if (sourceRoots != null && targetRoots != null) {
            checkUsage(sourceRoots!!.size == targetRoots!!.size) {
                "Mismatched amount of directories. Given ${sourceRoots!!.size} sources " +
                        "and ${targetRoots!!.size} targets."
            }
        }
    }

    private fun loadPlugins() = load(PluginBuilder(), plugins)

    private fun loadRenderers() = load(RendererBuilder(), renderers)

    private fun loadOptions(): List<OptionsProvider> {
        val providers = load(OptionsProviderBuilder(), optionProviders)
        val request = loadRequest()
        val files: FileSet = FileSet.of(request.protoFileList)
        val fileProviders = filterOptionFiles(files)
        val allProviders = providers.toMutableList()
        allProviders.addAll(fileProviders)
        allProviders.add(SpineOptionsProvider())
        return allProviders
    }

    private fun filterOptionFiles(files: FileSet): Sequence<FileOptionsProvider> {
        val fileProviders = files.files()
            .filter { it.extensions.isNotEmpty() }
            // Filter out files that do not have outer classes yet.
            // These are `.proto` files being processed by ProtoData that contain
            // option definitions. We cannot use these files because there is no binary Java
            // code generated for them at this stage. Because of this they cannot be added to
            // an `ExtensionRegistry` later.
            .filter { it.outerClass != null }
            .map(::FileOptionsProvider)
            .asSequence()
        return fileProviders
    }

    private fun <T: Any> load(builder: ReflectiveBuilder<T>, classNames: List<String>): List<T> {
        val classLoader = Thread.currentThread().contextClassLoader
        return classNames.map { builder.tryCreate(it, classLoader) }
    }

    private fun <T : Any> ReflectiveBuilder<T>.tryCreate(
        className: String,
        classLoader: ClassLoader
    ): T {
        try {
            return createByName(className, classLoader)
        } catch (e: ClassNotFoundException) {
            printError(e.stackTraceToString())
            printError(e.message)
            printError("Please add the required class `$className` to the user classpath.")
            if (classPath != null) {
                printError("User classpath contains: `${classPath!!.joinToString(pathSeparator)}`.")
            }
            exitProcess(1)
        }
    }
}

/**
 * Prints the given error [message] to the screen.
 */
private fun printError(message: String?) = TermUi.echo(message, err = true)

/**
 * Creates a list that contain a single, empty source set.
 */
private fun List<Path>.oneSetWithNoFiles(): List<SourceFileSet> =
    listOf(SourceFileSet.empty(first()))

/**
 * Throws an [UsageError] with the result of calling [lazyMessage] if the [condition] isn't met.
 */
private inline fun checkUsage(condition: Boolean, lazyMessage: () -> Any) {
    if (condition.not()) {
        val message = lazyMessage()
        throw UsageError(message.toString())
    }
}