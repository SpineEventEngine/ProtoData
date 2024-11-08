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

/**
 * A piece of Java code that yields a value.
 *
 * An example of creating an arbitrary Java expression:
 *
 * ```
 * val four = Expression<Int>("4 + 4")
 * ```
 *
 * @param T The type of the returned value.
 */
public interface Expression<T> : JavaElement {

    public companion object {

        /**
         * Creates a new instance of [Expression] with the given [code].
         *
         * @param T The type of the returned value.
         * @param code Java code denoting an expression.
         */
        public operator fun <T> invoke(code: String): Expression<T> = ArbitraryExpression(code)
    }
}

/**
 * An arbitrary Java expression.
 *
 * This is the basic and default implementation of [Expression].
 *
 * Please note, the type parameter [T] is not enforced in the runtime.
 * The methods of [Any] like [equals] and [hashCode] are solely based
 * on the value of the passed Java [code].
 *
 * For example:
 *
 * ```
 * val intFour = Expression<Int>("4 + 4")
 * val doubleFour = Expression<Double>("4 + 4")
 * println(intFour.equals(doubleFour)) // Prints `true`.
 * ```
 *
 * We would like to avoid passing around `KClass` instances for
 * the following reasons:
 *
 * 1. We are quite OK with only compile-time safety.
 * 2. Too much of ceremony with them.
 *
 * @param T The type of the returned value.
 * @param code Java code denoting an expression.
 */
public open class ArbitraryExpression<T>(code: String): ArbitraryElement(code), Expression<T>
