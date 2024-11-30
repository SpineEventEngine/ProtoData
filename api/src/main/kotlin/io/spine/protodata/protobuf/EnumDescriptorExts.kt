
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

package io.spine.protodata.protobuf

import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import io.spine.protodata.ast.EnumConstant
import io.spine.protodata.ast.EnumType
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.constantName
import io.spine.protodata.ast.copy
import io.spine.protodata.ast.enumConstant
import io.spine.protodata.ast.enumType
import io.spine.protodata.ast.documentation
import io.spine.protodata.ast.toList
import io.spine.protodata.ast.type

/**
 * Obtains the name of this enum type as a [TypeName].
 */
public fun EnumDescriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Converts this enum descriptor into [EnumType] instance.
 *
 * @see EnumDescriptor.toType
 */
public fun EnumDescriptor.toEnumType(): EnumType =
    enumType {
        val typeName = name()
        name = typeName
        option.addAll(options.toList())
        file = getFile().file()
        constant.addAll(values.map { it.toEnumConstant(typeName) })
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        doc = documentation().forEnum(this@toEnumType)
    }

/**
 * Converts this enum descriptor into an instance of [Type].
 *
 * @see EnumDescriptor.toEnumType
 */
public fun EnumDescriptor.toType(): Type = type {
    enumeration = name()
}

/**
 * Converts this enum value descriptor into an [EnumConstant] with options.
 *
 * @see buildConstant
 */
public fun EnumValueDescriptor.toEnumConstant(declaringType: TypeName): EnumConstant {
    val constant = buildConstant(this, declaringType)
    return constant.copy {
        option.addAll(options.toList())
    }
}

/**
 * Converts this enum value descriptor into an [EnumConstant].
 *
 * The resulting [EnumConstant] will not reflect the options on the enum constant.
 *
 * @see toEnumConstant
 */
public fun buildConstant(desc: EnumValueDescriptor, declaringType: TypeName): EnumConstant =
    enumConstant {
        name = constantName { value = desc.name }
        declaredIn = declaringType
        number = desc.number
        orderOfDeclaration = desc.index
        doc = desc.documentation().forEnumConstant(desc)
    }
