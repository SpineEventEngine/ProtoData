/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.testing

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.io.Resource
import io.spine.io.ResourceDirectory
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.reflect.CallerFinder.findCallerOf
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import io.spine.tools.code.Language
import io.spine.tools.code.Protobuf
import io.spine.tools.code.TypeScript
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_FILE
import java.nio.file.Path

/**
 * Creates a [Pipeline] for testing the given ProtoData [plugins].
 *
 * This class simulates the first step of the code generation process
 * performed by `protoc` compiler. Since `protoc` is a stable and predictable piece of
 * software, we do not need to go through the "vanilla" code generation process.
 *
 * @property plugins
 *         the list of plugins to be passed to the created pipeline.
 * @property request
 *         the code generator request created by `protoc` for the files to be processed.
 * @param inputRoot
 *         the root directory with the source code generated by `protoc`.
 * @param outputRoot
 *         the root directory to which the updated code will be placed.
 * @param settingsDir
 *         the directory to which store the settings for the given plugin.
 * @param writeSettings
 *         a callback for writing plugin settings before the pipeline is created.
 *
 * @see [io.spine.protodata.settings.LoadsSettings.consumerId]
 */
public class PipelineSetup(
    public val plugins: List<Plugin>,
    inputRoot: Path,
    outputRoot: Path,
    public val request: CodeGeneratorRequest,
    settingsDir: Path,
    private val writeSettings: (SettingsDirectory) -> Unit
) {
    /**
     * Creates an instance with only one plugin to be passed to the created pipeline.
     */
    public constructor(
        plugin: Plugin,
        inputRoot: Path,
        outputRoot: Path,
        request: CodeGeneratorRequest,
        settingsDir: Path,
        writeSettings: (SettingsDirectory) -> Unit
    ): this(listOf(plugin), inputRoot, outputRoot, request, settingsDir, writeSettings)

    /**
     * The directory to store settings for the [plugins].
     */
    public val settings: SettingsDirectory

    /**
     * A sole source file set used by the pipeline.
     */
    public val sourceFileSet: SourceFileSet

    init {
        require(plugins.isNotEmpty()) {
            "The list of plugins cannot be empty."
        }
        settingsDir.toFile().mkdirs()
        settings = SettingsDirectory(settingsDir)
        outputRoot.toFile().mkdirs()
        sourceFileSet = SourceFileSet.create(inputRoot, outputRoot)
    }

    /**
     * Creates the pipeline.
     */
    public fun createPipeline(): Pipeline {
        writeSettings(settings)
        val id = Pipeline.generateId()
        val pipeline = Pipeline(id, plugins, listOf(sourceFileSet), request, settings)
        return pipeline
    }

    public companion object {

        public fun byResources(
            language: Language,
            plugins: List<Plugin>,
            outputRoot: Path,
            settingsDir: Path,
            writeSettings: (SettingsDirectory) -> Unit
        ): PipelineSetup {
            val classLoader = classLoaderOfCaller()
            val inputRoot = inputRootOf(language, classLoader)
            val request = loadRequest(classLoader)
            return PipelineSetup(
                plugins,
                inputRoot,
                outputRoot,
                request,
                settingsDir,
                writeSettings,
            )
        }

        @VisibleForTesting
        internal fun detectCallingClass(): Class<*> {
            val caller = findCallerOf(this::class.java, 0)
            val callingClass = Class.forName(caller!!.className)
            return callingClass
        }

        private fun classLoaderOfCaller(): ClassLoader {
            val callingClass = detectCallingClass()
            return callingClass.classLoader
        }

        private fun loadRequest(classLoader: ClassLoader): CodeGeneratorRequest {
            val file = Resource.file(
                "$PROTOC_PLUGIN_NAME/$CODE_GENERATOR_REQUEST_FILE",
                classLoader
            )
            file.open().use {
                val request = CodeGeneratorRequest.parseFrom(it)
                return request
            }
        }

        private fun inputRootOf(language: Language, classLoader: ClassLoader): Path {
            val languageDir = language.protocOutputDir()
            val dirName = "$PROTOC_PLUGIN_NAME/$languageDir"
            val dir = ResourceDirectory.get(dirName, classLoader)
            val inputRoot = dir.toPath()
            return inputRoot
        }
    }
}

@VisibleForTesting
internal fun Language.protocOutputDir(): String {
    return when(this) {
        Java, Kotlin -> name.lowercase()
        Protobuf -> "proto"
        TypeScript -> "ts"
        else -> name
    }
}
