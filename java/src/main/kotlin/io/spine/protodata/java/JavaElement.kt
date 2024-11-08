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
 *
 * An example of creating an arbitrary piece of Java code:
 *
 * ```
 * val printOne = JavaElement("System.out.println(1.0);")
 * ```
 */
public interface JavaElement: CodeElement<Java> {

    public companion object {

        /**
         * Creates a new instance of [JavaElement] with the given [code].
         */
        public operator fun invoke(code: String): JavaElement = ArbitraryElement(code)
    }
}

/**
 * An arbitrary piece of Java code.
 *
 * This class is the default implementation of [JavaElement].
 */
public open class ArbitraryElement(private val code: String) : JavaElement {

    override fun toCode(): String = code

    override fun equals(other: Any?): Boolean =
        other is ArbitraryElement && this.code == other.code

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code
}
