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

package io.spine.protodata.java.file

import io.spine.protodata.render.CoordinatesFactory.Companion.nowhere
import io.spine.string.ti
import io.spine.text.TextCoordinates
import io.spine.text.TextFactory.text
import io.spine.tools.psi.java.lineNumber

/**
 * An insertion point located just before the primary declaration of a Java file.
 *
 * The primary declaration is the top-level class, interface, annotation, or an enum type,
 * which matches by name with the class.
 *
 * While technically Java allows other top-level declarations is the same file, those are rarely
 * used. `BeforePrimaryDeclaration` does not account for such declarations when searching for
 * a line number.
 *
 * This insertion point is not bound to the contents of the file in `label`,
 * thus allowing this type to be an object.
 *
 * @see BeforeNestedTypeDeclaration
 */
internal object BeforePrimaryDeclaration : io.spine.protodata.render.NonRepeatingInsertionPoint {

    override val label: String
        get() = this.javaClass.simpleName

    override fun locateOccurrence(text: String): TextCoordinates {
        val txt = text(text)
        val file = txt.psiFile()
        if (file.classes.isNotEmpty()) {
            val psiClass = file.classes.first()
            val lineNumber = psiClass.lineNumber
            return atLine(lineNumber)
        }
        logger.atWarning().log { """
            Could not find a primary declaration in the code:
            ```java
            $text
            ```    
            """.ti()
        }
        return nowhere
    }
}
