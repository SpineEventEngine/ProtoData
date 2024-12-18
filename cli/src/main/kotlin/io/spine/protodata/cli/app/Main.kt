/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.code.proto.parse
import io.spine.logging.Level
import io.spine.logging.WithLogging
import io.spine.logging.context.LogLevelMap
import io.spine.logging.context.ScopedLoggingContext
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.cli.DebugLoggingParam
import io.spine.protodata.cli.InfoLoggingParam
import io.spine.protodata.cli.Parameter
import io.spine.protodata.cli.PluginParam
import io.spine.protodata.cli.RequestParam
import io.spine.protodata.cli.SettingsDirParam
import io.spine.protodata.cli.SourceRootParam
import io.spine.protodata.cli.TargetRootParam
import io.spine.protodata.cli.UserClasspathParam
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.string.Separator
import io.spine.string.ti
import io.spine.tools.code.manifest.Version
import java.io.File
import java.io.File.pathSeparator
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * Launches the CLI application.
 *
 * When the application is done exists the process with the code `0`.
 * If an unhandled error occurs, exits the process with the code `-1`.
 */
@Suppress(
    "TooGenericExceptionCaught", // We do want the most generic type thrown.
    "PrintStackTrace"
)
public fun main(args: Array<String>) {
    try {
        val version = readVersion()
        val run = Run(version)
        run.main(args)
        exitProcess(0)
    } catch (e: Throwable) {
        System.err.run {
            println("Exception caught in ProtoData `main()`:")
            println("```")
            e.printStackTrace(this)
            println("```")
        }
        exitProcess(-1)
    }
}

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
@Suppress("TooManyFunctions") // It is OK for the `main` entry point.
internal class Run(version: String) : CliktCommand(
    name = "protodata",
    help = "ProtoData tool helps build better multi-platform code generation." +
            Separator.nl() +
            "Version $version.",
    epilog = "https://github.com/SpineEventEngine/ProtoData/",
    printHelpOnEmptyArgs = true
), WithLogging {

    private fun Parameter.toOption(cc: CompletionCandidates? = null) = option(
        name, shortName,
        help = help,
        completionCandidates = cc
    )

    private fun NullableOption<Path, Path>.splitPaths() = split(pathSeparator)

    private val plugins: List<String>
            by PluginParam.toOption().multiple()

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
            ).splitPaths().required()

    private val classpath: List<Path>?
            by UserClasspathParam.toOption().path(
                mustExist = true,
                mustBeReadable = true
            ).splitPaths()

    @Suppress("unused")
    private val settingsDir: Path
            by SettingsDirParam.toOption().path(
                mustExist = true,
                mustBeReadable = true,
                canBeDir = true,
                canBeSymlink = false
            ).required()

    private val debug: Boolean by DebugLoggingParam.toOption().flag(default = false)

    private val info: Boolean by InfoLoggingParam.toOption().flag(default = false)

    private val loggingLevel: Level by lazy {
        checkUsage(!(debug && info)) {
            "Debug and info logging levels cannot be enabled at the same time."
        }
        when {
            debug -> Level.DEBUG
            info -> Level.INFO
            else -> Level.WARNING
        }
    }

    override fun run() {
        if (loggingLevel == Level.WARNING) {
            doRun()
        } else {
            val logLevelMap = LogLevelMap.create(mapOf(), loggingLevel)
            val context = ScopedLoggingContext.newContext().withLogLevelMap(logLevelMap)
            context.execute {
                doRun()
            }
        }
    }

    private fun doRun() {
        val sources = createSourceFileSets()
        val plugins = loadPlugins()
        val request = loadRequest()
        val dir = SettingsDirectory(settingsDir)
        logger.atDebug().log { """
            Starting code generation with the following arguments:
              - plugins: ${plugins.joinToString()}
              - request
                  - files to generate: ${request.fileToGenerateList.joinToString()}
                  - parameter: ${request.parameter}
              - settings dir: ${settingsDir}.
            """.ti()
        }
        val pipeline = Pipeline(
            plugins = plugins,
            sources = sources,
            request = request,
            settings = dir
        )
        pipeline()
    }

    private fun loadRequest(): CodeGeneratorRequest {
        return codegenRequestFile.inputStream().use {
            CodeGeneratorRequest::class.parse(it)
        }
    }

    private fun createSourceFileSets(): List<SourceFileSet> {
        checkPaths()
        val sources = sourceRoots
        val targets = (targetRoots ?: sources)!!
        return sources
            ?.zip(targets)
            ?.filter { (s, _) -> s.exists() }
            ?.map { (s, t) -> SourceFileSet.create(s, t) }
            ?: targets.oneSetWithNoFiles()
    }

    private fun checkPaths() {
        if (sourceRoots == null) {
            checkUsage(targetRoots!!.size == 1) {
                "When not providing a source directory, only one target directory must be present."
            }
        }
        if (sourceRoots != null && targetRoots != null) {
            checkUsage(sourceRoots!!.size == targetRoots!!.size) {
                "Mismatched number of directories." +
                        " Given ${sourceRoots!!.size} source directories and" +
                        " ${targetRoots!!.size} target directories."
            }
        }
    }

    private fun loadPlugins(): List<Plugin> {
        val factory = PluginFactory(
            Thread.currentThread().contextClassLoader,
            classpath,
            ::printError
        )
        return factory.load(plugins)
    }

    /**
     * Prints the given error [message] to the screen.
     */
    private fun printError(message: String?) = echo(message, trailingNewline = true, err = true)
}

/**
 * Creates a list that contains a single, empty source set.
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
