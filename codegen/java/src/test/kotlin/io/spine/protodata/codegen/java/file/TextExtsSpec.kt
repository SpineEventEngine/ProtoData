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

package io.spine.protodata.codegen.java.file

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.codegen.java.ClassName
import io.spine.string.ti
import io.spine.text.text
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`TextExts` should")
internal class TextExtsSpec {

    @Nested inner class
    `locate in 'Text'` {

        @Test
        fun `a class`() {
            val className = ClassName(packageName, topLevelClass)
            val psiClass = sourceCode.locate(className)
            psiClass shouldNotBe null
            psiClass!!.name shouldBe className.simpleName
        }

        @Test
        fun `a nested enum`() {
            val className = ClassName(packageName, topLevelClass, "NestedEnum")
            val psiClass = sourceCode.locate(className)
            psiClass shouldNotBe null
            psiClass!!.name shouldBe className.simpleName
        }

        @Test
        fun `returning 'null' if class or enum is not found`() {
            val className = ClassName(packageName, "NonExistent")
            sourceCode.locate(className) shouldBe null
        }
    }
}

private const val packageName = "given.source.code"
private const val topLevelClass = "TopLevelClass"

private val sourceCode = text {
    value = """
    package $packageName;
    
    import io.spine.protodata.codegen.java.ClassOrEnumName;

    /**
     * A top-level class Javadoc.
     */
    public class $topLevelClass {
        
        private enum NestedEnum {
            VALUE_1,
            VALUE_2
        }
    }
    """.ti()
}
