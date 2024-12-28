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

package io.spine.protodata.backend

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.spine.protodata.params.RequestDirectory
import io.spine.protodata.testing.PipelineSetup
import io.spine.protodata.testing.parametersWithRequestFile
import io.spine.protodata.testing.recorder.RecordingPlugin
import io.spine.protodata.testing.recorder.enumTypeNames
import io.spine.protodata.testing.recorder.messageTypeNames
import io.spine.protodata.testing.recorder.serviceNames
import io.spine.tools.code.SourceSetName
import java.nio.file.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Filtering of a `Pipeline` should filter")
internal class PipelineFilteringSpec {

    private lateinit var recorder: RecordingPlugin

    @BeforeEach
    fun createRecorder() {
        recorder = RecordingPlugin()
    }

    /**
     * Creates a pipeline with the [recorder] plugin and the given [filter].
     */
    private fun createPipeline(
        requestsDir: Path,
        output: Path,
        filter: DescriptorFilter
    ): Pipeline {
        val requestFile = RequestDirectory(requestsDir).file(SourceSetName("testFixtures"))
        val params = parametersWithRequestFile(requestFile)
        val setup = PipelineSetup.byResources(
            params = params,
            plugins = listOf(recorder),
            outputRoot = output,
            descriptorFilter = filter
        ) { _ -> }
        val pipeline = setup.createPipeline()
        return pipeline
    }

    /**
     * Obtains a fully qualified name of a Protobuf declaration in `pipeline_filtering_spec.proto`.
     */
    private fun qualifiedNameOf(simpleName: String): String =
        "spine.protodata.backend.given.$simpleName"

    @Test
    fun `message types`(@TempDir requestsDir: Path, @TempDir output: Path) {
        val acceptedTypeName = "Message2"

        val filter: DescriptorFilter = {
            if (it is Descriptor) {
                it.name == acceptedTypeName
            } else {
                true
            }
        }

        val pipeline = createPipeline(requestsDir, output, filter)
        pipeline {
            it.messageTypeNames().let { list ->
                list shouldContain qualifiedNameOf(acceptedTypeName)
                list shouldNotContain qualifiedNameOf("Message1")
                list shouldNotContain qualifiedNameOf("Message3")
            }
        }
    }

    @Test
    fun `enum types`(@TempDir output: Path, @TempDir settings: Path) {
        val acceptedTypeName = "EnumType2"

        val filter: DescriptorFilter = {
            if (it is EnumDescriptor) {
                it.name == acceptedTypeName
            } else {
                true
            }
        }

        val pipeline = createPipeline(settings, output, filter)
        pipeline {
            it.enumTypeNames().let { list ->
                list shouldContain qualifiedNameOf(acceptedTypeName)
                list shouldNotContain qualifiedNameOf("EnumType1")
                list shouldNotContain qualifiedNameOf("EnumType3")
            }
        }
    }

    @Test
    fun services(@TempDir output: Path, @TempDir settings: Path) {
        val acceptedServiceName = "Service2"

        val filter: DescriptorFilter = {
            if (it is ServiceDescriptor) {
                it.name == acceptedServiceName
            } else {
                true
            }
        }

        val pipeline = createPipeline(settings, output, filter)
        pipeline {
            it.serviceNames().let { list ->
                list shouldContain qualifiedNameOf(acceptedServiceName)
                list shouldNotContain qualifiedNameOf("Service1")
                list shouldNotContain qualifiedNameOf("Service3")
            }
        }
    }
}
