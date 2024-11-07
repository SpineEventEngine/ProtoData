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

import com.google.protobuf.ByteString
import com.google.protobuf.Message

public class BuilderMethodCall @JvmOverloads constructor(
    private val scope: Expression<Message>,
    name: String,
    arguments: List<Expression<*>> = listOf(),
    generics: List<ClassName> = listOf()
) : MethodCall<ProtoBuilder>(scope, name, ProtoBuilder::class, arguments, generics) {

    public constructor(
        scope: Expression<Message>,
        name: String,
        argument: Expression<*>
    ) : this(scope, name, listOf(argument))

    /**
     * Constructs an expression chaining a call of the `build()` method.
     */
    public fun chainBuild(): MethodCall<Message> = chain<Message>("build")

    /**
     * Constructs an expression chaining a setter call.
     */
    public fun chainSet(field: String, value: Expression<*>): BuilderMethodCall =
        fieldAccess(field).setter(value)

    /**
     * Constructs an expression chaining a call of an `addField(...)` method.
     */
    public fun chainAdd(field: String, value: Expression<*>): BuilderMethodCall =
        fieldAccess(field).add(value)

    /**
     * Constructs an expression chaining a call of an `addAllField(...)` method.
     */
    public fun chainAddAll(field: String, value: Expression<*>): BuilderMethodCall =
        fieldAccess(field).addAll(value)

    private fun fieldAccess(fieldName: String) = FieldAccess(scope, fieldName)
}

/**
 * An expression which yields the given [ByteString].
 */
public class LiteralBytes(bytes: ByteString) : ArbitraryExpression<ByteString>(
    code = "$ByteStringClass.copyFrom(new byte[]{${bytes.toByteArray().joinToString()}})",
    type = ByteString::class
)

private val ByteStringClass = ByteString::class.qualifiedName!!
