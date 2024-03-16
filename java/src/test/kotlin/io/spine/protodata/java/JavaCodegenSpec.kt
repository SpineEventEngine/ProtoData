/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.java

import io.kotest.matchers.shouldBe
import io.spine.protodata.File
import io.spine.protodata.Option
import io.spine.protodata.enumType
import io.spine.protodata.file
import io.spine.protodata.java.file.javaMultipleFiles
import io.spine.protodata.java.file.javaOuterClassName
import io.spine.protodata.java.file.javaPackage
import io.spine.protodata.messageType
import io.spine.protodata.protoFileHeader
import io.spine.protodata.typeName
import java.io.File.separatorChar
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Java-related AST extensions should")
internal class JavaCodegenSpec {

    @Nested inner class
    `Obtain Java class name from` {

        @Test
        fun `top-level message`() {
            val typeName = "Anvil"
            val type = messageType(typeName)
            val header = headerMultipleFiles()

            val className = type.javaClassName(accordingTo = header)

            className.binary shouldBe "$JAVA_PACKAGE_NAME.$typeName"
        }

        @Test
        fun `nested message`() {
            val nestingType = "RedDynamite"
            val typeName = "Fuse"
            val type = nestedMessageType(typeName, nestingType)
            val header = headerMultipleFiles()

            val className = type.javaClassName(accordingTo = header)

            className.binary shouldBe "$JAVA_PACKAGE_NAME.$nestingType$$typeName"
        }

        @Test
        fun `message with Java outer class name`() {
            val typeName = "Fuse"
            val type = messageType(typeName)
            val header = headerSingleFile(outerClassName = javaOuterClassName)

            val className = type.javaClassName(accordingTo = header)

            className.binary shouldBe "${JAVA_PACKAGE_NAME}.${OUTER_CLASS_NAME}$${typeName}"
        }

        @Test
        fun enum() {
            val typeName = "ExplosiveType"
            val type = enumTypeNamed(typeName)
            val header = headerMultipleFiles()

            val className = type.javaClassName(accordingTo = header)

            className.binary shouldBe "$JAVA_PACKAGE_NAME.$typeName"
        }
    }

    @Nested inner class
    `Obtain Java file declaring generated class from` {

        @Test
        fun `top-level message`() {
            val typeName = "Anvil"
            val type = messageType(typeName)
            val header = headerMultipleFiles()

            val className = type.javaFile(accordingTo = header)

            className.toString() shouldBe "$packageNameAsPath$typeName.java"
        }

        @Test
        fun `nested message`() {
            val firstNesting = "RedDynamite"
            val typeName = "Fuse"
            val type = withDeeperNesting(typeName, firstNesting)
            val header = headerMultipleFiles()

            val className = type.javaFile(accordingTo = header)

            className.toString() shouldBe "$packageNameAsPath$firstNesting.java"
        }

        @Test
        fun `message with Java outer class name`() {
            val type = messageType("Fuse")
            val header = headerSingleFile(outerClassName = javaOuterClassName)

            val className = type.javaFile(accordingTo = header)

            className.toString() shouldBe "$packageNameAsPath$OUTER_CLASS_NAME.java"
        }
    }
}

private fun headerSingleFile(outerClassName: Option? = null) = protoFileHeader {
    file = fileName
    option.apply {
        add(javaPackage)
        outerClassName?.let { add(it) }
    }
}

private fun headerMultipleFiles(outerClassName: Option? = null) = protoFileHeader {
    file = fileName
    option.apply {
        add(javaPackage)
        add(javaMultipleFiles)
        outerClassName?.let { add(it) }
    }
}

private val fileName: File = file {
    path = "given/file.proto"
}

private fun messageType(typeName: String) = messageType {
    name = typeName {
        packageName = PROTO_PACKAGE_NAME
        simpleName = typeName
    }
    file = fileName
}

private fun nestedMessageType(typeName: String, nestingType: String) = messageType {
    name = typeName {
        packageName = "ecme.example"
        simpleName = typeName
        nestingTypeName.add(nestingType)
    }
    file = fileName
}

private fun withDeeperNesting(typeName: String, firstNesting: String) = messageType {
    name = typeName {
        packageName = PROTO_PACKAGE_NAME
        simpleName = typeName
        nestingTypeName.apply {
            add(firstNesting)
            add("Component")
        }
    }
    file = fileName
}

private fun enumTypeNamed(typeName: String) = enumType {
    name = typeName {
        packageName = PROTO_PACKAGE_NAME
        simpleName = typeName
    }
    file = fileName
}

private const val PROTO_PACKAGE_NAME = "ecme.example"
private const val JAVA_PACKAGE_NAME = "corp.acme.example"

private val packageNameAsPath = JAVA_PACKAGE_NAME.replace('.', separatorChar) + separatorChar

private const val OUTER_CLASS_NAME = "CartoonExplosives"

private val javaPackage = javaPackage(JAVA_PACKAGE_NAME)
private val javaOuterClassName = javaOuterClassName(OUTER_CLASS_NAME)
