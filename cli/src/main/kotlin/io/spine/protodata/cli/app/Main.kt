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
import io.spine.logging.Level
import io.spine.logging.WithLogging
import io.spine.logging.context.LogLevelMap
import io.spine.logging.context.ScopedLoggingContext
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.params.DebugLoggingParam
import io.spine.protodata.params.InfoLoggingParam
import io.spine.protodata.params.Parameter
import io.spine.protodata.params.ParametersFileParam
import io.spine.protodata.params.PipelineParameters
import io.spine.protodata.params.PluginParam
import io.spine.protodata.params.UserClasspathParam
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.util.parseFile
import io.spine.string.Separator
import io.spine.tools.code.manifest.Version
import java.io.File
import java.io.File.pathSeparator
import java.nio.file.Path
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

    private val paramsFile: File by ParametersFileParam.toOption().file(
        mustExist = true,
        canBeDir = false,
        canBeSymlink = false,
        mustBeReadable = true
    ).required()

    private val plugins: List<String>
            by PluginParam.toOption().multiple()

    private val classpath: List<Path>?
            by UserClasspathParam.toOption().path(
                mustExist = true,
                mustBeReadable = true
            ).splitPaths()

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
        val plugins = loadPlugins()

        val params = parseFile(paramsFile, PipelineParameters::class.java)

        val pipeline = Pipeline(
            params = params,
            plugins = plugins,
        )
        pipeline()
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
 * Throws an [UsageError] with the result of calling [lazyMessage] if the [condition] isn't met.
 */
private inline fun checkUsage(condition: Boolean, lazyMessage: () -> Any) {
    if (condition.not()) {
        val message = lazyMessage()
        throw UsageError(message.toString())
    }
}
