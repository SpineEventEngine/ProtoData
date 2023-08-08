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

package io.spine.protodata.codegen.java

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.codegen.java.given.TypesTestEnv.enumTypeName
import io.spine.protodata.codegen.java.given.TypesTestEnv.messageTypeName
import io.spine.protodata.codegen.java.given.TypesTestEnv.rejectionTypeName
import io.spine.protodata.codegen.java.given.TypesTestEnv.typeSystem
import kotlin.io.path.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`JavaTypeConvention` should")
class JavaTypeConventionSpec {

    @Test
    fun `convert a message type name into a Java class name`() {
        val converter = JavaTypeConvention(typeSystem)
        val declaration = converter.declarationFor(messageTypeName)
        declaration shouldNotBe null
        val (cls, path) = declaration
        cls.binary shouldBe "ua.acme.example.Foo"
        path shouldBe Path("ua/acme/example/Foo.java")
    }

    @Test
    fun `convert an enum type name into a Java class name`() {
        val converter = JavaTypeConvention(typeSystem)
        val declaration = converter.declarationFor(enumTypeName)
        declaration shouldNotBe null
        val (cls, path) = declaration
        cls.binary shouldBe "ua.acme.example.Kind"
        path shouldBe Path("ua/acme/example/Kind.java")
    }

    @Test
    fun `convert a rejection type name into a rejection throwable class`() {
        val converter = JavaTypeConvention(typeSystem)

        val message = converter.declarationFor(rejectionTypeName)
        message shouldNotBe null
        val throwable = converter.rejectionDeclarationFor(rejectionTypeName)
        throwable shouldNotBe null

        val (messageClass, messagePath) = message
        messageClass.binary shouldBe "ua.acme.example.CartoonRejections\$CannotDrawCartoon"
        messagePath shouldBe Path("ua/acme/example/CartoonRejections.java")

        val (throwableClass, throwablePath) = throwable!!
        throwableClass.binary shouldBe "ua.acme.example.CannotDrawCartoon"
        throwablePath shouldBe Path("ua/acme/example/CannotDrawCartoon.java")
    }

    @Test
    fun `not convert a regular message name to a rejection throwables class`() {
        val converter = JavaTypeConvention(typeSystem)
        val declaration = converter.rejectionDeclarationFor(messageTypeName)
        declaration shouldBe null
    }
}
