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

import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.toEnumType
import io.spine.protodata.ast.toMessageType
import io.spine.protodata.type.TypeSystem
import io.spine.string.shortly
import io.spine.string.simply

/**
 * Obtains a Java class name for this field type.
 *
 * If this type is primitive, the result would be [PrimitiveType]
 * the [class backing] [io.spine.protodata.ast.PrimitiveType.toJavaClass] corresponding
 * primitive value.
 *
 * For message or enum types, the name of the corresponding generated class will be returned.
 *
 * If this field is `repeated`, the method returns the class name of [java.util.List].
 * If this field type is a map, the method returns the class name of [java.util.Map].
 */
public fun FieldType.javaClassName(typeSystem: TypeSystem): ClassName = when {
    isMessage -> message.toMessageType(typeSystem).javaClassName(typeSystem)
    isEnum -> enumeration.toEnumType(typeSystem).javaClassName(typeSystem)
    isPrimitive -> primitive.toJavaClass()
    isList -> ClassName(List::class.java)
    isMap -> ClassName(Map::class.java)
    else -> error("Cannot determine class for `${simply<FieldType>()}`: `${shortly()}`.")
}
