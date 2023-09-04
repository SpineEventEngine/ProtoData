/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.protodata.type

import io.spine.protodata.TypeName
import io.spine.tools.code.AnyLanguage
import io.spine.tools.code.Language

/**
 * A set of [TypeConvention]s.
 *
 * This can be either a set of conventions for a specific language or just a mishmash of
 * different conventions.
 */
public class TypeConventions<L : Language, N : TypeNameElement<L>>
private constructor(
    private val conventions: Set<TypeConvention<L, N>>
) {

    internal companion object {

        /**
         * Constructs a new `TypeConventions` from a subset of the given [conventions] for a specific language.
         *
         * @param conventions a set of all known [TypeConvention]s.
         * @param target the target language for which the convention is defined.
         * @param L the specific type of the language.
         * @param T the type of the type name code element specific for the `LN` language.
         */
        fun <L : Language, T : TypeNameElement<L>> from(
            conventions: Set<TypeConvention<*, *>>,
            target: L
        ): TypeConventions<L, T> {
            if (target == AnyLanguage) {
                @Suppress("UNCHECKED_CAST") // Logically correct.
                return TypeConventions(conventions as Set<TypeConvention<L, T>>)
            }
            val subset = conventions
                .filter { it.language == target }
                .map {
                    @Suppress("UNCHECKED_CAST") // Ensured by the filter.
                    it as TypeConvention<L, T>
                }
                .toSet()
            return TypeConventions(subset)
        }
    }

    /**
     * Obtains all the possible generated declarations for the given Protobuf type.
     */
    public fun allDeclarationsFor(type: TypeName): Set<Declaration<L, N>> =
        conventions.asSequence()
            .map { it.declarationFor(type) }
            .filter { it != null }
            .map { it!! }
            .toSet()
}
