///*
// * Copyright 2023, TeamDev. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Redistribution and use in source and/or binary forms, with or without
// * modification, must retain the above copyright notice and the following
// * disclaimer.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package io.spine.protodata.java
//
//import assertCode
//import com.google.protobuf.ByteString
//import io.spine.protobuf.TypeConverter
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//
//@DisplayName("`Expression` should")
//internal class ExpressionSpec {
//
//    @Test
//    fun `pack value into 'Any'`() {
//        val expression = Literal("messageVar")
//        val packed = expression.packToAny()
//        assertCode(packed, "${TypeConverter::class.qualifiedName}.toAny(messageVar)")
//    }
//
//    @Nested
//    inner class Print {
//
//        @Test
//        fun `Java string literal`() = assertCode(LiteralString("foo"), "\"foo\"")
//
//        @Test
//        fun `'ByteString' constructor invocation`() {
//            val bytes = "foobar".toByteArray()
//            val expression = LiteralBytes(ByteString.copyFrom(bytes))
//            assertCode(
//                expression,
//                "${ByteString::class.qualifiedName}.copyFrom(new byte[]{${bytes.joinToString()}})"
//            )
//        }
//
//        @Test
//        fun `literal 'null'`() = assertCode(Null, "null")
//
//        @Test
//        fun `literal 'this'`() = assertCode(This, "this")
//
//        @Test
//        fun `a number`() = assertCode(Literal(42), "42")
//
//        @Test
//        fun `a boolean`() = assertCode(Literal(false), "false")
//
//        @Test
//        fun anything() {
//            val anything = "Frankie says relax"
//            val expression = Literal(anything)
//            assertCode(expression, anything)
//        }
//    }
//}
