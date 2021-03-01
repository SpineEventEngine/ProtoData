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

package io.spine.protodata

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.protodata.given.TestQueryingSubscriber
import io.spine.protodata.given.TestRenderer
import io.spine.protodata.given.TestSkippingSubscriber
import io.spine.protodata.given.TestSubscriber
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceSet
import io.spine.protodata.subscriber.CodeEnhancement
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.test.Journey
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

private const val JOURNEY_TYPE = "type.spine.io/spine.protodata.test.Journey"

class `'Pipeline' should` {

    private lateinit var srcRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var sourceFile: Path
    private lateinit var request: CodeGeneratorRequest
    private lateinit var renderer: TestRenderer

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        sourceFile = srcRoot.resolve("SourceCode.java")
        sourceFile.writeText("""
            ${Journey::class.simpleName} worth taking
        """.trimIndent())

        val protoFile = DoctorProto.getDescriptor()
        request = CodeGeneratorRequest
            .newBuilder()
            .addProtoFile(protoFile.toProto())
            .addFileToGenerate(protoFile.name)
            .build()
        codegenRequestFile.writeBytes(request.toByteArray())
        renderer = TestRenderer()
    }

    @Test
    fun `render enhanced code`() {
        Pipeline(
            listOf(TestSubscriber()),
            listOf(),
            rendererFactory(),
            SourceSet.fromContentsOf(srcRoot),
            request
        )()
        assertThat(sourceFile.readText())
            .isEqualTo("\$Journey$ worth taking")
        assertThat(renderer.called)
            .isTrue()
    }

    @Test
    fun `skip all code generation on 'SkipEverything'`() {
        val initialContents = sourceFile.readText()
        Pipeline(
            listOf(TestSkippingSubscriber()),
            listOf(),
            rendererFactory(),
            SourceSet.fromContentsOf(srcRoot),
            request
        )()
        assertThat(sourceFile.readText())
            .isEqualTo(initialContents)
        assertThat(renderer.called)
            .isFalse()
    }

    @Test
    fun `deliver all events to projections before notifying 'Subscribers'`() {
        val subscriber = TestQueryingSubscriber()
        Pipeline(
            listOf(subscriber),
            listOf(),
            rendererFactory(),
            SourceSet.fromContentsOf(srcRoot),
            request
        )()
        assertThat(subscriber.files)
            .hasSize(1)
        val file = subscriber.files.first()
        assertThat(file.typeMap)
            .hasSize(2)
        assertThat(file.typeMap)
            .containsKey(JOURNEY_TYPE)
        assertThat(file.typeMap[JOURNEY_TYPE]!!.fieldList)
            .hasSize(3)
    }

    private fun rendererFactory(): (List<CodeEnhancement>) -> Renderer {
        return { enhancements ->
            renderer.enhancements = enhancements
            renderer
        }
    }
}
