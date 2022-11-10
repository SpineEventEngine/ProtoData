/*
 * Copyright 2022, TeamDev. All rights reserved.
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

@file:Suppress("ConstPropertyName")

package io.spine.protodata.codegen.java

import com.google.common.truth.Truth.assertThat
import io.spine.protodata.Option
import io.spine.protodata.codegen.java.file.javaMultipleFiles
import io.spine.protodata.codegen.java.file.javaOuterClassName
import io.spine.protodata.codegen.java.file.javaPackage
import io.spine.protodata.enumType
import io.spine.protodata.file
import io.spine.protodata.messageType
import io.spine.protodata.typeName
import java.io.File.separatorChar
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Java-related AST extensions should")
class JavaCodegenSpec {

    @Nested
    inner class `Obtain Java class name from` {

        @Test
        fun `top-level message`() {
            val typeName = "Anvil"
            val type = messageType(typeName)
            val file = protoMultipleFiles()

            val className = type.javaClassName(declaredIn = file)

            assertThat(className.binary)
                .isEqualTo("$javaPackageName.$typeName")
        }

        @Test
        fun `nested message`() {
            val nestingType = "RedDynamite"
            val typeName = "Fuse"
            val type = nestedMessageType(typeName, nestingType)
            val file = protoMultipleFiles()

            val className = type.javaClassName(declaredIn = file)

            assertThat(className.binary)
                .isEqualTo("$javaPackageName.$nestingType$$typeName")
        }

        @Test
        fun `message with Java outer class name`() {
            val typeName = "Fuse"
            val type = messageType(typeName)
            val file = protoSingleFile(outerClassName = javaOuterClassName)

            val className = type.javaClassName(declaredIn = file)

            assertThat(className.binary)
                .isEqualTo("${javaPackageName}.${outerClassName}$${typeName}")
        }

        @Test
        fun enum() {
            val typeName = "ExplosiveType"
            val type = enumTypeNamed(typeName)
            val file = protoMultipleFiles()

            val className = type.javaClassName(declaredIn = file)

            assertThat(className.binary)
                .isEqualTo("$javaPackageName.$typeName")
        }
    }

    @Nested
    inner class `Obtain Java file declaring generated class from` {

        @Test
        fun `top-level message`() {
            val typeName = "Anvil"
            val type = messageType(typeName)
            val file = protoMultipleFiles()

            val className = type.javaFile(declaredIn = file)

            assertThat(className.toString())
                .isEqualTo("$packageNameAsPath$typeName.java")
        }

        @Test
        fun `nested message`() {
            val firstNesting = "RedDynamite"
            val typeName = "Fuse"
            val type = withDeeperNesting(typeName, firstNesting)
            val file = protoMultipleFiles()

            val className = type.javaFile(declaredIn = file)

            assertThat(className.toString())
                .isEqualTo("$packageNameAsPath$firstNesting.java")
        }

        @Test
        fun `message with Java outer class name`() {
            val type = messageType("Fuse")
            val file = protoSingleFile(outerClassName = javaOuterClassName)

            val className = type.javaFile(declaredIn = file)

            assertThat(className.toString())
                .isEqualTo("$packageNameAsPath$outerClassName.java")
        }
    }
}

private fun protoSingleFile(outerClassName: Option? = null) = file {
    option.apply {
        add(javaPackage)
        outerClassName?.let { add(it) }
    }
}

private fun protoMultipleFiles(outerClassName: Option? = null) = file {
    option.apply {
        add(javaPackage)
        add(javaMultipleFiles)
        outerClassName?.let { add(it) }
    }
}

private fun messageType(typeName: String) = messageType {
    name = typeName {
        packageName = protoPackageName
        simpleName = typeName
    }
}

private fun nestedMessageType(typeName: String, nestingType: String) = messageType {
    name = typeName {
        packageName = "ecme.example"
        simpleName = typeName
        nestingTypeName.add(nestingType)
    }
}

private fun withDeeperNesting(typeName: String, firstNesting: String) = messageType {
    name = typeName {
        packageName = protoPackageName
        simpleName = typeName
        nestingTypeName.apply {
            add(firstNesting)
            add("Component")
        }
    }
}

private fun enumTypeNamed(typeName: String) = enumType {
    name = typeName {
        packageName = protoPackageName
        simpleName = typeName
    }
}

private const val protoPackageName = "ecme.example"
private const val javaPackageName = "corp.acme.example"

private val packageNameAsPath = javaPackageName.replace('.', separatorChar) + separatorChar

private const val outerClassName = "CartoonExplosives"

private val javaPackage = javaPackage(javaPackageName)
private val javaOuterClassName = javaOuterClassName(outerClassName)