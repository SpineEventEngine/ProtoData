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

import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.UsageError
import com.google.protobuf.compiler.codeGeneratorRequest
import com.google.protobuf.stringValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.spine.base.Time
import io.spine.option.OptionsProto
import io.spine.protobuf.pack
import io.spine.protodata.cli.given.DefaultOptionsCounterPlugin
import io.spine.protodata.cli.given.DefaultOptionsCounterRenderer
import io.spine.protodata.cli.test.TestOptionsProto
import io.spine.protodata.cli.test.TestProto
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.test.ECHO_FILE
import io.spine.protodata.test.EchoRenderer
import io.spine.protodata.test.PlainStringRenderer
import io.spine.protodata.test.Project
import io.spine.protodata.test.ProjectProto
import io.spine.protodata.test.ProtoEchoRenderer
import io.spine.protodata.test.TestPlugin
import io.spine.protodata.test.UnderscorePrefixRenderer
import io.spine.protodata.test.echo
import io.spine.string.ti
import io.spine.time.LocalDates
import io.spine.time.Month.SEPTEMBER
import io.spine.time.toInstant
import io.spine.type.toCompactJson
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.reflect.jvm.jvmName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@DisplayName("ProtoData command-line application should")
class MainSpec {

    private lateinit var srcRoot : Path
    private lateinit var targetRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var targetFile: Path

    private val outputEchoFile: Path
        get() = targetRoot.resolve(ECHO_FILE)

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        targetRoot = sandbox.resolve("target")
        targetRoot.toFile().mkdirs()
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        val sourceFile = srcRoot.resolve("SourceCode.java")
        sourceFile.writeText("""
            ${Project::class.simpleName}.getUuid() 
        """.trimIndent())
        targetFile = targetRoot.resolve(sourceFile.name)

        val project = ProjectProto.getDescriptor()
        val testProto = TestProto.getDescriptor()
        val request = codeGeneratorRequest {
            protoFile.addAll(listOf(
                project.toProto(),
                testProto.toProto(),
                TestOptionsProto.getDescriptor().toProto(),
                OptionsProto.getDescriptor().toProto()
            ))
            fileToGenerate.addAll(listOf(
                project.name,
                testProto.name
            ))
        }
        codegenRequestFile.writeBytes(request.toByteArray())
    }

    @Test
    fun `render enhanced code`(@TempDir dir: Path) {
        launchApp(
            "-p", TestPlugin::class.jvmName,
            "-p", UnderscorePrefixRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "--target", targetRoot.toString(),
            "-t", codegenRequestFile.toString(),
            "-d", dir.pathString
        )
        targetFile.readText() shouldBe "_${Project::class.simpleName}.getUuid() "
    }

    @Test
    fun `provide Spine options by default`(@TempDir dir: Path) {
        launchApp(
            "-p", DefaultOptionsCounterPlugin::class.jvmName,
            "-p", DefaultOptionsCounterRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "--target", targetRoot.toString(),
            "-t", codegenRequestFile.toString(),
            "-d", dir.pathString
        )
        val generatedFile = targetRoot.resolve(DefaultOptionsCounterRenderer.FILE_NAME)
        generatedFile.readText() shouldBe "true, true"
    }

    @Nested
    inner class `Receive custom configuration through` {

        @Test
        fun `configuration file`(@TempDir dir: Path) {
            val settings = SettingsDirectory(dir)
            val name = "Internet"
            settings.writeFor<EchoRenderer>(Format.JSON, """
                    { "value": "$name" }
                """.ti()
            )

            launchApp(
                "-p", EchoRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", dir.pathString
            )
            outputEchoFile.readText() shouldBe name
        }

        @Test
        fun `configuration value`(@TempDir dir: Path) {
            val settings = SettingsDirectory(dir)
            val name = "Mr. World"
            settings.writeFor<EchoRenderer>(Format.JSON, """
                    { "value": "$name" }
                """.ti()
            )
            launchApp(
                "-p", EchoRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", dir.pathString,
            )
            outputEchoFile.readText() shouldBe name
        }
    }

    @Nested
    inner class `Receive custom configuration as` {

        @Test
        fun `plain JSON`(@TempDir dir: Path) {
            val settings = SettingsDirectory(dir)
            val name = "Internet"
            settings.writeFor<EchoRenderer>(Format.JSON, """
                    { "value": "$name" }
                """.ti()
            )
            launchApp(
                "-p", EchoRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", dir.pathString
            )
            outputEchoFile.readText() shouldBe name
        }

        @Test
        fun `Protobuf JSON`(@TempDir dir: Path) {
            val time = Time.currentTime()
            val json = echo {
                message = "English, %s!"
                extraMessage = stringValue { value = "Do you speak it?" }
                arg = stringValue { value = "Adam Falkner" }.pack()
                when_ = time
            }.toCompactJson()
            val settings = SettingsDirectory(dir)
            settings.writeFor<ProtoEchoRenderer>(Format.PROTO_JSON, json)

            launchApp(
                "-p", ProtoEchoRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", dir.pathString
            )
            val text = outputEchoFile.readText()

            text shouldStartWith time.toInstant().toString()
            text shouldEndWith "English, Adam Falkner!:Do you speak it?"
        }

        @Test
        fun `binary Protobuf`(@TempDir dir: Path) {
            val time = LocalDates.of(1962, SEPTEMBER, 12)
            val bytes = echo {
                message = "We choose to go to the %s."
                extraMessage = stringValue { value = "and do the other things" }
                arg = stringValue { value = "Moon" }.pack()
                when_ = time.toTimestamp()
            }.toByteArray()

            val settings = SettingsDirectory(dir)
            settings.writeFor<ProtoEchoRenderer>(Format.PROTO_BINARY, bytes)

            launchApp(
                "-p", ProtoEchoRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", dir.pathString
            )

            val text = outputEchoFile.readText()

            text shouldStartWith time.toInstant().toString()
            text shouldEndWith "We choose to go to the Moon.:and do the other things"
        }

        @Suppress("TestFunctionName")
        @Test
        fun YAML(@TempDir dir: Path) {
            val name = "Mr. Anderson"
            val settings = SettingsDirectory(dir)
            settings.writeFor<EchoRenderer>(Format.YAML, """
                    value: $name
                """.trimIndent()
            )

            launchApp(
                "-p", EchoRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", dir.pathString
            )
            outputEchoFile.readText() shouldBe name
        }

        @Test
        fun `plain string`(@TempDir configDir: Path) {
            val plainString = "dont.mail.me:42@example.org"
            SettingsDirectory(configDir).writeFor<PlainStringRenderer>(Format.PLAIN, plainString)

            launchApp(
                "-p", PlainStringRenderer::class.jvmName,
                "--src", srcRoot.toString(),
                "--target", targetRoot.toString(),
                "-t", codegenRequestFile.toString(),
                "-d", configDir.pathString
            )
            outputEchoFile.readText() shouldBe plainString
        }
    }

    @Nested
    inner class `Fail if` {

        @Test
        fun `target dir is missing`(@TempDir dir: Path) {
            assertThrows<UsageError> {
                launchApp(
                    "-p", TestPlugin::class.jvmName,
                    "-p", UnderscorePrefixRenderer::class.jvmName,
                    "-t", codegenRequestFile.toString(),
                    "--src", srcRoot.toString(),
                    "-d", dir.pathString
                )
            }
        }

        @Test
        fun `code generator request file is missing`(@TempDir dir: Path) {
            assertMissingOption {
                launchApp(
                    "-p", TestPlugin::class.jvmName,
                    "-p", UnderscorePrefixRenderer::class.jvmName,
                    "--src", srcRoot.toString(),
                    "-d", dir.pathString
                )
            }
        }

        private fun assertMissingOption(block: () -> Unit) {
            assertThrows<MissingOption>(block)
        }
    }

    private fun launchApp(vararg argv: String) = Run("42.0.0").parse(argv.toList())
}
