/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.kotest.matchers.shouldBe
import org.checkerframework.checker.nullness.qual.Nullable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`AnnotatedClassName` should annotate")
internal class AnnotatedClassNameSpec {

    private val annotationClass = ClassName(Nullable::class)
    private val annotation = "@org.checkerframework.checker.nullness.qual.Nullable"

    @Test
    fun `simple class name`() {
        val simpleName = "String"
        val string = ClassName(packageName = "", simpleName)
        val nullableString =  AnnotatedClassName(string, annotationClass)
        nullableString.canonical shouldBe "$annotation $simpleName"
    }

    @Test
    fun `fully-qualified class name`() {
        val string = ClassName(String::class)
        val nullableString =  AnnotatedClassName(string, annotationClass)
        nullableString.canonical shouldBe "java.lang.$annotation String"
    }

    @Test
    fun `class name with multiple simple names`() {
        val entry = ClassName(Map.Entry::class)
        val nullableEntry =  AnnotatedClassName(entry, annotationClass)
        nullableEntry.canonical shouldBe "java.util.Map.$annotation Entry"
    }
}
