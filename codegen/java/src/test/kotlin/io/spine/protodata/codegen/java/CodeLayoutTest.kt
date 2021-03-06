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

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import io.spine.protobuf.AnyPacker
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.Option
import io.spine.protodata.TypeName
import java.io.File.separatorChar
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `Java-related AST extensions should` {

    @Nested
    inner class `Obtain Java class name from` {

        @Test
        fun `top-level message`() {
            val simpleName = "Anvil"
            val type = MessageType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaMultipleFiles)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className.binary)
                .isEqualTo("$packageName.$simpleName")
        }

        @Test
        fun `nested message`() {
            val nestingTypeName = "RedDynamite"
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName)
                    .addNestingTypeName(nestingTypeName))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaMultipleFiles)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className.binary)
                .isEqualTo("$packageName.$nestingTypeName$$simpleName")
        }

        @Test
        fun `message with Java outer class name`() {
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaOuterClassName)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className.binary)
                .isEqualTo("${packageName}.${outerClassName}$${simpleName}")
        }

        @Test
        fun enum() {
            val simpleName = "ExplosiveType"
            val type = EnumType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaMultipleFiles)
                .build()
            val className = type.javaClassName(declaredIn = file)
            assertThat(className.binary)
                .isEqualTo("$packageName.$simpleName")
        }
    }

    @Nested
    inner class `Obtain Java file declaring generated class from` {

        @Test
        fun `top-level message`() {
            val simpleName = "Anvil"
            val type = MessageType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaMultipleFiles)
                .build()
            val className = type.javaFile(declaredIn = file)
            assertThat(className.toString())
                .isEqualTo("$packageNameAsPath$simpleName.java")
        }

        @Test
        fun `nested message`() {
            val firstNesting = "RedDynamite"
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("ecme.example")
                    .setSimpleName(simpleName)
                    .addNestingTypeName(firstNesting)
                    .addNestingTypeName("Component"))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaMultipleFiles)
                .build()
            val className = type.javaFile(declaredIn = file)
            assertThat(className.toString())
                .isEqualTo("$packageNameAsPath$firstNesting.java")
        }

        @Test
        fun `message with Java outer class name`() {
            val simpleName = "Fuse"
            val type = MessageType
                .newBuilder()
                .setName(
                    TypeName.newBuilder()
                    .setPackageName("acme.example")
                    .setSimpleName(simpleName))
                .build()
            val file = File
                .newBuilder()
                .addOption(javaPackage)
                .addOption(javaOuterClassName)
                .build()
            val className = type.javaFile(declaredIn = file)
            assertThat(className.toString())
                .isEqualTo("$packageNameAsPath$outerClassName.java")
        }
    }
}

private const val packageName = "corp.acme.example"

private val packageNameAsPath = packageName.replace('.', separatorChar) + separatorChar

private const val outerClassName = "CartoonExplosives"

private val javaPackage: Option = Option
    .newBuilder()
    .setName("java_package")
    .setValue(AnyPacker.pack(StringValue.of(packageName)))
    .build()

private val javaMultipleFiles: Option = Option
    .newBuilder()
    .setName("java_multiple_files")
    .setValue(AnyPacker.pack(BoolValue.of(true)))
    .build()

private val javaOuterClassName: Option = Option
    .newBuilder()
    .setName("java_outer_classname")
    .setValue(AnyPacker.pack(StringValue.of(outerClassName)))
    .build()
