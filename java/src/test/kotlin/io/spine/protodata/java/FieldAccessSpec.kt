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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.protobuf.Message
import io.kotest.matchers.shouldBe
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.TypeInstances
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.field
import io.spine.protodata.ast.fieldName
import io.spine.protodata.ast.fieldType
import io.spine.protodata.ast.mapEntryType
import io.spine.protodata.ast.oneofName
import io.spine.protodata.ast.toFieldType
import io.spine.protodata.ast.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`FieldAccess` should")
internal class FieldAccessSpec {

    @Test
    fun `provide a getter for a single field`() {
        val access = singleField().access()
        assertAccessor(access.getter<Any>(), "getIncarnation()")
    }

    @Test
    fun `provide a getter for a oneof field`() {
        val access = oneofField().access()
        assertAccessor(access.getter<Any>(), "getSidekick()")
    }

    @Test
    fun `provide a getter for a list field`() {
        val access = listField().access()
        assertAccessor(
            access.getter<Any>(),
            accessor = "getRouteList()",
        )
    }

    @Test
    fun `provide a getter for a map field`() {
        val access = mapField().access()
        assertAccessor(
            access.getter<Any>(),
            accessor = "getAttributesMap()",
        )
    }

    @Test
    fun `provide a setter`() {
        val access = singleField().access()
        assertAccessor(access.setter(Null), "setIncarnation(null)")
    }

    @Test
    fun `provide add() method`() {
        val access = listField().access()
        assertAccessor(access.add(Literal(42)), "addRoute(42)")
    }

    @Test
    fun `provide addAll() method`() {
        val access = listField().access()
        val expression = access.addAll(listExpression(listOf(Literal(3.14), Literal(2.71))))
        assertAccessor(expression, "addAllRoute($IMMUTABLE_LIST.of(3.14, 2.71))")
    }

    @Test
    fun `provide put() method`() {
        val access = mapField().access()
        val expression = access.put(StringLiteral("foo"), StringLiteral("bar"))
        assertAccessor(expression, "putAttributes(\"foo\", \"bar\")")
    }

    @Test
    fun `provide putAll() method`() {
        val access = mapField().access()
        val mapValue = mapExpression(
            mapOf(StringLiteral("foo") to StringLiteral("bar")),
            keyType = ClassName(String::class),
            valueType = ClassName(String::class)
        )
        val expression = access.putAll(mapValue)
        val type = String::class.java.canonicalName
        assertAccessor(
            expression,
            "putAllAttributes($IMMUTABLE_MAP.<$type, $type>builder().put(\"foo\", \"bar\").build())"
        )
    }
}

private val IMMUTABLE_LIST = ImmutableList::class.qualifiedName!!
private val IMMUTABLE_MAP = ImmutableMap::class.qualifiedName!!

private fun Field.access() = ReadVar<Message>("msg").field(this)

private val stubType: TypeName = typeName {
    simpleName = "StubType"
    packageName = "given.stub.type"
}

private fun singleField() = field {
    name = fieldName { value = "incarnation" }
    type = TypeInstances.string.toFieldType()
    declaringType = stubType
}

private fun listField() = field {
    name = fieldName { value = "route" }
    type = fieldType {
        list = TypeInstances.string
    }
    declaringType = stubType
}

private fun mapField() = field {
    name = fieldName { value = "attributes" }
    type = fieldType {
        map = mapEntryType {
            keyType = PrimitiveType.TYPE_STRING
            valueType = TypeInstances.string
        }
    }
    declaringType = stubType
}

private fun oneofField() = field {
    name = fieldName { value = "sidekick" }
    type = TypeInstances.string.toFieldType()
    enclosingOneof = oneofName { value = "crew" }
    declaringType = stubType
}

private fun assertAccessor(
    expression: Expression<*>,
    accessor: String
) {
    expression.toCode() shouldBe "msg.${accessor}"
}
