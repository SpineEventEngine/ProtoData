/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableMap
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Duration
import com.google.protobuf.FieldMask
import com.google.protobuf.Timestamp
import io.spine.protodata.test.Sidekick
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `'MethodCall' should` {

    @Test
    fun `print method invocation`() {
        val defaultInstance = ClassName(Timestamp::class).getDefaultInstance()
        assertThat(defaultInstance.toCode())
            .isEqualTo("${Timestamp::class.qualifiedName}.getDefaultInstance()")
    }

    @Test
    fun `print arguments`() {
        val call = MethodCall(MessageReference("msg"), name = "putHeader", arguments = listOf(
            LiteralString("cookez?"), LiteralString("heck_yeah")
        ))
        assertThat(call.toCode())
            .isEqualTo("msg.putHeader(\"cookez?\", \"heck_yeah\")")
    }

    @Test
    fun `print type arguments`() {
        val call = ClassName(ImmutableMap::class).call(
            "builder",
            arguments = listOf(), generics = listOf(
                ClassName(Sidekick::class), ClassName(Duration::class)
            )
        )
        val immutableMap = ImmutableMap::class.qualifiedName
        val sidekick = Sidekick::class.qualifiedName
        val duration = Duration::class.qualifiedName
        assertThat(call.toCode())
            .isEqualTo("$immutableMap.<$sidekick, $duration>builder()")
    }

    @Nested
    inner class Chain {

        @Test
        fun `another method`() {
            val defaultInstance = ClassName(Timestamp::class).getDefaultInstance()
            val getParser = defaultInstance.chain("getParserForType")
            assertThat(getParser.toCode())
                .isEqualTo(
                    "${Timestamp::class.qualifiedName}.getDefaultInstance().getParserForType()"
                )
        }

        @Test
        fun `a field setter by name`() {
            val defaultInstance = ClassName(Timestamp::class).newBuilder()
            val setter = defaultInstance.chainSet("nanos", Literal(100_000))
            assertThat(setter.toCode())
                .isEqualTo("${Timestamp::class.qualifiedName}.newBuilder().setNanos(100000)")
        }

        @Test
        fun `a field setter by 'FieldName'`() {
            val defaultInstance = ClassName(Timestamp::class).newBuilder()
            val setter = defaultInstance.chainSet("seconds", Literal(100_000L))
            assertThat(setter.toCode())
                .isEqualTo("${Timestamp::class.qualifiedName}.newBuilder().setSeconds(100000)")
        }

        @Test
        fun `add() method`() {
            val defaultInstance = ClassName(FieldMask::class).newBuilder()
            val setter = defaultInstance.chainAdd("paths", Literal(3))
            assertThat(setter.toCode())
                .isEqualTo("${FieldMask::class.qualifiedName}.newBuilder().addPaths(3)")
        }

        @Test
        fun `build() method`() {
            val defaultInstance = ClassName(FieldMask::class).newBuilder()
            val setter = defaultInstance.chainBuild()
            assertThat(setter.toCode())
                .isEqualTo("${FieldMask::class.qualifiedName}.newBuilder().build()")
        }
    }
}
