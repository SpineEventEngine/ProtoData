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

import com.google.api.QuotaLimit
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.protobuf.Duration
import com.google.protobuf.FieldMask
import com.google.protobuf.Message
import com.google.protobuf.Parser
import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.spine.protodata.test.Sidekick
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`MethodCall` should")
internal class MethodCallSpec {

    @Test
    fun `print method invocation`() {
        val defaultInstance = ClassName(Timestamp::class).getDefaultInstance()
        defaultInstance.toCode() shouldBe "${Timestamp::class.qualifiedName}.getDefaultInstance()"
    }

    @Test
    fun `print arguments`() {
        val call = MethodCall<Message.Builder>(
            Expression<Message>("msg"),
            name = "putHeader",
            arguments = listOf(StringLiteral("cookez?"), StringLiteral("heck_yeah"))
        )
        call.toCode() shouldBe "msg.putHeader(\"cookez?\", \"heck_yeah\")"
    }

    @Test
    fun `print type arguments`() {
        val call = ClassName(ImmutableMap::class)
            .call<ImmutableMap.Builder<Sidekick, Duration>>(
                "builder",
                arguments = listOf(),
                generics = listOf(ClassName(Sidekick::class), ClassName(Duration::class))
            )
        val immutableMap = ImmutableMap::class.qualifiedName
        val sidekick = Sidekick::class.qualifiedName
        val duration = Duration::class.qualifiedName
        call.toCode() shouldBe "$immutableMap.<$sidekick, $duration>builder()"
    }

    @Test
    fun `omit scope when it is empty or blank`() {
        val emptyScope = This<Message>(explicit = false)
        val call = MethodCall<Message>(emptyScope, "toBuilder")
        call.toCode() shouldBe "toBuilder()"
    }

    @Nested
    inner class Chain {

        @Test
        fun `another method`() {
            val defaultInstance = ClassName(Timestamp::class).getDefaultInstance()
            val getParser = defaultInstance.chain<Parser<Timestamp>>("getParserForType")
            getParser.toCode() shouldBe
                    "${Timestamp::class.qualifiedName}.getDefaultInstance().getParserForType()"
        }

        @Test
        fun `a field setter by name`() {
            val defaultInstance = ClassName(Timestamp::class).newBuilder()
            val setter = defaultInstance.chainSet("nanos", Literal(100_000))
            setter.toCode() shouldBe
                    "${Timestamp::class.qualifiedName}.newBuilder().setNanos(100000)"
        }

        @Test
        fun `a field setter by 'FieldName'`() {
            val defaultInstance = ClassName(Timestamp::class).newBuilder()
            val setter = defaultInstance.chainSet("seconds", Literal(100_000L))
            setter.toCode() shouldBe
                    "${Timestamp::class.qualifiedName}.newBuilder().setSeconds(100000)"
        }

        @Test
        fun `add() method`() {
            val defaultInstance = ClassName(FieldMask::class).newBuilder()
            val setter = defaultInstance.chainAdd("paths", Literal(3))
            setter.toCode() shouldBe
                    "${FieldMask::class.qualifiedName}.newBuilder().addPaths(3)"
        }

        @Test
        fun `addAll() method`() {
            val defaultInstance = ClassName(FieldMask::class).newBuilder()
            val setter = defaultInstance.chainAddAll(
                "paths",
                listExpression(Literal(1), Literal(2))
            )
            setter.toCode() shouldBe
                    "${FieldMask::class.qualifiedName}.newBuilder()" +
                    ".addAllPaths(${ImmutableList::class.qualifiedName}.of(1, 2))"
        }

        @Test
        fun `put() method`() {
            val builder = ClassName(QuotaLimit::class).newBuilder()
                .chainPut(
                    "values",
                    StringLiteral("Standard"),
                    LongLiteral(160)
                )
            builder.toCode() shouldBe
                    "${QuotaLimit::class.qualifiedName}.newBuilder().putValues(\"Standard\", 160L)"
        }

        @Test
        @Suppress("MaxLineLength") // To keep the readability of code literals.
        fun `putAll() method`() {
            val values = mapExpression(
                keyType = ClassName(String::class),
                valueType = ClassName(Long::class.javaObjectType),
                entries = mapOf(
                    StringLiteral("Short") to LongLiteral(80),
                    StringLiteral("Standard") to LongLiteral(160),
                    StringLiteral("Extended") to LongLiteral(560)
                )
            )
            val builder = ClassName(QuotaLimit::class).newBuilder()
                .chainPutAll("values", values)
            builder.toCode() shouldBe
                    "${QuotaLimit::class.qualifiedName}.newBuilder()" +
                    ".putAllValues(" +
                    "com.google.common.collect.ImmutableMap.<java.lang.String, java.lang.Long>builder()" +
                    ".put(\"Short\", 80L).put(\"Standard\", 160L).put(\"Extended\", 560L)" +
                    ".build()" +
                    ")"
        }

        @Test
        fun `build() method`() {
            val defaultInstance = ClassName(FieldMask::class).newBuilder()
            val build = defaultInstance.chainBuild<FieldMask>()
            build.toCode() shouldBe
                    "${FieldMask::class.qualifiedName}.newBuilder().build()"
        }
    }
}
