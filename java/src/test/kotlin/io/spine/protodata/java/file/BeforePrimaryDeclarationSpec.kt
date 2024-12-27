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

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.java.annotation.GeneratedTypeAnnotation
import io.spine.protodata.params.PipelineParameters
import io.spine.protodata.render.SourceFileSet
import io.spine.string.ti
import java.nio.file.Path
import javax.annotation.processing.Generated
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`BeforePrimaryDeclaration` should")
class BeforePrimaryDeclarationSpec {

    @Nested inner class
    `locate a top level Java type` {

        @Test
        fun `a class`() {
            val location = BeforePrimaryDeclaration.locateOccurrence(classSource)
            location.wholeLine shouldBe 6
        }

        @Test
        fun `an interface`() {
            val location = BeforePrimaryDeclaration.locateOccurrence(interfaceSource)
            location.wholeLine shouldBe 1
        }

        @Test
        fun `an enum`() {
            val location = BeforePrimaryDeclaration.locateOccurrence(enumSource)
            location.wholeLine shouldBe 2
        }
    }

    /**
     * Prepares test environment for the integration tests that use
     * [BeforePrimaryDeclaration] for adding the [Generated] annotation.
     */
    companion object {

        lateinit var classSrc: Path
        lateinit var interfaceSrc: Path
        lateinit var enumSrc: Path

        @JvmStatic
        @BeforeAll
        fun runPipeline(@TempDir input: Path, @TempDir output: Path) {
            val inputClassSrc = input / "TopLevelClass.java"
            inputClassSrc.run {
                createFile()
                writeText(classSource)
            }
            val inputInterfaceSrc = input / "TopLevelInterface.java"
            inputInterfaceSrc.run {
                createFile()
                writeText(interfaceSource)
            }
            val inputEnumSrc = input / "TopLevelEnum.java"
            inputEnumSrc.run {
                createFile()
                writeText(enumSource)
            }

            Pipeline(
                params = PipelineParameters.getDefaultInstance(),
                plugin = GeneratedTypeAnnotation().toPlugin(),
                sources = SourceFileSet.create(input, output)
            )()

            classSrc = output / inputClassSrc.name
            interfaceSrc = output / inputInterfaceSrc.name
            enumSrc = output / inputEnumSrc.name
        }
    }

    /**
     * Integration tests for using `BeforePrimaryDeclaration` in a pipeline which
     * annotates top level types with the [Generated] annotation.
     * See the companion object for the pipeline definition.
     */
    @Nested inner class
    `handle insertion of text before`{

        private lateinit var generatedCode: List<String>

        /**
         * The prefix expected in the inserted line.
         */
        private val expectedPrefix = "@${Generated::class.java.canonicalName}"

        /**
         * The line before the top level declaration.
         */
        private fun lineBefore(declPrefix: String): String {
            val declarationLine = generatedCode.indexOfFirst { it.startsWith(declPrefix) }
            check(declarationLine != -1) {
                "Top level declaration `$declPrefix` was not found."
            }
            return generatedCode[declarationLine - 1]
        }

        @Test
        fun `a class`() {
            generatedCode = classSrc.readLines()
            lineBefore("public class") shouldStartWith expectedPrefix
        }

        @Test
        fun `an interface`() {
            generatedCode = interfaceSrc.readLines()
            lineBefore("public interface") shouldStartWith expectedPrefix
        }

        @Test
        fun `an enum`() {
            generatedCode = enumSrc.readLines()
            lineBefore("public enum") shouldStartWith expectedPrefix
        }
    }
}

private const val PACKAGE_NAME = "given.java.file"

private val classSource = """
    /* File header comment. */    
    package $PACKAGE_NAME;

    /** 
     * Top level class Javadoc. 
     */
    public class TopLevelClass {

        /** Nested class Javadoc. */
        private static class Nested {
        }
    }
    """.ti()

private val interfaceSource = """
    /** Top level interface Javadoc. */
    public interface TopLevelInterface {
    }
    """.ti()

private val enumSource = """
    // File header comment.    
    package $PACKAGE_NAME;
    public enum TopLevelEnum { ONE, TWO }
    """.ti()
