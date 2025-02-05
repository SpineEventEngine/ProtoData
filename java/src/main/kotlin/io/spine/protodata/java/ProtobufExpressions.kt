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

// The fact that `PredefinedByteString` is a single class here is a coincidence.
@file:Suppress("MatchingDeclarationName")

package io.spine.protodata.java

import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.Any as ProtoAny
import io.spine.protobuf.TypeConverter
import io.spine.protodata.ast.Cardinality
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.cardinality

/**
 * An expression which yields the given Protobuf [ByteString]
 * using [ByteString.copyFrom] method.
 */
public class CopyByteString(bytes: ByteString) : Expression<ByteString>(
    "$ByteStringClass.copyFrom(new byte[]{${bytes.toByteArray().joinToString()}})"
)

private val ByteStringClass = ByteString::class.qualifiedName!!

/**
 * Wraps this [Expression] into Protobuf `Any` using [TypeConverter.toAny] method.
 */
public fun Expression<*>.packToAny(): Expression<ProtoAny> {
    val type = ClassName(TypeConverter::class)
    return type.call("toAny", arguments = listOf(this))
}

/**
 * Obtains a [FieldAccess] to the [field] of this message.
 */
public fun Expression<Message>.field(field: Field): FieldAccess =
    FieldAccess(this, field.name, field.type.cardinality)

/**
 * Obtains a [FieldAccess] to the field of this message with the given [name].
 */
public fun Expression<Message>.field(name: String, cardinality: Cardinality): FieldAccess =
    FieldAccess(this, name, cardinality)


/**
 * Constructs an expression which creates a new builder for this [Message].
 *
 * Example: `Expression("myMessage").toBuilder()` yields `"myMessage.toBuilder()"`.
 */
public fun Expression<Message>.toBuilder(): MethodCall<Message.Builder> =
    MethodCall(this, "toBuilder")
