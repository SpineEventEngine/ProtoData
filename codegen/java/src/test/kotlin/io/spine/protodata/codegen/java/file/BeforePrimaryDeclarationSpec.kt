/*
 * Copyright 2023, TeamDev. All rights reserved.
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
import io.spine.string.ti
import io.spine.text.text
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`BeforePrimaryDeclaration` should locate a top level Java type:")
class BeforePrimaryDeclarationSpec {

    @Test
    fun `a class`() {
        val location = BeforePrimaryDeclaration.locateOccurrence(classSource)
        location.wholeLine shouldBe 6
    }

    @Test
    fun `an interface`() {
        val location = BeforePrimaryDeclaration.locateOccurrence(interfaceSource)
        location.wholeLine shouldBe 1
    }

    @Test
    fun `an enum`() {
        val location = BeforePrimaryDeclaration.locateOccurrence(enumSource)
        location.wholeLine shouldBe 2
    }
}

private const val PACKAGE_NAME = "given.java.file"

private val classSource = text {
    value = """
    /* File header comment. */    
    package $PACKAGE_NAME;

    /** 
     * Top level class Javadoc. 
     */
    public class TopLevel {

        /** Nested class Javadoc. */
        private static class Nested {
        }
    }
    """.ti()
}

private val interfaceSource = text {
    value = """
    /** Top level interface Javadoc. */
    public interface TopLevel {
    }
    """.ti()
}

private val enumSource = text {
    value = """
    // File header comment.    
    package $PACKAGE_NAME;
    public enum TopLevel { ONE, TWO }
    """.ti()
}
