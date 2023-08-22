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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.logging.Level
import io.spine.logging.WithLogging
import io.spine.option.OptionsProto
import io.spine.protodata.cli.test.TestOptionsProto
import io.spine.protodata.cli.test.TestProto
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.test.Project
import io.spine.protodata.test.ProjectProto
import io.spine.tools.code.AnyLanguage
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
        launchWithLoggingParams("--debug")

        LoggingLevelAsserter.infoEnabled shouldBe true
        LoggingLevelAsserter.debugEnabled shouldBe true
    }

    @Test
    fun `set 'INFO' logging level`() {
        launchWithLoggingParams("--info")

        LoggingLevelAsserter.infoEnabled shouldBe true
        LoggingLevelAsserter.debugEnabled shouldBe false
    }

    @Test
    fun `default to 'WARN' logging level`() {
        launchWithLoggingParams()

        LoggingLevelAsserter.infoEnabled shouldBe false
        LoggingLevelAsserter.debugEnabled shouldBe false
    }

    private fun launchWithLoggingParams(vararg argv: String) {
        val params = mutableListOf(
            "-r", LoggingLevelAsserter::class.jvmName,
            "--src", srcRoot.toString(),
            "-t", codegenRequestFile.toString(),
            "--cv", "testing-logging-levels",
            "--cf", "plain",
        )
        params.addAll(argv)
        Run("1961.04.12").parse(params)
    }
}

/**
 * A pseudo-renderer which asserts that the logging levels are set correctly.
 */
class LoggingLevelAsserter: Renderer<AnyLanguage>(AnyLanguage), WithLogging {

    override fun render(sources: SourceFileSet) {
        debugEnabled = logger.at(Level.DEBUG).isEnabled()
        infoEnabled = logger.at(Level.INFO).isEnabled()
    }

    companion object {
        internal var debugEnabled: Boolean = false
        internal var infoEnabled: Boolean = false
    }
}
