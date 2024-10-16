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

package io.spine.protodata.java

import com.google.protobuf.Empty
import io.kotest.matchers.shouldBe
import io.spine.protodata.ast.file
import io.spine.protodata.ast.findMessage
import io.spine.protodata.ast.messageType
import io.spine.protodata.ast.protoFileHeader
import io.spine.protodata.ast.typeName
import io.spine.protodata.java.file.javaOuterClassName
import io.spine.protodata.java.file.javaPackage
import io.spine.protodata.protobuf.toPbSourceFile
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MessageType` extensions for Java should")
internal class MessageTypeExtsSpec {

    @Test
    fun `find its class, if it is present`() {
        val protoFile = Empty.getDescriptor().file.toPbSourceFile()
        val messageType = protoFile.findMessage("Empty")!!

        messageType.javaClass(protoFile.header) shouldBe Empty::class.java
    }

    @Test
    fun `return 'null', if the class is not available`() {
        val fileName = file {
            path = "given/types/synthetic.proto"
        }
        val type = messageType {
            name = typeName {
                packageName = "given.types"
                simpleName = "Synthetic"
            }
            file = fileName
        }
        val header = protoFileHeader {
            file = fileName
            option.apply {
                add(javaPackage("org.example.given.types"))
                add(javaOuterClassName("SyntheticProto"))
            }
        }

        type.javaClass(header) shouldBe null
    }
}
