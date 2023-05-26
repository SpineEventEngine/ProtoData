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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.renderer.codeLine
import io.spine.protodata.test.CatOutOfTheBoxEmancipator
import io.spine.protodata.test.IgnoreValueAnnotator
import io.spine.protodata.test.IgnoreValueAnnotator.Companion.ANNOTATION_TYPE
import io.spine.protodata.test.KotlinInsertionPoint.FILE_END
import io.spine.protodata.test.KotlinInsertionPoint.FILE_START
import io.spine.protodata.test.KotlinInsertionPoint.LINE_FOUR_COL_THIRTY_THREE
import io.spine.protodata.test.NonVoidMethod
import io.spine.protodata.test.NonVoidMethodPrinter
import io.spine.protodata.test.VariousKtInsertionPointsPrinter
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Insertion points should appear at")
class InsertionPointsSpec {

    private lateinit var kotlinFile: Path
    private lateinit var javaFile: Path

    @BeforeEach
    fun preparePipeline(@TempDir path: Path) {
        kotlinFile = path / "sources.kt"
        javaFile = path / "Source.java"
        kotlinFile.createFile()
        javaFile.createFile()
        kotlinFile.writeText("""
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
        javaFile.writeText("""
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
        Pipeline(
            plugins = listOf(),
            renderers = listOf(
                VariousKtInsertionPointsPrinter(), CatOutOfTheBoxEmancipator(),
                NonVoidMethodPrinter(), IgnoreValueAnnotator()
            ),
            sources = listOf(SourceFileSet.from(path)),
            request = PluginProtos.CodeGeneratorRequest.getDefaultInstance(),
        )()
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
        assertThat(contents)
            .isNotEmpty()
        assertThat(contents.last())
            .contains(FILE_END.label)
    }

    @Test
    fun `a specific line and column`() {
        val contents = kotlinFile.readLines()
        assertThat(contents)
            .isNotEmpty()
        assertThat(contents[3])
            .contains("I_AM_CONSTANT:  /* ${LINE_FOUR_COL_THIRTY_THREE.codeLine} */ String")
    }

    @Test
    fun `in multiple places in a line`() {
        val contents = javaFile.readText()
        contents shouldContain Regex("@$ANNOTATION_TYPE\\s+public int bar")
        contents shouldContain Regex("@$ANNOTATION_TYPE\\s+public final String baz")
    }
}
