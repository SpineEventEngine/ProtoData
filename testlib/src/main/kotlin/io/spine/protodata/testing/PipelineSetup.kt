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

package io.spine.protodata.testing

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.code.proto.parse
import io.spine.io.Resource
import io.spine.io.ResourceDirectory
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.reflect.CallerFinder.findCallerOf
import io.spine.testing.server.blackbox.BlackBox
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import io.spine.tools.code.Language
import io.spine.tools.code.Protobuf
import io.spine.tools.code.TypeScript
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_FILE
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * Creates a [Pipeline] for testing given ProtoData [plugins].
 *
 * This class simulates the first step of the code generation process
 * performed by `protoc` compiler. Since `protoc` is a stable and predictable piece of
 * software, we do not need to go through the "vanilla" code generation process when we
 * test ProtoData plugins.
 *
 * Instead of running the whole code generation process in a Gradle build with ProtoData Gradle
 * Plugin applied over and over again, we can generate the code and related binary data like
 * [CodeGeneratorRequest] or [FileDescriptorSet][com.google.protobuf.DescriptorProtos.FileDescriptorSet]
 * using `protoc` and then use its output in tests.
 *
 * A convenient way of capturing the generated "vanilla" code and associated files is
 * using [ProtoTap Gradle Plugin](https://github.com/SpineEventEngine/ProtoTap).
 * This is the recommended way for working with `PipelineSetup` which provides most of the input
 * for creation of a [Pipeline] automatically. Configuration steps associated with this approach are
 * described below.
 *
 * The class also allows fine-tuned way of working using its constructor directly for the usage
 * scenarios when generated code and [CodeGeneratorRequest] are already available or generated via
 * a custom procedure.
 *
 * ## Creating `PipelineSetup` with the help of ProtoTap
 * 
 * Here are the steps for creating an instance of `PipelineSetup` in your tests.
 *
 * ### 1. Add [`java-test-fixtures`](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures) plugin
 * The plugin will create the `testFixtures` source set in which we will put proto files.
 *
 * Alternatively, you can put proto files under the `test` source set, but with `testFixtures`
 * it is a bit neater.
 *
 * ### 2. Add [Protobuf Gradle Plugin](https://github.com/google/protobuf-gradle-plugin)
 * ... if it's not yet applied directly or indirectly.
 *
 * Please remember to specify
 * the [`protoc` artifact](https://github.com/google/protobuf-gradle-plugin?tab=readme-ov-file#locate-external-executables).
 *
 * ### 3. Add [ProtoTap Gradle Plugin](https://github.com/SpineEventEngine/ProtoTap)
 * If you're going to put proto files under `testFixtures` or `test` source sets, ProtoTap
 * would pick them automatically.
 *
 * If you're going to use a custom source set, please pass it to
 * the [plugin settings](https://github.com/SpineEventEngine/ProtoTap?tab=readme-ov-file#using-plugin-settings).
 *
 * ### 4. Add a test method with two `@TempDir` parameters
 * This step applies if you're using JUnit. We will need two directories: one is for storing
 * settings for the ProtoData plugins we're going to test, and another is for the output of
 * the code generation process:
 *
 * ```kotlin
 * @Test
 * fun `my test`(
 *     @TempDir outputDir: Path,
 *     @TempDir settingsDir: Path
 * ) {
 *     // Test code will be here.
 * }
 * ```
 *
 * ### 5. Create `PipelineSetup` instance using [byResources] factory method
 *
 * ```kotlin
 * val setup = PipelineSetup.byResources(outputDir, settingsDir) {
 *     // Write settings here.
 * }
 * ```
 * The above call to [byResources] assumes we work with code generation in [Java].
 * For other programming languages please use the overload which accepts [Language] as
 * the first parameter (e.g. [Kotlin] or [TypeScript]).
 *
 * The callback block for writing settings accepts an instance of [SettingsDirectory] that
 * will be available from the [Pipeline] to be created.
 *
 * ## Conventions for directory names with the generated code
 * Protobuf compiler creates a separate directory for each programming language after a name
 * of the corresponding `protoc` plugin or built-in. Correspondingly, directories in test resources
 * with the generated code copied by ProtoTap would have those names. Here are the conventions
 * used by `PipelineSetup` for accessing language subdirectories:
 *  * [Java] -> `"java"`
 *  * [Kotlin] -> `"kotlin"`
 *  * [TypeScript] -> `"ts"`
 *  * [Protobuf] -> `"proto"`
 *  * Other languages -> a lowercase version of [Language.name].
 *
 * @property plugins the list of plugins to be passed to the created pipeline.
 * @property request the code generator request created by `protoc` for the files to be processed.
 * @param inputDir the root directory with the source code generated by `protoc`.
 * @param outputDir the root directory to which the updated code will be placed.
 * @param settingsDir the directory to which store the settings for the given plugin.
 * @param writeSettings a callback for writing plugin settings before the pipeline is created.
 * @constructor Creates in instance for creating [Pipeline] for testing the given [plugins].
 *
 * @see [byResources]
 * @see [SettingsDirectory]
 * @see [io.spine.protodata.settings.LoadsSettings.consumerId]
 */
public class PipelineSetup(
    public val plugins: List<Plugin>,
    inputDir: Path,
    outputDir: Path,
    public val request: CodeGeneratorRequest,
    settingsDir: Path,
    private val writeSettings: (SettingsDirectory) -> Unit
) {
    /**
     * The directory to store settings for the [plugins].
     */
    public val settings: SettingsDirectory

    /**
     * The source file set used by the pipeline.
     */
    public val sourceFileSet: SourceFileSet

    init {
        require(plugins.isNotEmpty()) {
            "The list of plugins cannot be empty."
        }
        settingsDir.toFile().mkdirs()
        settings = SettingsDirectory(settingsDir)
        outputDir.toFile().mkdirs()
        sourceFileSet = SourceFileSet.create(inputDir, outputDir)
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

    @Deprecated(
        message = "Please use `createPipelineAndBlackBox()` instead.",
        replaceWith = ReplaceWith("createPipelineAndBlackBox()")
    )
    public fun createPipelineAndBlackbox(): Pair<Pipeline, BlackBox> =
        createPipelineWithBlackBox()

    /**
     * Creates a [Pipeline] and a [BlackBox] to for testing the [CodeGenerationContext] of
     * the created pipeline.
     *
     * The created instances of [Pipeline] and [BlackBox] are "entangled" in the sense
     * that the [BlackBox] instance should be queried only after the [Pipeline] is
     * [executed][Pipeline.invoke].
     *
     * @see BlackBox
     */
    public fun createPipelineWithBlackBox(): Pair<Pipeline, BlackBox> {
        val pipeline = createPipeline()
        val codegenContext = (pipeline.codegenContext as CodeGenerationContext).context
        val blackbox = BlackBox.from(codegenContext)
        return Pair(pipeline, blackbox)
    }

    public companion object {

        /**
         * Creates a Gradle project to be used in the tests.
         *
         * @param dir the project directory.
         */
        public fun createProject(dir: Path): Project =
            ProjectBuilder.builder().withProjectDir(dir.toFile()).build()

        /**
         * Creates an instance assuming that the input directory and [CodeGeneratorRequest] are
         * placed into the resources using [ProtoTap](https://github.com/SpineEventEngine/ProtoTap).
         *
         * @param language the programming language which is handled by the pipeline to be created.
         * @param plugins the list of plugins to be passed to the created pipeline.
         * @param outputRoot the root directory to which the updated code will be placed into
         *        the subdirectory calculated from the [language].
         * @param settingsDir the directory to which store the settings for the given plugin.
         * @param writeSettings a callback for writing plugin settings before
         *        the pipeline is created.
         */
        public fun byResources(
            language: Language,
            plugins: List<Plugin>,
            outputRoot: Path,
            settingsDir: Path,
            writeSettings: (SettingsDirectory) -> Unit
        ): PipelineSetup {
            val callingClass = detectCallingClass()
            val classLoader = callingClass.classLoader
            val inputDir = inputRootOf(language, classLoader)
            val outputDir = outputRoot.resolve(language.protocOutputDir())
            val request = loadRequest(classLoader)
            return PipelineSetup(
                plugins,
                inputDir,
                outputDir,
                request,
                settingsDir,
                writeSettings,
            )
        }

        /**
         * Creates an instance assuming that the input directory and [CodeGeneratorRequest] are
         * placed into the resources using [ProtoTap](https://github.com/SpineEventEngine/ProtoTap).
         *
         * The pipeline to be created will handle code generation in [Java].
         *
         * @param plugins the list of plugins to be passed to the created pipeline.
         * @param outputRoot the root directory to which the updated code will be placed under
         *        the `java` subdirectory.
         * @param settingsDir the directory to which store the settings for the given plugin.
         * @param writeSettings a callback for writing plugin settings before the pipeline
         *        is created.
         */
        public fun byResources(
            plugins: List<Plugin>,
            outputRoot: Path,
            settingsDir: Path,
            writeSettings: (SettingsDirectory) -> Unit
        ): PipelineSetup = byResources(Java, plugins, outputRoot, settingsDir, writeSettings)

        /**
         * Detects which class calls a method of [Pipeline.Companion].
         *
         * The method is used for obtaining a `ClassLoader` of a test suite class that
         * creates an instance of [PipelineSetup] so that the class loader can be used for
         * accessing the resources.
         *
         * @see byResources
         */
        @VisibleForTesting
        internal fun detectCallingClass(): Class<*> {
            val thisClass = this::class.java
            val caller = findCallerOf(thisClass, 0)
            check(caller != null) { "Unable to obtain the caller for the class `$thisClass`." }
            val callingClass = Class.forName(caller.className)
            return callingClass
        }

        private fun loadRequest(classLoader: ClassLoader): CodeGeneratorRequest {
            val file = Resource.file(
                "$PROTOC_PLUGIN_NAME/$CODE_GENERATOR_REQUEST_FILE",
                classLoader
            )
            file.open().use {
                val request = CodeGeneratorRequest::class.parse(it)
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
        TypeScript -> "ts"
        // It's not likely we have proto files in the output of `protoc` or ProtoData anytime soon.
        // But let's cover this case of the meta-codegen tools that produce Protobuf code.
        Protobuf -> "proto"
        else -> name
    }
}
