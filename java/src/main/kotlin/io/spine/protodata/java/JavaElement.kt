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

import io.spine.protodata.type.CodeElement
import io.spine.tools.code.Java

/**
 * A piece of Java code.
 */
public interface JavaElement : CodeElement<Java>

/**
 * An arbitrary piece of Java code.
 *
 * This class is the default implementation of [JavaElement].
 *
 * When defining a Java element with the [code] property, avoid leading spaces,
 * tab indents, or new lines. Many elements are converted to PSI counterparts,
 * which often prohibit leading whitespaces.
 *
 * Handling multiline strings:
 *
 * - Use `trimIndent()` for **literal** Kotlin multiline strings:
 *   ```kotlin
 *   val element = AnElement(
 *       """
 *       int x = 42;
 *       int y = 32;
 *       """.trimIndent()
 *   )
 *   ```
 *
 * - For **interpolated** or **concatenated** strings, prefer `trim()`:
 *   ```kotlin
 *   val point = """
 *       int x = 42;
 *       int y = 32;
 *   """.trimIndent()
 *   val element = AnElement(
 *       """
 *       $point
 *       System.out.println(x);
 *       System.out.println(y);
 *       """.trim()
 *   )
 *   ```
 *   `trimIndent()` will fail here, as dynamic content affects indentation detection.
 *
 * Some subclasses may accept [code] with leading spaces—if so, they must document it.
 *
 * @param code Java code without leading whitespaces.
 */
public open class AnElement(public val code: String) : JavaElement  {

    override fun toCode(): String = code

    override fun equals(other: Any?): Boolean =
        other is AnElement && this.code == other.code

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code
}
