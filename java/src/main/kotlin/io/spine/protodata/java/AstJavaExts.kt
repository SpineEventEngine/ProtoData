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

package io.spine.protodata.java

import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.isPrimitive
import io.spine.string.camelCase

/**
 * Obtains the name of the field in Java case, as if it were declared as a field
 * in a Java class.
 *
 * @return this field name in `lowerCamelCase` form.
 */
public fun FieldName.javaCase(): String {
    val camelCase = value.camelCase()
    return camelCase.replaceFirstChar { it.lowercaseChar() }
}

/**
 * Obtains the name of the primary setter method for this field.
 */
public val Field.primarySetterName: String
    get() = io.spine.protodata.java.FieldMethods(this).primarySetter

/**
 * Obtains the name of the accessor method for this field.
 */
public val Field.getterName: String
    get() = io.spine.protodata.java.FieldMethods(this).getter

/**
 * Tells if the type of this field corresponds to a primitive type in Java.
 */
public val Field.isJavaPrimitive: Boolean
    get() = if (!type.isPrimitive) {
        false
    } else when (type.primitive) {
        TYPE_STRING, TYPE_BYTES -> false
        else -> true
    }
