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

package io.spine.protodata.context

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.spine.protodata.ast.EnumInFile
import io.spine.protodata.ast.MessageInFile
import io.spine.protodata.ast.ServiceInFile
import io.spine.protodata.protobuf.ProtoFileList
import io.spine.protodata.testing.RenderingTestbed
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.testing.PipelineSetup
import io.spine.tools.code.Java
import java.nio.file.Path
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`Member` should")
internal class MemberSpec {

    companion object {

        val probe = ProbeRenderer()

        /**
         * Creates and runs a pipeline with [ProbeRenderer].
         *
         * Java language is used in the pipeline for simplicity of the setup
         * assisted by ProtoTap Gradle plugin.
         *
         * No Java codegen features are checked by this test suite.
         */
        @BeforeAll
        @JvmStatic
        fun setup(@TempDir outputDir: Path, @TempDir settingsDir: Path) {
            val setup = PipelineSetup.byResources(
                language = Java,
                protoFileList = ProtoFileList(listOf()),
                plugins = listOf(RenderingTestbed(probe)),
                outputRoot = outputDir,
                settingsDir = settingsDir
            ) {}

            val pipeline = setup.createPipeline()
            pipeline()
        }
    }

    @Nested inner class
    `provide extensions for` {

        @Test
        fun `obtaining message types`() {
            val messageTypes = probe.messageTypes

            messageTypes.find("Person").let {
                it shouldNotBe null
                it!!.filePath shouldEndWith "common.proto"
            }

            messageTypes.find("Vote").let {
                it shouldNotBe null
                it!!.filePath shouldEndWith "election.proto"
            }
        }

        @Test
        fun `obtaining enum types`() {
            val enumTypes = probe.enumTypes

            enumTypes.find("Status").let {
                it shouldNotBe null
                it!!.filePath shouldEndWith "common.proto"
            }

            enumTypes.find("Choice").let {
                it shouldNotBe null
                it!!.filePath shouldEndWith "election.proto"
            }
        }

        @Test
        fun `obtaining services`() {
            val services = probe.services

            services.find("Moderation").let {
                it shouldNotBe null
                it!!.filePath shouldEndWith "common.proto"
            }

            services.find("Voting").let {
                it shouldNotBe null
                it!!.filePath shouldEndWith "election.proto"
            }
        }
    }
}

/**
 * A diagnostic probe which performs queries as a [Member] of code generation process,
 * but does not render anything.
 */
class ProbeRenderer : Renderer<Java>(Java) {

    lateinit var messageTypes: Set<MessageInFile>
    lateinit var enumTypes: Set<EnumInFile>
    lateinit var services: Set<ServiceInFile>

    override fun render(sources: SourceFileSet) {
        messageTypes = findMessageTypes()
        enumTypes = findEnumTypes()
        services = findServices()
    }
}

private fun Iterable<MessageInFile>.find(simpleName: String): MessageInFile? =
    firstOrNull { it.message.name.simpleName == simpleName }

private val MessageInFile.filePath: String
    get() = fileHeader.file.path

private fun Iterable<EnumInFile>.find(simpleName: String): EnumInFile? =
    firstOrNull { it.enum.name.simpleName == simpleName }

private val EnumInFile.filePath: String
    get() = fileHeader.file.path

private fun Iterable<ServiceInFile>.find(simpleName: String): ServiceInFile? =
    firstOrNull { it.service.name.simpleName == simpleName }

private val ServiceInFile.filePath: String
    get() = fileHeader.file.path
