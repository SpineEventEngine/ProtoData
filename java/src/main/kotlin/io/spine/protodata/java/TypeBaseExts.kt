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

@file:JvmName("Types")

package io.spine.protodata.java

import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.ast.TypeBase
import io.spine.protodata.type.TypeSystem
import io.spine.protodata.type.findHeader
import io.spine.type.shortDebugString

/**
 * Obtains a fully qualified name of this type in the context of the given [TypeSystem].
 *
 * If this type [isPrimitive][TypeBase.isPrimitive], its name does not depend on [TypeSystem] and
 * the result of [toPrimitiveName][io.spine.protodata.ast.PrimitiveType.toPrimitiveName]
 * is returned.
 *
 * @param typeSystem The type system to use for resolving the Java type.
 * @throws IllegalStateException If the field type cannot be converted to a Java counterpart.
 */
public fun TypeBase.javaType(typeSystem: TypeSystem): String {
    if (isPrimitive) {
        return primitiveClassName()
    }
    val declaredIn = typeSystem.findHeader(this)
    check(declaredIn != null) {
        "Unable to locate a header of the file declaring the type `${shortDebugString()}`."
    }
    return javaClassName(declaredIn)
}

private fun TypeBase.primitiveClassName(): String {
    check(isPrimitive) {
        error("The type is not primitive: `${shortDebugString()}`.")
    }
    return primitive.primitiveClass().javaObjectType.simpleName
}

/**
 * Obtains a name of a Java class which corresponds to values with this type.
 */
public fun TypeBase.javaClassName(accordingTo: ProtoFileHeader): String = when {
    isPrimitive -> primitiveClassName()
    isMessage -> message.javaClassName(accordingTo).canonical
    isEnum -> enumeration.javaClassName(accordingTo).canonical
    else -> error("Unable to convert the type `$this` to Java counterpart.")
}

