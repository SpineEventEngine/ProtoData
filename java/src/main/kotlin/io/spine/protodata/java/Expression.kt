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

@file:JvmName("Expressions")

package io.spine.protodata.java

import kotlin.reflect.KClass

/**
 * A piece of Java code that yields a value.
 *
 * @param T The type of the returned value.
 */
public interface Expression<T> : JavaElement

/**
 * An arbitrary Java expression that yields a value.
 *
 * An example usages:
 *
 * ```
 * val four = ArbitraryExpression("2 + 2", JavaInteger::class)
 * val four = ArbitraryExpression<JavaInteger>("2 + 2")
 * ```
 *
 * @param T The type of the returned value.
 * @param code Java code denoting an expression.
 * @param type The class denoting the returned type of the expression.
 */
public open class ArbitraryExpression<T : Any>(
    private val code: String,
    private val type: KClass<T>
) : Expression<T> {

    public companion object {

        /**
         * Creates a new instance of [ArbitraryExpression] from the given [code].
         *
         * This factory method is an alternative to passing [KClass] to the constructor.
         * See the class docs for the example usage.
         *
         * @param T The type of the returned value.
         */
        public inline operator fun <reified T : Any> invoke(code: String): ArbitraryExpression<T> =
            ArbitraryExpression(code, T::class)
    }

    override fun toCode(): String = code

    override fun equals(other: Any?): Boolean =
        other is ArbitraryExpression<*> && code == other.code && type == other.type

    override fun hashCode(): Int = 31 * code.hashCode() + type.hashCode()
}
