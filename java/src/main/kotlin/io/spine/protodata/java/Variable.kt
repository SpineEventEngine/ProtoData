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
 * Declares a local Java variable.
 *
 * The variable declaration is performed in Java 11 style, without
 * an explicit variable type being specified.
 *
 * An example of the declared variable:
 *
 * ```
 * val ten = Variable<Int>("five", "5 + 5")
 * println("$five") // Prints `var five = 5 + 5;`
 * println("${five.read()}") // Prints `five`.
 * ```
 *
 * @param T The type of the variable.
 * @param name The variable name.
 * @param init The variable initializer.
 */
public class VariableDeclaration<T>(
    public val name: String,
    public val init: Expression<T>,
) : ArbitraryStatement("var $name = $init;") {

    public fun read(): ReadVariable<T> = ReadVariable(name)
}

/**
 * Provides a read access to the variable with the given name.
 *
 * A variable of type [T] is also an [Expression] of the same type.
 *
 * @param T The type of the variable.
 * @param name The name of the variable.
 */
public class ReadVariable<T>(name: String) : ArbitraryExpression<T>(name)
