/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata.cli

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MutuallyExclusiveGroupException
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.code.proto.FileName
import io.spine.code.proto.FileSet
import io.spine.io.Resource
import io.spine.protodata.Pipeline
import io.spine.protodata.config.Configuration
import io.spine.protodata.config.ConfigurationFormat
import io.spine.protodata.config.ConfigurationFormat.JSON
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.config.ConfigurationFormat.PROTO_JSON
import io.spine.protodata.config.ConfigurationFormat.YAML
import io.spine.protodata.option.OptionsProvider
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceSet
import java.io.File
import java.io.File.pathSeparator
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * The resource file containing the version of ProtoData.
 *
 * Such a resource name might be duplicated in other places in ProtoData code base. The reason for
 * this is to avoid creating an extra dependencies. Search by the string value of this constant
 * when making changes.
 */
private const val VERSION_FILE_NAME = "version.txt"

/**
 * Launches the CLI application.
 *
 * When the application is done or an unhandled error occurs, exits the process.
 */
public fun main(args: Array<String>): Unit =
    Run(readVersion()).main(args)

private fun readVersion(): String {
    val versionFile = Resource.file(VERSION_FILE_NAME, Run::class.java.classLoader)
    return versionFile.read()
}

/**
 * The main CLI command which performs the ProtoData code generation tasks.
 *
 * The command accepts class names for the service provider interface implementations via the CLI
 * parameters, such as `--plugin`, `--renderer`, `--option-provider`, and `--options`, all of which
 * can be repeated parameters, if required. Then, using the classpath of the app and
 * the user classpath supplied via the `--user-classpath` parameter, loads those classes.
 * `Code Generation` context accept Protobuf compiler events, regarding the Protobuf types, listed
 * in the `CodeGeneratorRequest.file_to_generate` as loaded from the `--request` parameter. Finally,
 * the renderers apply required changes to the source set with the root path, supplied in
 * the `--source-root` parameter.
 */
internal class Run(version: String) : CliktCommand(
    name = "protodata",
    help = "ProtoData tool helps build better multi-platform code generation. Version ${version}.",
    epilog = "https://github.com/SpineEventEngine/ProtoData/",
    printHelpOnEmptyArgs = true
) {

    private val plugins: List<String> by option("--plugin", "-p", help = """
        The name of a Java class, a subtype of `${Plugin::class.qualifiedName}`.
        There can be multiple providers. To pass more then one value, type:
           `<...> -p com.foo.MyEntitiesPlugin -p com.foo.OtherEntitiesPlugin`
    """.trimIndent()).multiple()
    private  val renderers: List<String> by option("--renderer", "-r", help = """
        The name of a Java class, a subtype of `${Renderer::class.qualifiedName}`.
        There can only be multiple renderers. To pass more then one value, type:
           `<...> -r com.foo.MyJavaRenderer -r com.foo.MyKotlinRenderer`
    """.trimIndent()).multiple(required = true)
    private val optionProviders: List<String> by option("--option-provider", "--op",
        help = """
        The name of a Java class, a subtype of `${OptionsProvider::class.qualifiedName}`.
        There can be multiple providers. To pass more then one value, type:
           `<...> --op com.foo.TypeOptionsProvider --op com.foo.FieldOptionsProvider`
    """.trimIndent()).multiple()
    private val options: List<String> by option("--options", "-o", help = """
        A file which defines custom Protobuf options.
        There can be multiple files. To pass more then one value, type:
            `<...> -o acme/base/options.proto -o example/other_options.proto`
    """.trimIndent()).multiple()
    private val codegenRequestFile: File by option("--request", "-t", help =
    "The path to the binary file containing a serialized instance of " +
            "`${CodeGeneratorRequest.getDescriptor().name}`."
    ).file(
        mustExist = true,
        canBeDir = false,
        canBeSymlink = false,
        mustBeReadable = true
    ).required()
    private val sourceRoot: Path? by option("--source-root", "--src", help = """
        The path to a directory which contains the source files to be processed.
        Skip this argument if there is no initial source to modify.
    """.trimIndent()
    ).path(
        mustExist = true,
        canBeFile = false,
        canBeSymlink = false
    )
    private val targetRoot: Path? by option("--target-root", "--destination", "-d", help = """
        The path where the processed files should be placed.
        May be the same as `--sourceRoot`. For editing files in-place, skip this option. 
    """.trimIndent()
    ).path(
        canBeFile = false,
        canBeSymlink = false
    )
    private val classPath: List<Path>? by option("--user-classpath" ,"--ucp", help = """
        The user classpath which contains all `--renderer` classes, user-defined policies, views,
        events, etc., as well as all their dependencies, which are not included as a part of
        the ProtoData library. This may be omitted if the classes are already present in
        the ProtoData classpath. May be one path to a JAR, a ZIP, or a directory. Or may be many
        paths separated by the `$pathSeparator` separator char (system-dependent).
    """.trimIndent()
    ).path(
        mustExist = true,
        mustBeReadable = true
    ).split(pathSeparator)
    private val configurationFile: Path? by option(ConfigOpt.FILE, "-c", help = """
        File which contains the custom configuration for ProtoData.

        May be a JSON, a YAML, or a binary Protobuf file.
        JSON files must have `.json` extension.
        JSON files with Protobuf JSON format must have `.pb.json` extension.
        YAML files must have `.yml` or `.yaml` extension.
        Protobuf binary files must have `.pb` or `.bin` extension. Messages must not be delimited.
    """.trimIndent()
    ).path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false,
        canBeSymlink = false
    )
    private val configurationValue: String? by option(ConfigOpt.VALUE, "--cv", help = """
        Custom configuration for ProtoData.
        May be a JSON or a YAML.
        Must be used alongside with `--configuration-format`
    """.trimIndent())
    private val configurationFormat: String? by option(ConfigOpt.FORMAT, "--cf", help = """
        The format of the custom configuration.
        Must be one of: `yaml`, `json`, `proto_json`, `plain`.
        Must be used alongside with `--configuration-value`.
    """.trimIndent(), completionCandidates = CompletionCandidates.Fixed(
        setOf(YAML, JSON, PROTO_JSON, PLAIN).map { it.name.lowercase() }.toSet()
    ))

    private object ConfigOpt {

        const val FILE = "--configuration-file"
        const val VALUE = "--configuration-value"
        const val FORMAT = "--configuration-format"
    }

    override fun run() {
        val sourceSet = createSourceSet()
        val plugins = loadPlugins()
        val renderer = loadRenderers()
        val registry = createRegistry()
        val codegenRequest = loadRequest(registry)
        val config = resolveConfig()
        Pipeline(plugins, renderer, sourceSet, codegenRequest, config)()
    }

    private fun resolveConfig(): Configuration? {
        val hasFile = configurationFile != null
        val hasValue = configurationValue != null
        val hasFormat = configurationFormat != null
        if (hasFile && hasValue) {
            throw MutuallyExclusiveGroupException(
                listOf(ConfigOpt.FILE, ConfigOpt.VALUE)
            )
        }
        if (hasValue != hasFormat) {
            throw UsageError(
                "Options `${ConfigOpt.VALUE}` and `${ConfigOpt.FORMAT}` must be used together."
            )
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
        optionsProviders.forEach { it.dumpTo(registry) }
        return registry
    }

    private fun createSourceSet(): SourceSet {
        checkPaths()
        val source = sourceRoot
        val target = (targetRoot ?: source)!!
        return if (source == null) {
            SourceSet.empty(target)
        } else {
            SourceSet.from(source, target)
        }
    }

    private fun checkPaths() {
        if (sourceRoot == null && targetRoot == null) {
            throw UsageError("Either source root or target root or both must be set.")
        }
    }

    private fun loadPlugins() =
        load(PluginBuilder(), plugins)

    private fun loadRenderers() =
        load(RendererBuilder(), renderers)

    private fun loadOptions(): List<OptionsProvider> {
        val providers = load(OptionsProviderBuilder(), optionProviders)
        val request = loadRequest()
        val files: FileSet = FileSet.of(request.protoFileList)
        val fileProviders = options
            .asSequence()
            .map(FileName::of)
            .mapNotNull { name -> files.findOptionFile(name) }
            .map(::FileOptionsProvider)
        val allProviders = providers.toMutableList()
        allProviders.addAll(fileProviders)
        return allProviders
    }

    private fun FileSet.findOptionFile(name: FileName): FileDescriptor? {
        val found = tryFind(name).orElse(null)
        if (found == null) {
            echo("WARNING. Option file `$name` not found.")
        }
        return found
    }

    private fun <T: Any> load(builder: ReflectiveBuilder<T>, classNames: List<String>): List<T> {
        val classLoader = Thread.currentThread().contextClassLoader
        return classNames.map { builder.tryCreate(it, classLoader) }
    }

    private fun <T: Any> ReflectiveBuilder<T>.tryCreate(className: String,
                                                        classLoader: ClassLoader): T {
        try {
            return createByName(className, classLoader)
        } catch (e: ClassNotFoundException) {
            error(e.message)
            error("Please add the required class `$className` to the user classpath.")
            if (classPath != null) {
                error("User classpath contains: `${classPath!!.joinToString(pathSeparator)}`.")
            }
            exitProcess(1)
        }
    }

    private fun error(msg: String?) {
        echo(msg, err = true)
    }
}
