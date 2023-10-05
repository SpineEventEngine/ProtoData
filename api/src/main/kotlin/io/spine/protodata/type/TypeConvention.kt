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
import io.spine.tools.code.Language

/**
 * The scheme by which the Protobuf type names are converted into language-specific type names.
 *
 * @param L the type of the target programming language.
 * @param T the type of the programming language element in the target language.
 *
 * @property language a programming language for which the convention is defined.
 */
public interface TypeConvention<L : Language, T: TypeNameElement<L>> {

    /**
     * Given a Protobuf type name, obtains the primary declaration generated from this Proto type.
     *
     * Not all Protobuf types are necessarily converted into declarations. Some conventions may
     * define generated declarations for only a portion of the Protobuf types. For others, this
     * method will return `null`.
     *
     * @param name the name of the type to define the declaration for.
     * @return the declaration generated for the given type or `null` if the declaration
     *         is not defined for the given type.
     */
    public fun declarationFor(name: TypeName): Declaration<L, T>?

    /**
     * The target programming language.
     */
    public val language: L
}
