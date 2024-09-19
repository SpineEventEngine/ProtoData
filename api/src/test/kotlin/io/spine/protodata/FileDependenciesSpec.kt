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

package io.spine.protodata

import com.google.protobuf.AnyProto
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.TimestampProto
import io.kotest.matchers.shouldBe
import io.spine.option.OptionsProto
import io.spine.protodata.protobuf.FileDependencies
import io.spine.protodata.test.ImportsTestProto
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`FileDependencies` should")
internal class FileDependenciesSpec {

    @Test
    fun `obtain a list with only the file if there are no dependencies`() {
        val anyProto = AnyProto.getDescriptor()
        val deps = FileDependencies(listOf(anyProto))
        deps.asList() shouldBe listOf(anyProto)
    }

    @Test
    fun `obtain imports and the file itself`() {
        val deps = FileDependencies(listOf(ImportsTestProto.getDescriptor()))

        deps.asList() shouldBe listOf(
            // Sorted by least dependencies, and names.
            AnyProto.getDescriptor(),
            DescriptorProtos.getDescriptor(),
            TimestampProto.getDescriptor(),
            OptionsProto.getDescriptor(),
            ImportsTestProto.getDescriptor()
        )
    }

    @Test
    fun `obtain dependencies of multiple files`() {
        val deps = FileDependencies(listOf(
            AnyProto.getDescriptor(),
            TimestampProto.getDescriptor(),
            ImportsTestProto.getDescriptor()
        ))

        deps.asList() shouldBe listOf(
            AnyProto.getDescriptor(),
            DescriptorProtos.getDescriptor(),
            TimestampProto.getDescriptor(),
            OptionsProto.getDescriptor(),
            ImportsTestProto.getDescriptor()
        )
    }
}
