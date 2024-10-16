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

@file:JvmName("PrimitiveTypes")

package io.spine.protodata.ast

import io.spine.protodata.ast.PrimitiveType.TYPE_BOOL

/**
 * Obtains a [Type] wrapping this `PrimitiveType`.
 */
public fun PrimitiveType.toType(): Type = type {
    primitive = this@toType
}

/**
 * Obtains a [Type] wrapping this `PrimitiveType`.
 */
@Deprecated(message = "Please use `toType()` instead.", replaceWith = ReplaceWith("toType()"))
public fun PrimitiveType.asType(): Type = toType()

/**
 * Obtains a name of a field type declared in a message.
 */
public val PrimitiveType.protoName: String
    get() {
        if (this == TYPE_BOOL) {
            return "boolean"
        }
        val knownPrefix = "TYPE_"
        return if (name.startsWith(knownPrefix)) {
            name.removePrefix(knownPrefix).lowercase()
        } else {
            name
        }
    }
