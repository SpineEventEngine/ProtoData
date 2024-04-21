/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:JvmName("MessageTypes")

package io.spine.protodata

import io.spine.protodata.type.TypeSystem

/**
 * Obtains the package and the name of the type.
 */
public val MessageType.qualifiedName: String
    get() = name.qualifiedName

/**
 * Obtains column fields of this message type.
 *
 * @return the list if the column fields, or
 *         empty list if none of the fields has the `(column)` option.
 */
public val MessageType.columns: List<Field>
    get() = fieldList.filter { it.optionList.any { option -> option.isColumn } }

/**
 * Obtains message types nested into this one.
 *
 * @param typeSystem
 *         the type system used to resolve types by their names.
 */
public fun MessageType.nestedMessageTypes(typeSystem: TypeSystem): List<MessageType> =
    nestedMessagesList.map { typeName ->
        val type = typeSystem.findMessage(typeName)?.first
        check(type != null) {
            "Unable to obtain a message type named `${typeName.qualifiedName}`."
        }
        type
    }
