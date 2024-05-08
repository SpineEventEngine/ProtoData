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

import com.google.protobuf.Empty
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.io.ResourceDirectory
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.settings.Format.PROTO_JSON
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.tools.code.Java
import io.spine.tools.prototap.Names.PROTOC_PLUGIN_NAME
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`PipelineSetup` should")
internal class PipelineSetupSpec {

    private val emptyRequest = CodeGeneratorRequest.getDefaultInstance()

    @Test
    fun `ensure directories are created`(
        @TempDir input: Path,
        @TempDir output: Path,
        @TempDir settings: Path,
    ) {
        val setup = PipelineSetup(
            listOf(StubPlugin()),
            input,
            output,
            emptyRequest,
            settings,
        ) { _ -> }

        setup.run {
            this.settings.path.exists() shouldBe true
            sourceFileSet.run {
                input shouldBe input
                output.run {
                    this shouldBe output
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
        val setup = PipelineSetup(
            listOf(StubPlugin()),
            input,
            output,
            emptyRequest,
            settings,
        ) { it.write("foo_bar", PROTO_JSON, Empty.getDefaultInstance().toByteArray()) }

        settings.fileCount() shouldBe 0
        setup.createPipeline()
        settings.fileCount() shouldBe 1
    }

    @Test
    fun `obtain the calling class`() {
        val callingClass = PipelineSetup.detectCallingClass()
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
            val resourceDir = ResourceDirectory.get(
                "${PROTOC_PLUGIN_NAME}/${language.protocOutputDir()}",
                this::class.java.classLoader
            )
            sourceFileSet.run {
                isEmpty shouldBe false
                inputRoot shouldBe resourceDir.toPath()
                find(
                    Path("io/spine/given/domain/gas/CompressorStation.java")
                ) shouldNotBe null
            }
        }
    }
}

private fun setupByResources(
    language: Java,
    outputRoot: Path,
    settingsDir: Path
): PipelineSetup = byResources(
    language,
    listOf(StubPlugin()),
    outputRoot,
    settingsDir,
) { _ -> }

internal class StubPlugin: Plugin

private fun Path.fileCount() = toFile().list()!!.size
