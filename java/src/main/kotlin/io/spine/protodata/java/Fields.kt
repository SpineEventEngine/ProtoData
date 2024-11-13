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

public open class FieldDeclaration(code: String) : AnElement(code)

public class InitField<T>(
    public val modifiers: String,
    public val type: JavaTypeName,
    public val name: String,
    public val value: Expression<T>
) : FieldDeclaration("$modifiers $type $name = $value;") {

    public fun read(): ReadField<T> = ReadField(name)
}

public class DeclField<T>(
    public val modifiers: String,
    public val type: JavaTypeName,
    public val name: String
) : FieldDeclaration("$modifiers $type $name;") {

    public fun read(): ReadField<T> = ReadField(name)
}

public class SetField<T>(
    public val name: String,
    public val value: Expression<T>,
    public val useThis: Boolean = false
) : Statement("${if (useThis) "this." else ""}$name = $value;") {

    public fun read(): ReadField<T> = ReadField(name)
}

public class ReadField<T>(name: String) : Expression<T>(name)

public fun FieldDeclaration.toPsi(context: PsiElement? = null): PsiField =
    elementFactory.createFieldFromText(toCode(), context)
