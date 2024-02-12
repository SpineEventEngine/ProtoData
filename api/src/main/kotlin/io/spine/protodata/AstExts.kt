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

@file:JvmName("Ast")

@file:Suppress("TooManyFunctions")

package io.spine.protodata

import com.google.protobuf.Message

/**
 * Obtains a name of this Protobuf file without the extension.
 */
public fun ProtoFileHeader.nameWithoutExtension(): String {
    val name = file.path.split("/").last()
    val index = name.indexOf(".")
    return if (index > 0) {
        name.substring(0, index)
    } else {
        name
    }
}

/**
 * Tells if this type is a Protobuf primitive type.
 */
public val Type.isPrimitive: Boolean
    get() = hasPrimitive()

/**
 * Tells if this type represents a Protobuf message.
 */
public val Type.isMessage: Boolean
    get() = hasMessage()

/**
 * Tells if this type represents a Protobuf `enum`.
 */
public val Type.isEnum: Boolean
    get() = hasEnumeration()

/**
 * Tells if this type is `google.protobuf.Any`.
 */
public val Type.isAny: Boolean
    get() = isMessage
            && message.packageName.equals("google.protobuf")
            && message.simpleName.equals("Any")

/**
 * Obtains the fully qualified name from this `TypeName`.
 */
public val TypeNameOrBuilder.qualifiedName: String
    get() {
        val names = buildList<String> {
            add(packageName)
            addAll(nestingTypeNameList)
            add(simpleName)
        }
        return names.filter { it.isNotEmpty() }.joinToString(separator = ".")
    }

/**
 * Obtains a [Type] wrapping this `PrimitiveType`.
 */
public fun PrimitiveType.asType(): Type = type { primitive = this@asType }

/**
 * Obtains the package and the name of the type.
 */
public val MessageType.qualifiedName: String
    get() = name.qualifiedName

/**
 * Tells if this field is a Protobuf message.
 */
public val Field.isMessage: Boolean
    get() = type.isMessage

/**
 * Shows if this field is a `map`.
 *
 * If the field is a `map`, the `Field.type` contains the type of the value, and
 * the `Field.map.key_type` contains the type the map key.
 */
public val Field.isMap: Boolean
    get() = hasMap()

/**
 * Shows if this field is a list.
 *
 * In Protobuf `repeated` keyword denotes a sequence of values for a field.
 * However, a map is also treated as a repeated field for serialization reasons.
 * We use the term "list" for repeated fields which are not maps.
 */
public val Field.isList: Boolean
    get() = hasList()

/**
 * Shows if this field repeated.
 *
 * Can be declared in Protobuf either as a `map` or a `repeated` field.
 */
public val Field.isRepeated: Boolean
    get() = isMap || isList

/**
 * Shows if this field is a part of a `oneof` group.
 *
 * If the field is a part of a `oneof`, the `Field.oneof_name` contains the name of that `oneof`.
 */
public val Field.isPartOfOneof: Boolean
    get() = hasOneofName()

/**
 * The field name containing a qualified name of the declaring type.
 */
public val Field.qualifiedName: String
    get() = "${declaringType.qualifiedName}.${name.value}"

/**
 * Looks up an option value by the [optionName].
 *
 * If the option has a Protobuf primitive type, [cls] must be the wrapper type.
 * For example, an `Int32Value` for `int32`, `StringValue` for `string`, etc.
 *
 * @return the value of the option or a `null` if the option is not found.
 */
public fun <T : Message> Iterable<Option>.find(optionName: String, cls: Class<T>): T? {
    val value = firstOrNull { it.name == optionName }?.value
    return value?.unpack(cls)
}
