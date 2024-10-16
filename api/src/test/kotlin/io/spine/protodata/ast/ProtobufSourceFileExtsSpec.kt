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

package io.spine.protodata.ast

import com.google.protobuf.Empty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.given.type.DeeplyNestedProto
import io.spine.protodata.protobuf.toPbSourceFile
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ProtobufSourceFile` extensions should")
internal class ProtobufSourceFileExtsSpec {

    @Test
    fun `find a top level message type by its simple name`() {
        val protoFile = Empty.getDescriptor().file.toPbSourceFile()
        val simpleName = "Empty"
        val messageType = protoFile.findMessage(simpleName)

        messageType shouldNotBe null
        messageType!!.name.simpleName shouldBe simpleName
    }

    @Test
    fun `find a nested message by its nesting path`() {
        val protoFile = DeeplyNestedProto.getDescriptor().toPbSourceFile()

        val outer = "Outer"
        val inner = "Inner"
        val yetInner = "YetInner"

        // Positive cases.
        protoFile.findMessage(outer)!!.name.simpleName shouldBe outer
        protoFile.findMessage(inner, outer)!!.name.simpleName shouldBe inner
        protoFile.findMessage(yetInner, outer, inner)!!.name.simpleName shouldBe yetInner

        // Negative cases.

        // The nested message does not have a full path to it.
        protoFile.findMessage(yetInner) shouldBe null

        // Not a full path given.
        protoFile.findMessage(yetInner, outer) shouldBe null

        // Wrong order of enclosing types.
        protoFile.findMessage(yetInner, inner, outer) shouldBe null
    }
}
