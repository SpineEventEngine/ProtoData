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

import io.spine.protodata.Type
import io.spine.protodata.Type.KindCase.ENUMERATION
import io.spine.protodata.Type.KindCase.MESSAGE
import io.spine.protodata.Type.KindCase.PRIMITIVE
import io.spine.protodata.value.Value
import io.spine.protodata.type.ValueConverter
import io.spine.tools.code.Java

/**
 * A [ValueConverter] which converts values into Java expressions.
 */
@Suppress("TooManyFunctions")
public class JavaValueConverter(
    private val convention: MessageOrEnumConvention
) : ValueConverter<Java, Expression>() {

    override fun toNull(type: Type): Expression = Null

    override fun toBool(value: Value): Expression = Literal(value.boolValue)

    override fun toDouble(value: Value): Expression = Literal(value.doubleValue)

    override fun toInt(value: Value): Expression = Literal(value.intValue)

    override fun toString(value: Value): Expression = LiteralString(value.stringValue)

    override fun toBytes(value: Value): Expression = LiteralBytes(value.bytesValue)

    override fun toMessage(value: Value): Expression {
        val messageValue = value.messageValue
        val type = messageValue.type
        val className = convention.declarationFor(type).name as ClassName
        return if (messageValue.fieldsMap.isEmpty()) {
            className.getDefaultInstance()
        } else {
            var builder = className.newBuilder()
            messageValue.fieldsMap.forEach { (k, v) ->
                builder = builder.chainSet(k, valueToCode(v))
            }
            builder.chainBuild()
        }
    }

    override fun toEnum(value: Value): MethodCall {
        val enumValue = value.enumValue
        val type = enumValue.type
        val enumClassName = convention.declarationFor(type).name as EnumName
        return enumClassName.enumValue(enumValue.constNumber)
    }

    override fun toList(value: Value): Expression {
        val expressions = value.listValue
            .valuesList
            .map(this::valueToCode)
        return listExpression(expressions)
    }

    override fun toMap(value: Value): MethodCall {
        val firstEntry = value.mapValue.valueList.firstOrNull()
        val firstKey = firstEntry?.key
        val keyClass = firstKey?.type?.toClass() as ClassName?
        val firstValue = firstEntry?.value
        val valueClass = firstValue?.type?.toClass() as ClassName?
        val valuesMap = value.mapValue.valueList.associate {
            valueToCode(it.key) to valueToCode(it.value)
        }
        return mapExpression(valuesMap, keyClass, valueClass)
    }

    private fun Type.toClass(): ClassOrEnumName = when (kindCase) {
        MESSAGE -> convention.declarationFor(message).name
        ENUMERATION -> convention.declarationFor(enumeration).name
        PRIMITIVE -> primitive.toJavaClass()
        else -> error("Expected a valid type.")
    }
}
