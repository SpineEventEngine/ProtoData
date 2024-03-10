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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.spine.protodata.test.TypesTestEnv.serviceNameMultiple
import io.spine.protodata.test.TypesTestEnv.serviceNameSingle
import io.spine.protodata.test.TypesTestEnv.typeSystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import toSourcePath

@DisplayName("`GrpcServiceConvention` should")
internal class GrpcServiceConventionSpec {

    private lateinit var convention: GrpcServiceConvention

    @BeforeEach
    fun createConvention() {
        convention = GrpcServiceConvention(typeSystem)
    }

    @Nested inner class
    `convert a service name into a Java class name` {

        @Test
        fun `when '(java_multiple_files = true)'`() {
            val declaration = convention.declarationFor(serviceNameMultiple)
            declaration shouldNotBe null
            val (cls, path) = declaration
            val expectedClassName = "dev.acme.example.${serviceNameMultiple.simpleName}Grpc"
            cls.binary shouldBe expectedClassName
            path shouldBe expectedClassName.toSourcePath()
            path.toString() shouldEndWith "Grpc.java"
        }

        @Test
        fun `when '(java_multiple_files = false)'`() {
            val declaration = convention.declarationFor(serviceNameSingle)
            declaration shouldNotBe null
            val (cls, path) = declaration
            val expectedClassName = "dev.acme.example.${serviceNameSingle.simpleName}Grpc"
            cls.binary shouldBe expectedClassName
            path shouldBe expectedClassName.toSourcePath()
            path.toString() shouldEndWith "Grpc.java"
        }
    }
}
