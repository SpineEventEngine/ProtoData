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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.protobuf.Message

internal const val OF = "of"

/**
 * Constructs a call to a static method of this class.
 *
 * @param name The name of the method.
 * @param arguments The method arguments.
 * @param generics The method type parameters.
 */
@JvmOverloads
public fun <T> ClassName.call(
    name: String,
    arguments: List<Expression<*>> = listOf(),
    generics: List<ClassName> = listOf()
): MethodCall<T> = MethodCall(this, name, arguments, generics)

/**
 * Constructs an expression which creates a new builder for this class.
 *
 * Example: `ClassName("com.acme.Bird").newBuilder()` yields
 * `"com.acme.Bird.newBuilder()"`.
 */
public fun ClassName.newBuilder(): MethodCall<Message.Builder> =
    MethodCall(this, "newBuilder")

/**
 * Constructs an expression which obtains the default instance for this class.
 *
 * Example: `ClassName("com.acme.Bird").getDefaultInstance()` yields
 * `"com.acme.Bird.getDefaultInstance()"`.
 */
public fun ClassName.getDefaultInstance(): MethodCall<Message> =
    call("getDefaultInstance")

/**
 * Constructs an expression that returns the Protobuf enum value by
 * the given number from this class.
 *
 * Example: `ClassName("com.acme.Bird").enumValue(1)` yields
 * `"com.acme.Bird.forNumber(1)"`.
 */
public fun EnumName.enumValue(number: Int): MethodCall<Message> =
    call("forNumber", listOf(Literal(number)))

/**
 * Constructs an expression of a list from the given list of [expressions].
 *
 * The resulting expression always yields an instance of Guava `ImmutableList`.
 */
public fun listExpression(expressions: List<Expression<*>>): MethodCall<ImmutableList<*>> =
    ClassName(ImmutableList::class).call(OF, expressions)

/**
 * Constructs an expression of a list of the given [expressions].
 *
 * The resulting expression always yields an instance of Guava `ImmutableList`.
 */
public fun listExpression(vararg expressions: Expression<*>): MethodCall<ImmutableList<*>> =
    listExpression(expressions.toList())

/**
 * Constructs an expression that yields Guava's [ImmutableMap] with the given [entries].
 *
 * @param entries The entries to fill the map with, can't be empty.
 * @param keyType The type of the keys in the map.
 * @param valueType The type of the values in the map.
 */
public fun mapExpression(
    entries: Map<Expression<*>, Expression<*>>,
    keyType: ClassName,
    valueType: ClassName
): MethodCall<ImmutableMap<*, *>> {
    require(entries.isNotEmpty()) {
        "Can't construct an expression to yield Guava's `ImmutableMap` with empty `entries`." +
                "Use a parameterless overload of this method to create an empty map," +
                " or pass a non-empty `entries`."
    }

    val immutableMapClass = ClassName(ImmutableMap::class)
    var call = immutableMapClass.call<ImmutableMap.Builder<*, *>>(
        "builder",
        generics = listOf(keyType, valueType)
    )
    entries.forEach { (k, v) ->
        call = call.chain("put", listOf(k, v))
    }

    return call.chainBuild()
}

/**
 * Constructs an expression that yields an empty Guava's [ImmutableMap].
 */
public fun mapExpression(): MethodCall<ImmutableMap<*, *>> {
    val immutableMapClass = ClassName(ImmutableMap::class)
    return immutableMapClass.call(OF)
}
