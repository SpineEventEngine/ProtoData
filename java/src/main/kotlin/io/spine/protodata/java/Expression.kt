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

@file:JvmName("Expressions")

package io.spine.protodata.java

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.protobuf.ByteString
import io.spine.protobuf.TypeConverter
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.Field.CardinalityCase
import io.spine.protodata.ast.Field.CardinalityCase.SINGLE
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.fieldName
import io.spine.protodata.java.MethodCall.Companion.OF

/**
 * A piece of Java code which yields a value.
 */
public sealed class Expression(private val code: String) : JavaElement {

    /**
     * Prints this Java expression.
     */
    public final override fun toCode(): String = code

    final override fun toString(): String = toCode()

    /**
     * Obtains an `Expression` which wraps this `Expression`'s value in a `com.google.protobuf.Any`.
     */
    public fun packToAny(): Expression {
        val type = ClassName(TypeConverter::class)
        return type.call("toAny", arguments = listOf(this))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Expression
        return code == other.code
    }

    override fun hashCode(): Int = code.hashCode()
}

/**
 * A literal `null` expression.
 */
public object Null : Expression("null")

/**
 * A literal `this` reference.
 */
public object This : Expression("this") {

    /**
     * Reference to `this` as a message reference.
     */
    @get:JvmName("asMessage")
    public val asMessage: MessageReference = MessageReference(this.toCode())
}

/**
 * A string literal.
 *
 * Represented as the same value as the given string, wrapped in quotation marks.
 * No extra character escaping is performed.
 */
public class LiteralString(value: String) : Expression("\"$value\"")

private val byteStringClass = ByteString::class.qualifiedName!!

/**
 * An expression which yields the given byte string.
 */
public class LiteralBytes(bytes: ByteString) : Expression(
    "$byteStringClass.copyFrom(new byte[]{${bytes.toByteArray().joinToString()}})"
)

/**
 * An expression represented by the given string.
 *
 * No extra processing is done upon the given code.
 */
public class Literal(value: Any) : Expression(value.toString())

/**
 * Constructs a call to a static method of this class.
 *
 * @param name The name of the method.
 * @param arguments The method arguments.
 * @param generics The method type parameters.
 */
@JvmOverloads
public fun ClassOrEnumName.call(
    name: String,
    arguments: List<Expression> = listOf(),
    generics: List<ClassName> = listOf()
): MethodCall =
    MethodCall(this, name, arguments, generics)

/**
 * Constructs an expression which creates a new builder for this class.
 *
 * Example: `ClassName("com.acme.Bird").newBuilder()` yields
 * `"com.acme.Bird.newBuilder()"`.
 */
public fun ClassName.newBuilder(): MethodCall =
    call("newBuilder")

/**
 * Constructs an expression which obtains the default instance for this class.
 *
 * Example: `ClassName("com.acme.Bird").getDefaultInstance()` yields
 * `"com.acme.Bird.getDefaultInstance()"`.
 */
public fun ClassName.getDefaultInstance(): MethodCall =
    call("getDefaultInstance")

/**
 * Constructs an expression which obtains the Protobuf enum value by
 * the given number from this class.
 *
 * Example: `ClassName("com.acme.Bird").enumValue(1)` yields
 * `"com.acme.Bird.forNumber(1)"`.
 */
public fun EnumName.enumValue(number: Int): MethodCall =
    call("forNumber", listOf(Literal(number)))

/**
 * An expression representing a reference to a Protobuf message value.
 */
public class MessageReference(label: String) : Expression(label) {

    /**
     * Obtains a [FieldAccess] to the [field] of this message.
     */
    public fun field(field: Field): FieldAccess =
        FieldAccess(this, field.name, field.cardinalityCase)

    /**
     * Obtains a [FieldAccess] to the field of this message with the given [fieldName].
     */
    public fun field(fieldName: String, cardinality: CardinalityCase): FieldAccess =
        FieldAccess(this, fieldName, cardinality)
}

/**
 * A selector for an access method for a Protobuf message field.
 *
 * Depending on the field type, may allow generating getters and setters for the field.
 */
public class FieldAccess
internal constructor(
    private val message: Expression,
    name: FieldName,
    cardinality: CardinalityCase = SINGLE
) : FieldConventions(name, cardinality) {

    /**
     * Constructs field access for the given [message] and [name].
     */
    internal constructor(
        message: Expression,
        name: String,
        cardinality: CardinalityCase = SINGLE
    ) : this(message, fieldName { value = name }, cardinality)

    /**
     * A getter expression for the associated field.
     */
    public val getter: MethodCall
        get() = MethodCall(message, getterName)

    /**
     * Constructs a setter expression for the associated field.
     */
    public fun setter(value: Expression): MethodCall =
        MethodCall(message, setterName, value)

    /**
     * Constructs an `addField(..)` expression for the associated field.
     */
    public fun add(value: Expression): MethodCall =
        MethodCall(message, addName, value)

    /**
     * Constructs an `addAllField(..)` expression for the associated field.
     */
    public fun addAll(value: Expression): MethodCall =
        MethodCall(message, addAllName, value)

    /**
     * Constructs an `putField(..)` expression for the associated field.
     */
    public fun put(key: Expression, value: Expression): MethodCall =
        MethodCall(message, putName, key, value)

    /**
     * Constructs an `putAllField(..)` expression for the associated field.
     */
    public fun putAll(value: Expression): MethodCall =
        MethodCall(message, putAllName, value)

    override fun toString(): String {
        return "FieldAccess[$message#${name.value}]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldAccess) return false
        if (!super.equals(other)) return false

        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }
}

/**
 * An expression of a Java method call.
 *
 * Can be a static or an instance method. In the case of the former, the scope is a class name.
 * In the case of the latter — an object reference.
 */
public class MethodCall
/**
 * Creates a new `MethodCall`.
 *
 * @param scope The scope of the method invocation: an instance receiving the method call, or
 *   the name of the class declaring a static method.
 * @param name The name of the method.
 * @param arguments The list of the arguments passed to the method.
 * @param generics The list of the type arguments passed to the method.
 */
@JvmOverloads constructor(
    scope: JavaElement,
    name: String,
    arguments: List<Expression> = listOf(),
    generics: List<ClassName> = listOf()
) : Expression(
    "${scope.toCode()}.${generics.genericTypes()}$name(${arguments.formatParams()})"
) {

    /**
     * Creates a new, non-generified, method call with the given [arguments].
     *
     * @param scope The scope of the method invocation: an instance receiving the method call, or
     *   the name of the class declaring a static method.
     * @param name The name of the method.
     * @param arguments The list of the arguments passed to the method.
     */
    public constructor(
        scope: JavaElement,
        name: String,
        vararg arguments: Expression
    ) : this(scope, name, arguments.toList())

    /**
     * Constructs an expression of calling another method on the result of this method call.
     */
    @JvmOverloads
    public fun chain(method: String, arguments: List<Expression> = listOf()): MethodCall =
        MethodCall(this, method, arguments)

    /**
     * Constructs an expression chaining a setter call.
     */
    public fun chainSet(field: String, value: Expression): MethodCall =
        fieldAccess(field).setter(value)

    /**
     * Constructs an expression chaining a call of an `addField(...)` method.
     */
    public fun chainAdd(field: String, value: Expression): MethodCall =
        fieldAccess(field).add(value)

    /**
     * Constructs an expression chaining a call of an `addAllField(...)` method.
     */
    public fun chainAddAll(field: String, value: Expression): MethodCall =
        fieldAccess(field).addAll(value)

    private fun fieldAccess(fieldName: String) = FieldAccess(this, fieldName)

    /**
     * Constructs an expression chaining a call of the `build()` method.
     */
    public fun chainBuild(): MethodCall =
        chain("build")

    internal companion object {
        internal const val OF = "of"
    }
}

/**
 * Constructs an expression of a list from the given list of [expressions].
 *
 * The resulting expression always yields an instance of Guava `ImmutableList`.
 */
public fun listExpression(expressions: List<Expression>): MethodCall =
    ClassName(ImmutableList::class).call(OF, expressions)

/**
 * Constructs an expression of a list of the given [expressions].
 *
 * The resulting expression always yields an instance of Guava `ImmutableList`.
 */
public fun listExpression(vararg expressions: Expression): MethodCall =
    listExpression(expressions.toList())

/**
 * Constructs an expression of a map of the given [expressions].
 *
 * The resulting expression always yields an instance of Guava `ImmutableMap`.
 *
 * @param expressions The expressions representing the entries.
 * @param keyType The type of the keys in the map;
 *   must be non-`null` if the map is not empty, may be `null` otherwise.
 * @param valueType The type of the values in the map;
 *   must be non-`null` if the map is not empty, may be `null` otherwise.
 */
public fun mapExpression(
    expressions: Map<Expression, Expression>,
    keyType: ClassName?,
    valueType: ClassName?
): MethodCall {
    val immutableMapClass = ClassName(ImmutableMap::class)

    if (expressions.isEmpty()) {
        return immutableMapClass.call(OF)
    }
    checkNotNull(keyType) { "Map key type is not set." }
    checkNotNull(valueType) { "Map value type is not set." }
    var call = immutableMapClass.call("builder", generics = listOf(keyType, valueType))
    expressions.forEach { (k, v) ->
        call = call.chain("put", listOf(k, v))
    }
    return call.chainBuild()
}

/**
 * Formats these class names as type arguments, including the angle brackets.
 */
private fun List<ClassName>.genericTypes() =
    if (isEmpty()) "" else "<${joinToString()}>"

/**
 * Formats these expressions as method parameters, not including the brackets.
 */
private fun List<Expression>.formatParams() =
    joinToString { it.toCode() }
