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

@file:JvmName("Fields")

package io.spine.protodata.ast

import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.google.protobuf.Message
import io.spine.protobuf.defaultInstance
import io.spine.protodata.type.TypeSystem
import io.spine.string.camelCase
import io.spine.string.simply
import io.spine.type.typeName
import java.io.File

/**
 * Obtains the type of this field as [Type] instance.
 *
 * @throws IllegalStateException If the field is a list or a map.
 */
public fun Field.toType(): Type = type.toType()

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
    get() = type.isMap

/**
 * Shows if this field is a list.
 *
 * In Protobuf `repeated` keyword denotes a sequence of values for a field.
 * However, a map is also treated as a repeated field for serialization reasons.
 * We use the term "list" for repeated fields which are not maps.
 */
public val Field.isList: Boolean
    get() = type.isList

/**
 * Shows if this field repeated.
 *
 * Can be declared in Protobuf either as a `map` or a `repeated` field.
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Please use either `isList` or `isMap`." /*
        We do not want this duality in our code. Also, this introduces confusion with the Protobuf
        declaration `repeated`. The fact that maps are implemented internally as repeated entries
        should not leak into public API. */
)
public val Field.isRepeated: Boolean
    get() = isMap || isList

/**
 * Shows if this field is a part of a `oneof` group.
 *
 * If the field is a part of a `oneof`, the `Field.oneof_name` contains the name of that `oneof`.
 */
public val Field.isPartOfOneof: Boolean
    get() = hasEnclosingOneof()

/**
 * The field name containing a qualified name of the declaring type.
 */
public val Field.qualifiedName: String
    get() = "${declaringType.qualifiedName}.${name.value}"

/**
 * Obtains a `CamelCase` version of this field name.
 */
public val FieldName.camelCase: String
    get() = value.camelCase()

/**
 * Obtains the path to a Protobuf source file which declares
 * the message type to which this field belongs.
 *
 * @return The full path, if the declaring file is listed among [TypeSystem.compiledProtoFiles],
 *  otherwise the path is relative.
 */
public fun Field.declaringFile(typeSystem: TypeSystem): File {
    val messageType = declaringType.toMessageType(typeSystem)
    val file = messageType.file.toPath().toFile()
    val fullPath = typeSystem.compiledProtoFiles.find(file)
    return fullPath ?: file
}

/**
 * Creates a new instance of [FieldRef] for this [Field].
 */
public val Field.ref: FieldRef
    get() = fieldRef {
        type = declaringType
        name = this@ref.name
    }

/**
 * Finds the option with the given type [T] applied to this [Field].
 *
 * @param T The type of the option.
 * @return the option or `null` if there is no option with such a type applied to the field.
 * @see Field.option
 */
public inline fun <reified T : Message> Field.findOption(): T? {
    val typeUrl = T::class.java.defaultInstance.typeName.toUrl().value()
    val option = optionList.find { opt ->
        opt.value.typeUrl == typeUrl
    }
    return option?.unpack()
}

/**
 * Obtains the option with the given type [T] applied to this [Field].
 *
 * Invoke this function if you are sure the option with the type [T] is applied
 * to the receiver field. Otherwise, please use [findOption].
 *
 * @param T The type of the option.
 * @return the option.
 * @throws IllegalStateException if the option is not found.
 * @see Field.findOption
 */
public inline fun <reified T : Message> Field.option(): T = findOption<T>()
    ?: error("The field `${qualifiedName}` must have the `${simply<T>()}` option.")

/**
 * Finds the option applied to this [Field] by its generated extension type.
 *
 * @param [extension] The extension type used to represent the option.
 * @return the option or `null` if there is no option with such a type applied to the field.
 * @see [option]
 */
public fun Field.findOption(extension: GeneratedExtension<*, *>): Option? =
    optionList.find { it.name == extension.descriptor.name && it.number == extension.number }

/**
 * Obtains the option applied to this [Field] by its generated extension type.
 *
 * Invoke this function if you are sure the option with the [extension] type
 * is applied to the receiver field. Otherwise, please use [findOption].
 *
 * @param [extension] The extension type used to represent the option.
 * @throws IllegalStateException if the option is not found.
 * @see [findOption]
 */
public fun Field.option(extension: GeneratedExtension<*, *>): Option = findOption(extension)
    ?: error("The field `${qualifiedName}` must have the `${extension.descriptor.name}` option.")
