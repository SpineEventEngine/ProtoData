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

package io.spine.protodata.codegen

import io.spine.protodata.PrimitiveType
import io.spine.protodata.Type
import io.spine.protodata.TypeName

/**
 * An abstract base for type systems of an application being built by ProtoData.
 *
 * The type system knows the association between the Protobuf types and the types of
 * a target language.
 *
 * @param T the type of language-specific type name.
 * @param V the type of language-specific expression, i.e. the code that yields a value.
 */
public abstract class BaseTypeSystem<T: CodeElement, V: CodeElement>
protected constructor(
    private val knownTypes: Map<TypeName, T>
) {

    /**
     * Converts the given Protobuf type into a language-specific type name.
     */
    public fun typeNameToCode(type: Type): T {
        return when {
            type.hasPrimitive() -> convertPrimitiveType(type.primitive)
            type.hasMessage() -> convertTypeName(type.message)
            type.hasEnumeration() -> convertTypeName(type.enumeration)
            else -> unknownType(type)
        }
    }

    /**
     * Converts the given Protobuf primitive type into a language-specific type name.
     *
     * For example, `PrimitiveType.TYPE_INT64` is converted into `long` in Java.
     */
    protected abstract fun convertPrimitiveType(type: PrimitiveType): T

    /**
     * Converts the given Protobuf type name into a language-specific type name.
     *
     * The type name can represent a message or an enum type.
     */
    public fun convertTypeName(protoName: TypeName): T =
        knownTypes[protoName] ?: unknownType(protoName)
}

private fun unknownType(type: Type): Nothing =
    error("Unknown type: `${type}`.")

private fun unknownType(typeName: TypeName): Nothing =
    error("Unknown type: `${typeName.typeUrl}`.")
