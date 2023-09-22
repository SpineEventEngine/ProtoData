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

package io.spine.protodata

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.compiler.PluginProtos
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.string.shouldContain
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.renderer.SourceFileSetLabel
import io.spine.protodata.renderer.codeLine
import io.spine.protodata.test.CatOutOfTheBoxEmancipator
import io.spine.protodata.test.CompanionFramer
import io.spine.protodata.test.CompanionLalalaRenderer
import io.spine.protodata.test.CompanionLalalaRenderer.Companion.LALALA
import io.spine.protodata.test.IgnoreValueAnnotator
import io.spine.protodata.test.IgnoreValueAnnotator.Companion.ANNOTATION_TYPE
import io.spine.protodata.test.KotlinInsertionPoint.FILE_END
import io.spine.protodata.test.KotlinInsertionPoint.FILE_START
import io.spine.protodata.test.KotlinInsertionPoint.LINE_FOUR_COL_THIRTY_THREE
import io.spine.protodata.test.NonVoidMethodPrinter
import io.spine.protodata.test.VariousKtInsertionPointsPrinter
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import java.lang.System.lineSeparator
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Insertion points should appear at")
class InsertionPointsSpec {

    private lateinit var kotlinFile: Path
    private lateinit var javaFile: Path

    @BeforeEach
    fun preparePipeline(@TempDir input: Path, @TempDir output: Path) {
        val kt = "kt"
        val java = "jj"
        val inputKtFile = input / "$kt/sources.kt"
        val inputJavaFile = input / "$java/Source.java"
        inputKtFile.create("""
            class LabMouse {
                companion object {
                    const val I_AM_CONSTANT: String = "!!"
                }
               
                fun letsHaveFun(): String {
                    return \"trololo\"
                }
            }
            """.trimIndent()
        )
        inputJavaFile.create("""
            package com.example;
            
            public class Source {
            
                public void foo() {}
                
                public int bar() {
                    return 42;
                }
                
                public final String baz() {
                    return "123";
                }
            }
            """.trimIndent()
        )
        val javaSet = SourceFileSet.create(SourceFileSetLabel(Java), input / java, output / java)
        val ktSet = SourceFileSet.create(SourceFileSetLabel(Kotlin), input / kt, output / kt)
        Pipeline(
            plugins = listOf(),
            renderers = listOf(
                VariousKtInsertionPointsPrinter(), CatOutOfTheBoxEmancipator(),
                NonVoidMethodPrinter(), IgnoreValueAnnotator(),
                CompanionFramer(), CompanionLalalaRenderer()
            ),
            sources = listOf(javaSet, ktSet),
            request = PluginProtos.CodeGeneratorRequest.getDefaultInstance(),
        )()
        kotlinFile = output / kt / inputKtFile.name
        javaFile = output / java / inputJavaFile.name
    }

    @Test
    fun `the start of a file`() {
        val contents = kotlinFile.readLines()
        assertThat(contents)
            .isNotEmpty()
        assertThat(contents[0])
            .contains(FILE_START.label)
    }

    @Test
    fun `the end of a file`() {
        val contents = kotlinFile.readLines()
        contents shouldNotHaveSize 0
        contents.last() shouldContain FILE_END.label
    }

    @Test
    fun `a specific line and column`() {
        val contents = kotlinFile.readLines()
        contents shouldHaveAtLeastSize 4
        withClue(contents.joinToString(lineSeparator())) {
            contents[3] shouldContain
                    "I_AM_CONSTANT:  /* ${LINE_FOUR_COL_THIRTY_THREE.codeLine} */ String"
        }
    }

    @Test
    fun `in multiple places in a file`() {
        val contents = javaFile.readText()
        contents shouldContain Regex("@$ANNOTATION_TYPE\\s+public int bar")
        contents shouldContain Regex("@$ANNOTATION_TYPE\\s+public final String baz")
    }

    @Test
    fun `in multiple places in one line`() {
        val lines = kotlinFile.readLines()
        lines[2] shouldContain Regex("$LALALA\\s+companion.+$LALALA\\s+object", DOT_MATCHES_ALL)
    }
}

private fun Path.create(content: String) {
    parent.toFile().mkdirs()
    createFile()
    writeText(content)
}
