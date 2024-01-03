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

import io.spine.protodata.codegen.java.ClassOrEnumName
import io.spine.protodata.renderer.CoordinatesFactory.Companion.nowhere
import io.spine.protodata.renderer.NonRepeatingInsertionPoint
import io.spine.string.ti
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.tools.psi.java.lineNumber

/**
 * An insertion point before a nested type declaration.
 *
 * @see BeforePrimaryDeclaration
 */
public class BeforeNestedTypeDeclaration(
    private val name: ClassOrEnumName
) : NonRepeatingInsertionPoint {

    init {
        require(name.isNested) {
            "The type `${name.canonical}` is not nested."
        }
    }

    override val label: String = ""

    override fun locateOccurrence(text: Text): TextCoordinates {
        val psiClass = text.locate(name)
        psiClass?.let {
            val lineNumber = it.lineNumber
            return atLine(lineNumber)
        }
        logger.atWarning().log { """
            Cannot find a declaration of the nested type `$name` in the code:
            ```java
            ${text.printLines()}            
            ```
            """.ti()
        }
        return nowhere
    }
}
