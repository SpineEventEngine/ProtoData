/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.github.ajalt.clikt.core.UsageError
import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.string.shouldContain
import io.spine.option.OptionsProto
import io.spine.protodata.cli.test.TestOptionsProto
import io.spine.protodata.cli.test.TestProto
import io.spine.protodata.test.PlainStringRenderer
import io.spine.protodata.test.Project
import io.spine.protodata.test.ProjectProto
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.reflect.jvm.jvmName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class `ProtoData CLI logging levels should` {

    private lateinit var codegenRequestFile: Path
    private lateinit var srcRoot : Path
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
    fun `fail if both 'debug' and 'info' flags are set`() {
        val error = assertThrows<UsageError> {
            launchWithLoggingParams(
                "--debug", "--info"
            )
        }
        // Check that we got the required usage error.
        error.message shouldContain
                "Debug and info logging levels cannot be enabled at the same time."
    }

    @Test
    fun `set 'DEBUG' logging level`() {
        val consoleOutput = tapConsole {
            launchWithLoggingParams(
                "--debug"
            )
        }
        // The below tests both the logging domain and the message issued `atDebug` level.
        // It also tests that the message is indented correctly. If not, the message is
        // likely to have more than one space after the logging domain prefix.
        consoleOutput shouldContain
                "[ProtoData] Starting code generation with the following arguments:"
    }

    private fun launchWithLoggingParams(vararg argv: String) {
        val params = mutableListOf(
            "-r", PlainStringRenderer::class.jvmName,
            "--src", srcRoot.toString(),
            "-t", codegenRequestFile.toString(),
            "--cv", "testing-logging-levels",
            "--cf", "plain",
        )
        params.addAll(argv)
        Run("1961.04.12").parse(params)
    }
}

private fun tapConsole(block: () -> Unit): String {
    val bytes = ByteArrayOutputStream()
    val stream = PrintStream(bytes)
    val saveOut = System.out
    val saveErr = System.err
    System.setOut(stream);
    System.setErr(stream)

    try {
        block()
        bytes.flush()
        return bytes.toString()
    } finally {
        System.setOut(saveOut)
        System.setErr(saveErr)
    }
}
