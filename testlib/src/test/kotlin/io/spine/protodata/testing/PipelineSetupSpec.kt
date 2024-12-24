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

import com.google.protobuf.Empty
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.io.ResourceDirectory
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.protobuf.ProtoFileList
import io.spine.protodata.util.Format.PROTO_JSON
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.protodata.testing.PipelineSetup.Companion.detectCallingClass
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import io.spine.tools.code.TypeScript
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`PipelineSetup` should")
internal class PipelineSetupSpec {

    @Test
    fun `ensure output and settings directories are created`(
        @TempDir input: Path,
        @TempDir output: Path,
        @TempDir settings: Path,
    ) {
        // Since JUnit creates `output` and `settings` directories automatically,
        // have nested ones to check the creation.
        val outputPrim = output.resolve("primo")
        val settingsPrim = settings.resolve("primus")
        val setup = setup(input, outputPrim, settingsPrim) { _ -> }
        setup.run {
            this.settings.path.exists() shouldBe true
            sourceFileSet.run {
                inputRoot shouldBe input
                outputRoot.run {
                    this shouldBe outputPrim
                    exists() shouldBe true
                }
            }
        }
    }

    @Test
    fun `invoke settings callback before creating a pipeline`(
        @TempDir input: Path,
        @TempDir output: Path,
        @TempDir settings: Path,
    ) {
        val setup = setup(input, output, settings) {
            it.write("foo_bar", PROTO_JSON, Empty.getDefaultInstance().toByteArray())
        }
        settings.fileCount() shouldBe 0
        setup.createPipeline()
        settings.fileCount() shouldBe 1
    }

    @Test
    fun `obtain the calling class`() {
        val callingClass = detectCallingClass()
        callingClass shouldBe this::class.java
    }

    @Test
    fun `use 'CodeGeneratorRequest' captured by ProtoTap`(
        @TempDir output: Path,
        @TempDir settings: Path,
    ) {
        val setup = setupByResources(Java, output, settings)
        setup.request shouldNotBe CodeGeneratorRequest.getDefaultInstance()
    }

    @Test
    fun `use sources captured by ProtoTap`(
        @TempDir output: Path,
        @TempDir settings: Path,
    ) {
        val language = Java
        val setup = setupByResources(language, output, settings)
        setup.run {
            val langDir = language.protocOutputDir()
            val resourceDir = ResourceDirectory.get(
                "${PROTOC_PLUGIN_NAME}/$langDir",
                this::class.java.classLoader
            )
            sourceFileSet.run {
                // We have generated sources as input in the set.
                isEmpty shouldBe false
                val inputPath = resourceDir.toPath()
                inputRoot shouldBe inputPath

                // The output root of the source file set is configured with the subdirectory
                // which corresponds to the input.
                outputRoot shouldBe output.resolve(langDir)
                find(
                    Path("io/spine/given/domain/gas/CompressorStation.java")
                ) shouldNotBe null
            }
        }
    }

    @Test
    fun `calculate the name of 'protoc' output directory for a 'Language'`() {
        Java.protocOutputDir() shouldBe "java"
        Kotlin.protocOutputDir() shouldBe "kotlin"
        TypeScript.protocOutputDir() shouldBe "ts"
    }

    @Test
    fun `create a pipeline with 'BlackBox' instance over 'CodeGeneratorContext'`(
        @TempDir output: Path,
        @TempDir settings: Path,
    ) {
        val setup = setupByResources(Java, output, settings)
        val (pipeline, blackbox) = setup.createPipelineWithBlackBox()

        // We do not expose the type behind the `codegenContext` property for additional
        // safety of the usage. We still assume and test it here because it's essential for
        // the testing utilities we provide.
        (pipeline.codegenContext is CodeGenerationContext) shouldBe true
        val underlyingContext = (pipeline.codegenContext as CodeGenerationContext).context

        blackbox.use {
            it.isOpen shouldBe true
            pipeline.invoke()
            underlyingContext.isOpen shouldBe false
        }
    }
}

private fun setup(
    input: Path,
    output: Path,
    settings: Path,
    writeSettings: (SettingsDirectory) -> Unit
): PipelineSetup = PipelineSetup(
    ProtoFileList(listOf()),
    listOf(StubPlugin()),
    input,
    output,
    settings,
    CodeGeneratorRequest.getDefaultInstance(),
    writeSettings = writeSettings
)

private fun setupByResources(
    language: Java,
    outputRoot: Path,
    settingsDir: Path
): PipelineSetup = byResources(
    language,
    ProtoFileList(listOf()),
    listOf(StubPlugin()),
    outputRoot,
    settingsDir,
) { _ -> }

internal class StubPlugin: Plugin()

private fun Path.fileCount() = toFile().list()!!.size
