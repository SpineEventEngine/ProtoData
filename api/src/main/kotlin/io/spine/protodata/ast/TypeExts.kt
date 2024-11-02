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

@file:JvmName("Types")

package io.spine.protodata.ast

import io.spine.protodata.ast.Type.KindCase.ENUMERATION
import io.spine.protodata.ast.Type.KindCase.MESSAGE
import io.spine.protodata.ast.Type.KindCase.PRIMITIVE

/**
 * Obtains a human-readable name of this type.
 *
 * For message and enum types it would be a qualified name of the type.
 * For primitive types it would be a name of the type as used when declaring fields.
 */
public val Type.name: String
    get() = when (kindCase) {
        MESSAGE -> message.qualifiedName
        ENUMERATION -> enumeration.qualifiedName
        PRIMITIVE -> primitive.protoName
        else -> kindCase.name
    }

/**
 * Converts this type into [FieldType] instance.
 */
public fun Type.toFieldType(): FieldType = fieldType {
    val self = this@toFieldType
    val dsl = this@fieldType
    when {
        isMessage -> dsl.message = self.message
        isEnum -> dsl.enumeration = self.enumeration
        isPrimitive -> dsl.primitive = self.primitive
        // This is a safety net for the unlikely extension of `Type`
        // becoming wider than `FieldType`.
        else -> error("Cannot convert `$self` to `${FieldType::class.simpleName}`.")
    }
}

/**
 * A collection of types used by ProtoData.
 */
public object TypeInstances {

    /**
     * The boolean value type.
     */
    public val boolean: Type by lazy {
        type { primitive = PrimitiveType.TYPE_BOOL }
    }

    /**
     * The string value type.
     */
    public val string: Type by lazy {
        type { primitive = PrimitiveType.TYPE_STRING }
    }
}
