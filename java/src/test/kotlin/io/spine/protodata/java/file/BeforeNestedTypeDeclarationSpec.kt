/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.java.file

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.protodata.backend.ImplicitPluginWithRenderers
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.ClassOrEnumName
import io.spine.protodata.java.EnumName
import io.spine.protodata.java.annotation.TypeAnnotation
import io.spine.protodata.render.CoordinatesFactory.Companion.nowhere
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.string.ti
import io.spine.text.TextCoordinates
import io.spine.tools.code.Java
import java.nio.file.Path
import javax.annotation.processing.Generated
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@DisplayName("`BeforeNestedTypeDeclaration` should")
class BeforeNestedTypeDeclarationSpec {

    @Test
    fun `reject non-nested types`() {
        assertThrows<IllegalArgumentException> {
            BeforeNestedTypeDeclaration(topLevelClassName)
        }
    }

    @Nested
    inner class
    `locate nested` {

        @Test
        fun classes() {
            val nestedLocation = locate(nestedClassName)
            val deeperLocation = locate(deeplyNestedClassName)

            nestedLocation shouldNotBe nowhere
            deeperLocation shouldNotBe nowhere
            deeperLocation.wholeLine shouldBeGreaterThan nestedLocation.wholeLine
        }

        @Test
        fun enums() {
            val location = locate(nestedEnum)
            location shouldNotBe nowhere
            sourceCode.lines()[location.wholeLine] shouldContain "public enum $NESTED_ENUM"
        }

        private fun locate(type: ClassOrEnumName): TextCoordinates {
            val location = BeforeNestedTypeDeclaration(type).locateOccurrence(sourceCode)
            return location
        }
    }

    /**
     * Prepares test environment for the integration tests that use
     * [BeforePrimaryDeclaration] for adding the [Generated] annotation.
     */
    companion object {

        lateinit var javaFile: Path

        @JvmStatic
        @BeforeAll
        fun runPipeline(@TempDir settingsDir: Path, @TempDir input: Path, @TempDir output: Path) {
            val inputClassSrc = input / "$TOP_CLASS.java"
            inputClassSrc.run {
                createFile()
                writeText(sourceCode)
            }

            Pipeline(
                plugin = ImplicitPluginWithRenderers(
                    SuppressWarningsAnnotation(deeplyNestedClassName),
                    SuppressWarningsAnnotation(nestedEnum),
                    SuppressWarningsAnnotation(nestedInterface),
                ),
                sources = SourceFileSet.create(input, output),
                request = CodeGeneratorRequest.getDefaultInstance(),
                settings = SettingsDirectory(settingsDir)
            )()

            javaFile = output / inputClassSrc.name
        }
    }

    @Nested inner class
    `serve insertion for nested` {

        private val generatedCode = javaFile.readText().lines()
        private val expectedAnnotation = "@${SuppressWarnings::class.java.simpleName}"

        private fun lineBefore(declaration: String): String {
            val declarationLine = generatedCode.indexOfFirst { it.contains(declaration) }
            check(declarationLine != -1) {
                "Top declaration `$declaration` was not found."
            }
            return generatedCode[declarationLine - 1]
        }

        @Test
        fun classes() {
            lineBefore("private static class $DEEPLY_NESTED") shouldContain expectedAnnotation
        }

        @Test
        fun enums() {
            lineBefore("public enum $NESTED_ENUM") shouldContain expectedAnnotation
        }

        @Test
        fun interfaces() {
            lineBefore("private interface $NESTED_INTERFACE") shouldContain expectedAnnotation
        }
    }
}

// Stub source code constants.

private const val PACKAGE_NAME = "given.java.source.file"
private const val TOP_CLASS = "TopLevel"
private const val NESTED = "Nested"
private const val DEEPLY_NESTED = "DeeplyNested"
private const val NESTED_ENUM = "NestedEnum"
private const val NESTED_INTERFACE = "NestedInterface"

private val topLevelClassName = ClassName(PACKAGE_NAME, TOP_CLASS)
private val nestedClassName = ClassName(PACKAGE_NAME, TOP_CLASS, NESTED)
private val deeplyNestedClassName = ClassName(PACKAGE_NAME, TOP_CLASS, NESTED, DEEPLY_NESTED)
private val nestedEnum = EnumName(PACKAGE_NAME, TOP_CLASS, NESTED_ENUM)
private val nestedInterface = ClassName(PACKAGE_NAME, TOP_CLASS, NESTED_INTERFACE)

private val sourceCode = """
    /* File header comment. */    
    package $PACKAGE_NAME;

    /** 
     * Top level class Javadoc. 
     */
    public class $TOP_CLASS {

        /** Nested class Javadoc. */
        private static class $NESTED {
    
            private static class $DEEPLY_NESTED {
            }
        }
                
        /** Nested enum Javadoc. */
        public enum $NESTED_ENUM {
            UM,
            DOIS,
            TRÊS
        }

        // Not Javadoc, but a leading comment.                 
        private interface $NESTED_INTERFACE {
            void doNothing() {
                // By design.
            }
        } 
    }
    """.ti() // We deliberately use OS-specific line endings here to simulate loading from disk.

/**
 * Stub renderer which adds the [SuppressWarnings] annotation to the given type.
 */
private class SuppressWarningsAnnotation(subject: ClassOrEnumName) :
    TypeAnnotation<SuppressWarnings>(SuppressWarnings::class.java, subject) {

    override fun renderAnnotationArguments(file: SourceFile<Java>): String = ""
}
