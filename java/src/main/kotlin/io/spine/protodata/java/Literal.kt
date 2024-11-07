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

import kotlin.reflect.KClass

/**
 * An arbitrary literal.
 *
 * May denote any number or boolean constants.
 */
public class Literal<T : Any>(value: T, type: KClass<T>) : ArbitraryExpression<T>("$value", type) {

    public companion object {

        /**
         * Creates a new instance of [Literal] from the given [value].
         *
         * This factory method is an alternative to passing [KClass] to the constructor.
         * See the class docs for the example usage.
         *
         * @param T The type of the returned value.
         */
        public inline operator fun <reified T : Any> invoke(value: T): Literal<T> =
            Literal(value, T::class)
    }
}

/**
 * A string literal.
 *
 * Represented as the same value as the given string, wrapped in quotation marks.
 * No extra character escaping is performed.
 */
public class StringLiteral(value: String) :
    ArbitraryExpression<String>("\"$value\"", String::class)
