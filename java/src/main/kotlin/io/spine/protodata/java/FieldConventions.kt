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

package io.spine.protodata.java

import io.spine.protodata.Field
import io.spine.protodata.Field.CardinalityCase
import io.spine.protodata.Field.CardinalityCase.LIST
import io.spine.protodata.Field.CardinalityCase.MAP
import io.spine.protodata.Field.CardinalityCase.SINGLE
import io.spine.protodata.FieldName
import io.spine.string.camelCase

/**
 * Abstract base for field access conventions.
 */
public abstract class FieldConventions(
    protected val name: FieldName,
    protected val cardinality: CardinalityCase = SINGLE
) {
    protected val getterName: String
        get() = when (cardinality) {
            LIST -> getListName
            MAP -> getMapName
            else -> prefixed("get")
        }

    private val getListName: String
        get() = "get${name.value.camelCase()}List"

    private val getMapName: String
        get() = "get${name.value.camelCase()}Map"

    protected val setterName: String
        get() = when (cardinality) {
            LIST -> addAllName
            MAP -> putAllName
            else -> prefixed("set")
        }

    protected val addName: String
        get() = prefixed("add")

    protected val addAllName: String
        get() = prefixed("addAll")

    protected val putName: String
        get() = prefixed("put")

    protected val putAllName: String
        get() = prefixed("putAll")

    private fun prefixed(prefix: String): String =
        "$prefix${name.value.camelCase()}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldConventions) return false

        if (name != other.name) return false
        if (cardinality != other.cardinality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + cardinality.hashCode()
        return result
    }
}

/**
 * Obtains the names of the methods associated with the given field.
 *
 * The class is made `open` for accessing `protected` properties of
 * [FieldConventions] via inheriting this class.
 */
public open class FieldMethods(public val field: Field) :
    FieldConventions(field.name, field.cardinalityCase) {

    /**
     * The name of the primary method which sets a value of the field.
    */
    public val primarySetter: String = setterName

    /**
     * The name of the accessor method for the field.
     */
    public val getter: String = getterName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldMethods) return false
        if (!super.equals(other)) return false

        if (field != other.field) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + field.hashCode()
        return result
    }
}
