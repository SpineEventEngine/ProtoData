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

package io.spine.protodata

import com.google.protobuf.Empty
import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.spine.protodata.api.given.Project
import io.spine.protodata.test.packageless.GlobalMessage
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.google.protobuf.Any as ProtoAny

@DisplayName("`MessageType` extensions should")
internal class MessageTypeExtsSpec {

    @Test
    fun `obtain qualified name of the type`() {
        val type = ProtoAny.getDescriptor().toMessageType()
        type.qualifiedName shouldBe "google.protobuf.Any"
    }

    @Test
    fun `obtain entity columns`() {
        val type = Project.getDescriptor().toMessageType()
        val columns = type.columns

        columns.size shouldBe 2
        columns[0].name.value shouldBe "name"
        columns[1].name.value shouldBe "status"
    }

    @Test
    fun `obtain the first field`() {
        val type = Timestamp.getDescriptor().toMessageType()

        type.firstField.name.value shouldBe "seconds"

        assertThrows<IllegalStateException> {
            val empty = Empty.getDescriptor().toMessageType()
            empty.firstField
        }
    }

    @Test
    fun `tell if the message is top-level`() {
        val any = ProtoAny.getDescriptor().toMessageType()
        any.isTopLevel shouldBe true

        val nested = GlobalMessage.LocalMessage.getDescriptor().toMessageType()
        nested.isTopLevel shouldBe false
    }

    @Test
    fun `obtain a field by a short name`() {
        val type = Timestamp.getDescriptor().toMessageType()
        val fieldName = "seconds"
        val field = type.field(fieldName)
        field.name.value shouldBe fieldName

        assertThrows<IllegalStateException> {
            type.field("minutes")
        }
    }
}
