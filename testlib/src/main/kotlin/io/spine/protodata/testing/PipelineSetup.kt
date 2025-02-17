/*
 * Copyright 2025, TeamDev. All rights reserved.
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
import io.spine.io.replaceExtension
import io.spine.protodata.ast.file
import io.spine.protodata.ast.toPath
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.backend.DescriptorFilter
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.params.PipelineParameters
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.protodata.util.Format.PROTO_JSON
import io.spine.protodata.util.extensions
import io.spine.reflect.CallerFinder.findCallerOf
import io.spine.testing.server.blackbox.BlackBox
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import io.spine.tools.code.Language
import io.spine.tools.code.Protobuf
import io.spine.tools.code.TypeScript
import io.spine.tools.prototap.CompiledProtosFile
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import io.spine.tools.prototap.Paths.CODE_GENERATOR_REQUEST_FILE
import io.spine.tools.prototap.Paths.COMPILED_PROTOS_FILE
import io.spine.type.toJson
import io.spine.validate.NonValidated
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
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
 * val setup = PipelineSetup.byResources(plugins, outputDir, settingsDir, descriptorFilter) {
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
 * @property params The pipeline parameters instance, which may contain only
 *   partial information required for the test.
 * @property plugins The list of plugins to be passed to the created pipeline.
 * @param inputDir The root directory with the source code generated by `protoc`.
 * @param outputDir The root directory where the updated code will be placed.
 * @property descriptorFilter The predicate to accept descriptors during parsing of
 *  [CodeGeneratorRequest] loaded by the pipeline.
 *  The default value accepts all the descriptors.
 *  The primary usage scenario for this parameter is accepting only
 *  descriptors of interest when running tests.
 * @param writeSettings A callback for writing plugin settings before the pipeline is created.
 * @constructor Creates in instance for creating [Pipeline] for testing the given [plugins].
 *
 * @see [byResources]
 * @see [SettingsDirectory]
 * @see [io.spine.protodata.settings.LoadsSettings.consumerId]
 * @see DescriptorFilter
 */
@Suppress("LongParameterList") // OK, assuming the default value.
public class PipelineSetup(
    params: @NonValidated PipelineParameters,
    public val plugins: List<Plugin>,
    inputDir: Path,
    outputDir: Path,
    private val descriptorFilter: DescriptorFilter = { true },
    private val writeSettings: (SettingsDirectory) -> Unit
) {
    /**
     * Parameters with populated source and target rood directories passed as `inputDir` and
     * `outputDir` constructor parameters.
     */
    public val params: PipelineParameters = params.toBuilder()
        .withRoots(inputDir, outputDir)
        .buildPartial()

    /**
     * The directory to store settings for the [plugins].
     */
    public val settings: SettingsDirectory

    /**
     * The pipeline returned by the [createPipeline] method.
     *
     * The only purpose of this property is to serve the deprecated
     * [sourceFileSet] and [sourceFiles] properties.
     * Once these properties are removed, this property should be removed too.
     */
    private lateinit var pipeline: Pipeline

    /**
     * The source file set used by the pipeline.
     */
    @Deprecated(
        message = "Please use `pipeline.sources[0]` instead.",
        replaceWith = ReplaceWith("pipeline.sources[0]")
    )
    public val sourceFiles: SourceFileSet
        get() = pipeline.sources[0]

    /**
     * The source file set used by the pipeline.
     */
    @Deprecated(
        message = "Please use `pipeline.sources[0]` instead.",
        replaceWith = ReplaceWith("pipeline.sources[0]")
    )
    public val sourceFileSet: SourceFileSet
        get() = pipeline.sources[0]

    init {
        require(plugins.isNotEmpty()) {
            "The list of plugins cannot be empty."
        }
        settings = SettingsDirectory(params.settings.toPath())
        outputDir.toFile().mkdirs()
    }

    /**
     * Creates the pipeline.
     */
    public fun createPipeline(): Pipeline {
        writeSettings(settings)
        val id = Pipeline.generateId()
        val p = Pipeline(
            id,
            params,
            plugins,
            descriptorFilter,
        )
        this.pipeline = p
        return p
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
         * @param language The programming language which is handled by the pipeline to be created.
         * @param params The pipeline parameters instance, which may contain only
         *   partial information required for the test.
         * @param plugins The list of plugins to be passed to the created pipeline.
         * @param outputRoot The root directory to which the updated code will be placed into
         *  the subdirectory calculated from the [language].
         * @param descriptorFilter The predicate to accept descriptors.
         *  The default value accepts all the descriptors.
         *  The primary usage scenario for this parameter is accepting only
         *  descriptors of interest when running tests.
         * @param writeSettings A callback for writing plugin settings before
         *  the pipeline is created.
         */
        @Suppress("LongParameterList") // OK, assuming the default value.
        public fun byResources(
            language: Language,
            params: @NonValidated PipelineParameters,
            plugins: List<Plugin>,
            outputRoot: Path,
            descriptorFilter: DescriptorFilter = { true },
            writeSettings: (SettingsDirectory) -> Unit
        ): PipelineSetup {
            val callingClass = detectCallingClass()
            val classLoader = callingClass.classLoader
            val inputDir = inputRootOf(language, classLoader)
            val outputDir = outputRoot.resolve(language.protocOutputDir())

            writeRequestFiles(params, classLoader)

            val updatedParams = listCompiledProtoFiles(params, classLoader)

            return PipelineSetup(
                updatedParams,
                plugins,
                inputDir,
                outputDir,
                descriptorFilter,
                writeSettings,
            )
        }

        private fun writeRequestFiles(
            params: PipelineParameters,
            classLoader: ClassLoader
        ) {
            val request = loadRequest(classLoader)
            val requestFile = params.request.toPath()
            requestFile.parent.toFile().mkdirs()
            requestFile.writeBytes(request.toByteArray(), CREATE, TRUNCATE_EXISTING)
            val jsonFile = requestFile.replaceExtension(PROTO_JSON.extensions[0])
            val json = request.toJson()
            jsonFile.writeText(json, options = arrayOf(CREATE, TRUNCATE_EXISTING))
        }

        /**
         * Adds the list of compiled proto files to the given instance of [PipelineParameters].
         *
         * The list of files is loaded from [COMPILED_PROTOS_FILE] prepared by ProtoTap IFF
         * given parameters do not have listed files.
         * In such a case, the given instance is returned as the result of the function.
         */
        private fun listCompiledProtoFiles(
            params: @NonValidated PipelineParameters,
            classLoader: ClassLoader
        ): @NonValidated PipelineParameters{
            return if (params.compiledProtoList.isEmpty()) {
                val files = CompiledProtosFile(classLoader)
                    .listFiles { file { path = it } }
                params.toBuilder()
                    .addAllCompiledProto(files)
                    .buildPartial()
            } else {
                params
            }
        }

        /**
         * Creates an instance assuming that the input directory and [CodeGeneratorRequest] are
         * placed into the resources using [ProtoTap](https://github.com/SpineEventEngine/ProtoTap).
         *
         * The pipeline to be created will handle code generation in [Java].
         *
         * @param params The pipeline parameters instance, which may contain only
         *   partial information required for the test.
         * @param plugins The list of plugins to be passed to the created pipeline.
         * @param outputRoot The root directory to which the updated code will be placed under
         *   the `java` subdirectory.
         * @param descriptorFilter The predicate to accept descriptors.
         *  The default value accepts all the descriptors.
         *  The primary usage scenario for this parameter is accepting only
         *  descriptors of interest when running tests.
         * @param writeSettings a callback for writing plugin settings before the pipeline
         *   is created.
         */
        public fun byResources(
            params: @NonValidated PipelineParameters = PipelineParameters.getDefaultInstance(),
            plugins: List<Plugin>,
            outputRoot: Path,
            descriptorFilter: DescriptorFilter = { true },
            writeSettings: (SettingsDirectory) -> Unit
        ): PipelineSetup =
            byResources(
                Java,
                params,
                plugins,
                outputRoot,
                descriptorFilter,
                writeSettings
            )

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

        /**
         * Loads [CodeGeneratorRequest] from the file created by ProtoTap.
         *
         * We load and parse the request, instead of just copying the file,
         * to ensure its correctness of the request file.
         */
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
