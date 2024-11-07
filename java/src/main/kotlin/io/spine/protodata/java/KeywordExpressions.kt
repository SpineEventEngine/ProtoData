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
 * A literal `null` expression.
 */
public object Null : ArbitraryExpression<JavaNull>("null", JavaNull::class)

/**
 * A literal `this` reference.
 *
 * An example usages:
 *
 * ```
 * val this1 = This(JavaObject::class)
 * val this2 = This<JavaObject>()
 * ```
 */
public class This<T : JavaType>(type: KClass<T>) : Expression<T> {

    public companion object {

        /**
         * Creates a new instance of [This] expression for the given type [T].
         *
         * This factory method is an alternative to passing [KClass] to the constructor.
         * See the class docs for the example usage.
         *
         * @param T The type of the returned value.
         */
        public inline operator fun <reified T : JavaType> invoke(): This<T> = This(T::class)
    }

    private val expression = ArbitraryExpression("this", type)

    override fun toCode(): String = expression.toCode()

    override fun equals(other: Any?): Boolean = other is This<*> && expression == other.expression

    override fun hashCode(): Int = expression.hashCode()
}
