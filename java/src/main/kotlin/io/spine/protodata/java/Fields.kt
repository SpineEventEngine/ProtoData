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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * A declaration of a Java field.
 *
 * The declared field may OR may not be initialized,
 * depending on a specific implementation.
 */
public sealed class FieldDeclaration<T>(public val name: String, code: String) : Statement(code) {

    /**
     * Returns an expression that reads the value of this field.
     */
    public fun read(useThis: Boolean = false): ReadField<T> = ReadField(name, useThis)
}

/**
 * Declares and initializes a Java field.
 *
 * An example usage:
 *
 * ```
 * val height = InitField("public final", PrimitiveName.INT, "height", Expression<Int>("180"))
 * println(height) // `public final int height = 180;`
 * ```
 *
 * @param modifiers The field modifiers separated with a space.
 * @param type The field type.
 * @param name The field name.
 * @param value The field value.
 */
public class InitField<T>(
    public val modifiers: String,
    public val type: JavaTypeName,
    name: String,
    public val value: Expression<T>
) : FieldDeclaration<T>(name, "$modifiers $type $name = $value;")

/**
 * Declares a Java field with the given parameters.
 *
 * Please note, the declared field is NOT initialized.
 *
 * An example usage:
 *
 * ```
 * val height = DeclField("public final", PrimitiveName.INT, "height")
 * println(height) // `public final int height;`
 * ```
 *
 * @param modifiers The field modifiers separated with a space.
 * @param type The field type.
 * @param name The field name.
 */
public class DeclField<T>(
    public val modifiers: String,
    public val type: JavaTypeName,
    name: String
) : FieldDeclaration<T>(name, "$modifiers $type $name;") {

    /**
     * Returns an expression that sets the value for this field.
     */
    public fun set(value: Expression<T>, useThis: Boolean = false): SetField<T> =
        SetField(name, value, useThis)
}

/**
 * Assigns a [value] to a field with the given [name].
 *
 * An example usage:
 *
 * ```
 * val setHeight = SetField("height", Expression<Int>("180"), useThis = true)
 * println(setHeight) // `this.height = 180;`
 * ```
 *
 * @param name The field name.
 * @param value The value to assign.
 * @param useThis Tells whether to use the explicit `this` keyword.
 */
public class SetField<T>(
    public val name: String,
    public val value: Expression<T>,
    public val useThis: Boolean = false
) : Statement("${field(name, useThis)} = $value;") {

    /**
     * Returns an expression that reads the field value.
     */
    public fun read(useThis: Boolean = false): ReadField<T> = ReadField(name, useThis)
}

/**
 * Provides a read access to the field with the given name.
 *
 * An example usage:
 *
 * ```
 * val user = ReadField<User>("user", useThis = true)
 * println(user) // `this.user`
 * ```
 *
 * @param T The type of the field.
 * @param name The name of the field.
 * @param useThis Tells whether to use the explicit `this` keyword.
 */
public class ReadField<T>(name: String, useThis: Boolean = false) :
    Expression<T>(field(name, useThis))

/**
 * Creates a new [PsiField] from this Java [FieldDeclaration].
 */
public fun FieldDeclaration<*>.toPsi(context: PsiElement? = null): PsiField =
    elementFactory.createFieldFromText(toCode(), context)

private fun field(name: String, useThis: Boolean) = if (useThis) "this.$name" else name
