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

import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.UsageError
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.compiler.PluginProtos
import io.spine.option.OptionsProto
import io.spine.protodata.cli.given.CustomOptionPlugin
import io.spine.protodata.cli.given.CustomOptionRenderer
import io.spine.protodata.cli.given.TestOptionProvider
import io.spine.protodata.cli.test.TestOptionsProto
import io.spine.protodata.cli.test.TestProto
import io.spine.protodata.test.Project
import io.spine.protodata.test.ProjectProto
import io.spine.protodata.test.TestPlugin
import io.spine.protodata.test.TestRenderer
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.reflect.jvm.jvmName
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class `Command line application should` {

    private lateinit var srcRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var sourceFile: Path

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        sourceFile = srcRoot.resolve("SourceCode.java")
        sourceFile.writeText("""
            ${Project::class.simpleName}.getUuid() 
        """.trimIndent())

        val project = ProjectProto.getDescriptor()
        val testProto = TestProto.getDescriptor()
        val request = PluginProtos.CodeGeneratorRequest
            .newBuilder()
            .addProtoFile(project.toProto())
            .addProtoFile(testProto.toProto())
            .addProtoFile(TestOptionsProto.getDescriptor().toProto())
            .addProtoFile(OptionsProto.getDescriptor().toProto())
            .addFileToGenerate(project.name)
            .addFileToGenerate(testProto.name)
            .build()
        codegenRequestFile.writeBytes(request.toByteArray())
    }

    @Test
    fun `render enhanced code`() {
        launchApp(
            "-p", TestPlugin::class.jvmName,
            "-r", TestRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "-t", codegenRequestFile.toString()
        )
        assertThat(sourceFile.readText())
            .isEqualTo("_${Project::class.simpleName}.getUuid() ")
    }

    @Test
    fun `supply options by file path`() {
        launchApp(
            "-p", CustomOptionPlugin::class.jvmName,
            "-r", CustomOptionRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "-t", codegenRequestFile.toString(),
            "-o", "spine/protodata/cli/test/options.proto",
            "-o", "spine/options.proto"
        )
        val generatedFile = srcRoot.resolve(CustomOptionRenderer.FILE_NAME)
        assertThat(generatedFile.readText())
            .isEqualTo("custom_field_for_test")
    }

    @Test
    fun `supply options by a provider`() {
        launchApp(
            "-p", CustomOptionPlugin::class.jvmName,
            "-r", CustomOptionRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "-t", codegenRequestFile.toString(),
            "--op", TestOptionProvider::class.jvmName
        )
        val generatedFile = srcRoot.resolve(CustomOptionRenderer.FILE_NAME)
        assertThat(generatedFile.readText())
            .isEqualTo("custom_field_for_test")
    }

    @Nested
    inner class `Fail if` {

        @Test
        fun `renderer is missing`() {
            assertMissingOption {
                launchApp(
                    "-p", TestPlugin::class.jvmName,
                    "--src", srcRoot.toString(),
                    "-t", codegenRequestFile.toString()
                )
            }
        }

        @Test
        fun `source and target dirs are missing`() {
            assertThrows<UsageError> {
                launchApp(
                    "-p", TestPlugin::class.jvmName,
                    "-r", TestRenderer::class.jvmName,
                    "-t", codegenRequestFile.toString()
                )
            }
        }

        @Test
        fun `code generator request file is missing`() {
            assertMissingOption {
                launchApp(
                    "-p", TestPlugin::class.jvmName,
                    "-r", TestRenderer::class.jvmName,
                    "--src", srcRoot.toString()
                )
            }
        }

        private fun assertMissingOption(block: () -> Unit) {
            assertThrows<MissingOption>(block)
        }
    }

    private fun launchApp(vararg argv: String) = Run("42.0.0").parse(argv.toList())
}
