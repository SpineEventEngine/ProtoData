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

package io.spine.protodata

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.Empty
import com.google.protobuf.StringValue
import io.spine.protobuf.AnyPacker.pack
import io.spine.protodata.PrimitiveType.TYPE_STRING
import java.io.File.separatorChar
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `AST extensions should` {

    @Nested
    inner class `Obtain Java class name from` {

        @Test
        fun `top-level message`() {
            val simpleName = "Anvil"
            val type = MessageType
                .newBuilder()
                .setName(TypeName.newBuilder()
                                 .setPackageName("ecme.example")
                                 .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(TestEnv.javaPackage)
                .addOption(TestEnv.javaMultipleFiles)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className)
                .isEqualTo("${TestEnv.packageName}.${simpleName}")
        }

        @Test
        fun `nested message`() {
            val nestingTypeName = "RedDynamite"
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName)
                    .addNestingTypeName(nestingTypeName))
                .build()
            val file = File
                .newBuilder()
                .addOption(TestEnv.javaPackage)
                .addOption(TestEnv.javaMultipleFiles)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className)
                .isEqualTo("${TestEnv.packageName}.${nestingTypeName}$${simpleName}")
        }

        @Test
        fun `message with Java outer class name`() {
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(TestEnv.javaPackage)
                .addOption(TestEnv.javaOuterClassName)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className)
                .isEqualTo("${TestEnv.packageName}.${TestEnv.outerClassName}$${simpleName}")
        }
    }

    @Nested
    inner class `Obtain Java file declaring generated class from`() {

        @Test
        fun `top-level message`() {
            val simpleName = "Anvil"
            val type = MessageType
                .newBuilder()
                .setName(TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(TestEnv.javaPackage)
                .addOption(TestEnv.javaMultipleFiles)
                .build()
            val className = type.javaFile(declaredIn = file)
            assertThat(className.toString())
                .isEqualTo("${TestEnv.packageNameAsPath}${simpleName}.java")
        }

        @Test
        fun `nested message`() {
            val firstNesting = "RedDynamite"
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName)
                    .addNestingTypeName(firstNesting)
                    .addNestingTypeName("Component"))
                .build()
            val file = File
                .newBuilder()
                .addOption(TestEnv.javaPackage)
                .addOption(TestEnv.javaMultipleFiles)
                .build()
            val className = type.javaFile(declaredIn = file)
            assertThat(className.toString())
                .isEqualTo("${TestEnv.packageNameAsPath}${firstNesting}.java")
        }

        @Test
        fun `message with Java outer class name`() {
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(TestEnv.javaPackage)
                .addOption(TestEnv.javaOuterClassName)
                .build()
            val className = type.javaFile(declaredIn = file)
            assertThat(className.toString())
                .isEqualTo("${TestEnv.packageNameAsPath}${TestEnv.outerClassName}.java")
        }
    }

    @Nested
    inner class `Check if a field is` {

        @Test
        fun `repeated if list`() {
            val field = Field
                .newBuilder()
                .setList(Empty.getDefaultInstance())
                .buildPartial()
            assertThat(field.isRepeated())
                .isTrue()
        }

        @Test
        fun `repeated if map`() {
            val field = Field
                .newBuilder()
                .setMap(Field.OfMap.newBuilder()
                    .setKeyType(TYPE_STRING)
                    .build())
                .buildPartial()
            assertThat(field.isRepeated())
                .isTrue()
        }

        @Test
        fun `not repeated`() {
            val field = Field
                .newBuilder()
                .setSingle(Empty.getDefaultInstance())
                .buildPartial()
            assertThat(field.isRepeated())
                .isFalse()
        }
    }
}

private object TestEnv {

    const val packageName = "corp.ackme.example"
    val packageNameAsPath = packageName.replace('.', separatorChar) + separatorChar
    const val outerClassName = "CartoonExplosives"
    val javaPackage: Option = Option
        .newBuilder()
        .setName("java_package")
        .setValue(pack(StringValue.of(packageName)))
        .build()
    val javaMultipleFiles: Option = Option
        .newBuilder()
        .setName("java_multiple_files")
        .setValue(pack(BoolValue.of(true)))
        .build()
    val javaOuterClassName: Option = Option
        .newBuilder()
        .setName("java_outer_classname")
        .setValue(pack(StringValue.of(outerClassName)))
        .build()
}
