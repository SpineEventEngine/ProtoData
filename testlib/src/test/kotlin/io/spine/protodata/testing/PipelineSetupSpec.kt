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

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.AnyProto
import com.google.protobuf.Empty
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.given.domain.gas.GasTransportationProto
import io.spine.given.domain.oil.OilRefineryProto
import io.spine.io.Resource
import io.spine.io.ResourceDirectory
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.settings.Format
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`PipelineSetup` should")
internal class PipelineSetupSpec {

    @Test
    fun `ensure directories are created`(
        @TempDir settingsDir: Path,
        @TempDir inputRoot: Path,
        @TempDir outputRoot: Path,
    ) {
        val setup = PipelineSetup(
            StubPlugin(),
            listOf(AnyProto.getDescriptor()),
            settingsDir,
            inputRoot,
            outputRoot
        ) { _, _ -> }

        setup.settings.path.exists() shouldBe true
        
        setup.sourceFileSet.inputRoot shouldBe inputRoot

        setup.sourceFileSet.outputRoot.run {
            this shouldBe outputRoot
            exists() shouldBe true
        }
    }

    @Test
    fun `invoke settings callback before creating a pipeline`(
        @TempDir settingsDir: Path,
        @TempDir inputRoot: Path,
        @TempDir outputRoot: Path,
    ) {
        val setup = PipelineSetup(
            StubPlugin(),
            listOf(AnyProto.getDescriptor()),
            settingsDir,
            inputRoot,
            outputRoot
        ) { _, settings ->
            settings.write("foo_bar", Format.PROTO_JSON, Empty.getDefaultInstance().toByteArray())
        }

        settingsDir.fileCount() shouldBe 0
        setup.createPipeline()
        settingsDir.fileCount() shouldBe 1
    }

    @Test
    fun `load Java source code copied by a Gradle task to test resources`(
        @TempDir settingsDir: Path,
        @TempDir outputRoot: Path,
    ) {
        val dir = ResourceDirectory.get("java", this::class.java.classLoader)
        val inputRoot = dir.toPath()
        val setup = PipelineSetup(
            StubPlugin(),
            listOf(AnyProto.getDescriptor()),
            settingsDir,
            inputRoot,
            outputRoot
        ) { _, _ -> }

        val sourceFileSet = setup.sourceFileSet
        sourceFileSet.find(
            Paths.get("io/spine/given/domain/gas/CompressorStation.java")
        ) shouldNotBe null
    }

    @Test
    @Disabled("Until migration to ProtoTap")
    fun `create 'CodeGeneratorRequest' instance mimicking 'protoc'`(
        @TempDir settingsDir: Path,
        @TempDir outputRoot: Path,
    ) {
        val dir = ResourceDirectory.get("java", this::class.java.classLoader)
        val inputRoot = dir.toPath()
        val setup = PipelineSetup(
            StubPlugin(),
            listOf(
                GasTransportationProto.getDescriptor(),
                OilRefineryProto.getDescriptor()
            ),
            settingsDir,
            inputRoot,
            outputRoot
        ) { _, _ -> }

        val resourceDir = "pipeline-setup"
        val classLoader = this::class.java.classLoader

        //val descriptorSetFile = Resource.file("$resourceDir/FileDescriptorSet.binpb", classLoader)
        //val descriptorSet = FileDescriptorSet.parseFrom(descriptorSetFile.open())

        val requestFile = Resource.file("$resourceDir/CodeGeneratorRequest.binpb", classLoader)
        val requestFromResources = CodeGeneratorRequest.parseFrom(requestFile.open())

        val fromSetup = setup.createRequest()

        assertThat(requestFromResources)
            .comparingExpectedFieldsOnly()
            .isEqualTo(fromSetup)
    }
}

internal class StubPlugin: Plugin

private fun Path.fileCount() = toFile().list()!!.size
