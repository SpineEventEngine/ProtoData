/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.codegen.java

import com.google.common.truth.Truth.assertThat
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.TypedInsertionPoint.BUILDER_IMPLEMENTS
import io.spine.protodata.codegen.java.TypedInsertionPoint.BUILDER_SCOPE
import io.spine.protodata.codegen.java.TypedInsertionPoint.CLASS_SCOPE
import io.spine.protodata.codegen.java.TypedInsertionPoint.ENUM_SCOPE
import io.spine.protodata.codegen.java.TypedInsertionPoint.MESSAGE_IMPLEMENTS
import io.spine.protodata.renderer.InsertionPoint
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.codeLine
import kotlin.io.path.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val PACKAGE = "example"
private val PERSON_NAME = typeName(PACKAGE, "PersonName")
private val ACCOUNT_TYPE = typeName(PACKAGE, "AccountType")

class `'SourceFile' with Java should` : WithSourceFileSet() {

    private lateinit var file: SourceFile

    @BeforeEach
    fun createFile() {
        file = sources.file(Path(JAVA_FILE))
    }

    @Nested
    inner class `find Protoc insertion point` {

        @Test
        fun outer_class_scope() {
            checkPoint(OUTER_CLASS_SCOPE)
        }

        @Test
        fun class_scope() {
            checkPoint(CLASS_SCOPE.forType(PERSON_NAME))
        }

        @Test
        fun builder_scope() {
            checkPoint(BUILDER_SCOPE.forType(PERSON_NAME))
        }

        @Test
        fun enum_scope() {
            checkPoint(ENUM_SCOPE.forType(ACCOUNT_TYPE))
        }

        @Test
        fun message_implements() {
            checkPoint(MESSAGE_IMPLEMENTS.forType(PERSON_NAME))
        }

        @Test
        fun builder_implements() {
            checkPoint(BUILDER_IMPLEMENTS.forType(PERSON_NAME))
        }

        private fun checkPoint(point: InsertionPoint) {
            val comment = "// This is Protoc standard insertion point."
            file.at(point)
                .add(comment)
            val code = file.code()
            assertThat(code)
                .contains(comment)

            val lineIndex = file.lineNumberBy(suffix = comment)
            val insertionPointIndex = file.lineNumberBy(suffix = point.codeLine)
            assertThat(lineIndex)
                .isEqualTo(insertionPointIndex + 1)
        }
    }
}

private fun SourceFile.lineNumberBy(suffix: String): Int =
    lines()
        .mapIndexed { i, line -> i to line.trim() }
        .first { it.second.endsWith(suffix) }
        .first

private fun typeName(packageName: String, simple: String): TypeName =
    TypeName.newBuilder()
        .setPackageName(packageName)
        .setSimpleName(simple)
        .build()
