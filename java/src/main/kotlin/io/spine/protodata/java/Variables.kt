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

/**
 * Creates a local Java variable with the given [name] and [value].
 *
 * The variable is created in Java 10 style, without an explicit
 * variable type being specified.
 *
 * An example usage:
 *
 * ```
 * val ten = InitVar<Int>("ten", "5 + 5")
 * println(ten) // `var ten = 5 + 5;`
 * ```
 *
 * @param T The type of the variable.
 * @param name The variable name.
 * @param value The variable initializer.
 */
public class InitVar<T>(
    public val name: String,
    public val value: Expression<T>
) : Statement("var $name = $value;") {

    /**
     * Returns an expression that reads the variable value.
     */
    public fun read(): ReadVar<T> = ReadVar(name)
}

/**
 * Creates a local Java variable with explicitly specified [type].
 *
 * An example usage:
 *
 * ```
 * val ten = InitTypedVar<Int>(PrimitiveName.Int, "ten", "5 + 5")
 * println(ten) // `int ten = 5 + 5;`
 * ```
 *
 * @param T The type of the variable.
 * @param type The variable type name.
 * @param name The variable name.
 * @param value The variable initializer.
 */
public class InitTypedVar<T>(
    public val type: JavaTypeName,
    public val name: String,
    public val value: Expression<T>
) : Statement("$type $name = $value;") {

    /**
     * Returns an expression that reads the variable value.
     */
    public fun read(): ReadVar<T> = ReadVar(name)
}

/**
 * Declares a local Java variable with the give [name] and [type].
 *
 * An example usage:
 *
 * ```
 * val ten = DeclVar<Int>(PrimitiveName.Int, "ten")
 * println(ten) // `int ten;`
 * ```
 *
 * @param T The type of the variable.
 * @param type The variable type name.
 * @param name The variable name.
 */
public class DeclVar<T>(
    public val type: JavaTypeName,
    public val name: String
) : Statement("$type $name;") {

    /**
     * Returns an expression that reads the variable value.
     */
    public fun read(): ReadVar<T> = ReadVar(name)
}

/**
 * Assigns a [value] to a variable with the given [name].
 *
 * An example usage:
 *
 * ```
 * val setTen = SetVar<Int>("ten", "5")
 * println(ten) // `ten = 5;`
 * ```
 *
 * @param T The type of the variable.
 * @param name The variable name.
 * @param value The value to assign.
 */
public class SetVar<T>(
    public val name: String,
    public val value: Expression<T>
) : Statement("$name = $value;")  {

    /**
     * Returns an expression that reads the variable value.
     */
    public fun read(): ReadVar<T> = ReadVar(name)
}

/**
 * Provides a read access to the variable with the given name.
 *
 * An example usage:
 *
 * ```
 * val user = ReadVar<User>("user")
 * println(user) // `user`.
 * ```
 *
 * @param T The type of the variable.
 * @param name The name of the variable.
 */
public class ReadVar<T>(name: String) : Expression<T>(name)
