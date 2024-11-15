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

import io.spine.protodata.type.NameElement
import io.spine.tools.code.Java

/**
 * A name of a Java type.
 */
public abstract class JavaTypeName : NameElement<Java>, JavaElement {

    /**
     * The fully qualified name that uniquely identifies the type within
     * the Java language.
     *
     * May contain the following:
     *
     * 1. For classes and interfaces, this includes the package and any enclosing classes.
     * 2. For primitives, since they are not part of any package or class, the canonical
     * name is the same as the simple name.
     * 3. When canonical name contains generics, the type parameters are printed "as is".
     */
    public abstract val canonical: String

    override fun toCode(): String = canonical

    override fun toString(): String = toCode()

    override fun equals(other: Any?): Boolean =
        other is JavaTypeName && other.toCode() == toCode()

    override fun hashCode(): Int = toCode().hashCode()

    // Primitive types.
    public object VOID : JavaTypeName() {
        override val canonical: String = "void"
    }
    public object BOOLEAN : JavaTypeName() {
        override val canonical: String = "boolean"
    }
    public object BYTE : JavaTypeName() {
        override val canonical: String = "byte"
    }
    public object SHORT : JavaTypeName() {
        override val canonical: String = "short"
    }
    public object INT : JavaTypeName() {
        override val canonical: String = "int"
    }
    public object LONG : JavaTypeName() {
        override val canonical: String = "long"
    }
    public object CHAR : JavaTypeName() {
        override val canonical: String = "char"
    }
    public object FLOAT : JavaTypeName() {
        override val canonical: String = "float"
    }
    public object DOUBLE : JavaTypeName() {
        override val canonical: String = "double"
    }
}

/**
 * A name of a Java reference type.
 */
public abstract class ObjectName : JavaTypeName()
