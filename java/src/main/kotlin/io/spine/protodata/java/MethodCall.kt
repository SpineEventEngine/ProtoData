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

import com.google.protobuf.Message
import kotlin.reflect.KClass

/**
 * The assumed reference to `this` when calling a method within an instance.
 */
public object InstanceScope : ArbitraryExpression<Any>("", Any::class)

/**
 * An expression of a Java method call.
 *
 * Can be a static or an instance method. In the case of the former, the scope is a class name.
 * In the case of the latter — an object reference.
 *
 * @param T The returned type.
 *
 * @param scope The scope of the method invocation: an instance receiving the method call, or
 *   the name of the class declaring a static method.
 * @param name The name of the method.
 * @param returnedType The returned type.
 * @param arguments The list of the arguments passed to the method.
 * @param generics The list of the type arguments passed to the method.
 */
public open class MethodCall<T : Any> @JvmOverloads constructor(
    private val scope: JavaElement,
    name: String,
    returnedType: KClass<T>,
    arguments: List<Expression<*>> = listOf(),
    generics: List<ClassName> = listOf()
) : ArbitraryExpression<T>(
    code = "${scope.toCode()}.${generics.genericTypes()}$name(${arguments.formatParams()})",
    type = returnedType
) {

    public companion object {

        /**
         * Creates a new instance of [MethodCall] from the given parameters.
         *
         * This factory method is an alternative to passing [KClass] to the constructor.
         * See the class docs for the example usage.
         *
         * @param T The type of the returned value.
         */
        public inline operator fun <reified T : Any> invoke(
            scope: JavaElement,
            name: String,
            arguments: List<Expression<*>> = listOf(),
            generics: List<ClassName> = listOf()
        ): MethodCall<T> = MethodCall(scope, name, T::class, arguments, generics)

        public inline operator fun <reified T : Any> invoke(
            scope: JavaElement,
            name: String,
            argument: Expression<*>
        ): MethodCall<T> = MethodCall(scope, name, T::class, listOf(argument))
    }

    /**
     * Constructs an expression of calling another method on the result of this method call.
     */
    @JvmOverloads
    public inline fun <reified R : Any> chain(
        method: String,
        arguments: List<Expression<*>> = listOf(),
    ): MethodCall<R> = MethodCall(this, method, R::class, arguments)

    /**
     * Constructs an expression chaining a call of the `build()` method.
     */
    public inline fun <reified R : Any> chainBuild(): MethodCall<R> = chain<R>("build")

    /**
     * Constructs an expression chaining a setter call.
     */
    public fun chainSet(field: String, value: Expression<*>): MethodCall<MessageBuilder> =
        fieldAccess(field).setter(value)

    /**
     * Constructs an expression chaining a call of an `addField(...)` method.
     */
    public fun chainAdd(field: String, value: Expression<*>): MethodCall<MessageBuilder> =
        fieldAccess(field).add(value)

    /**
     * Constructs an expression chaining a call of an `addAllField(...)` method.
     */
    public fun chainAddAll(field: String, value: Expression<*>): MethodCall<MessageBuilder> =
        fieldAccess(field).addAll(value)

    private fun fieldAccess(fieldName: String) = FieldAccess(
        // Would take a lot of time otherwise.
        // There should be `MethodCall` and a separate `BuilderMethodCall`.
        ArbitraryExpression<Message>(scope.toCode()),
        fieldName
    )
}

/**
 * Formats these class names as type arguments, including the angle brackets.
 */
private fun List<ClassName>.genericTypes() =
    if (isEmpty()) "" else "<${joinToString()}>"

/**
 * Formats these expressions as method parameters, not including the brackets.
 */
private fun List<Expression<*>>.formatParams() =
    joinToString { it.toCode() }
