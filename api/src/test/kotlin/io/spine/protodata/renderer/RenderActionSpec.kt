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

package io.spine.protodata.renderer

import io.kotest.matchers.shouldBe
import io.spine.protodata.MessageType
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.messageType
import io.spine.protodata.typeName
import io.spine.tools.code.Java
import kotlin.io.path.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * The purpose of this test suite is to document the signature of the [RenderAction] class and
 * prevent accidental removal of this abstract class which is not (yet) otherwise used in the code.
 */
@DisplayName("`MessageAction` should")
internal class RenderActionSpec {

    private val typeName = "MyType"

    private val subject = messageType {
        name = typeName { simpleName = typeName }
        file = io.spine.protodata.file { path = "acme/example/my_type.proto" }
    }

    val sourceFile = SourceFile.byCode<Java>(Path("$typeName.java"), """
            public class $typeName {}
            """.trimIndent()
    )

    private val action: MessageAction<Java> = StubAction(subject, sourceFile)

    @Test
    fun `be language-specific`() {
        action.language shouldBe Java
    }

    @Test
    fun `accept 'ProtoDeclaration' and 'SourceFile' as parameters for rendering`() {
        assertDoesNotThrow {
            action.render()
        }
    }
}

private val context = CodeGenerationContext("fiz-buz")

private class StubAction(type: MessageType, file: SourceFile<Java>) :
    MessageAction<Java>(Java, type, file, context) {

    override fun render() {
        // Do nothing.
    }
}
