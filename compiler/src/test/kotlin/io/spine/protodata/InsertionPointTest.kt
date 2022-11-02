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
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.renderer.codeLine
import io.spine.protodata.test.CatOutOfTheBoxEmancipator
import io.spine.protodata.test.KotlinInsertionPoint.FILE_END
import io.spine.protodata.test.KotlinInsertionPoint.FILE_START
import io.spine.protodata.test.KotlinInsertionPoint.LINE_FOUR_COL_THIRTY_THREE
import io.spine.protodata.test.VariousKtInsertionPointsPrinter
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class `Insertion points should` {

    companion object {
        private const val FILE_NAME = "sources.kt"
    }

    @Nested
    inner class `appear at` {

        private lateinit var pipeline: Pipeline
        private lateinit var path: Path

        @BeforeEach
        fun preparePipeline(@TempDir path: Path) {
            this.path = path
            val file = path / FILE_NAME
            file.createFile()
            file.writeText("""
             class LabMouse {
                 companion object {
                     const val I_AM_CONSTANT: String = "!!"
                 }
                
                 fun letsHaveFun(): String {
                     return "trololo"
                 }
            }
            """.trimIndent())
            pipeline = Pipeline(
                listOf(),
                listOf(VariousKtInsertionPointsPrinter(), CatOutOfTheBoxEmancipator()),
                listOf(SourceFileSet.from(path)),
                PluginProtos.CodeGeneratorRequest.getDefaultInstance(),
            )
        }

        @Test
        fun `the start of a file`() {
            pipeline()
            val file = path / FILE_NAME
            val contents = file.readLines()
            assertThat(contents)
                .isNotEmpty()
            assertThat(contents[0])
                .contains(FILE_START.label)
        }

        @Test
        fun `the end of a file`() {
            pipeline()
            val file = path / FILE_NAME
            val contents = file.readLines()
            assertThat(contents)
                .isNotEmpty()
            assertThat(contents.last())
                .contains(FILE_END.label)
        }

        @Test
        fun `a specific line and column`() {
            pipeline()
            val file = path / FILE_NAME
            val contents = file.readLines()
            assertThat(contents)
                .isNotEmpty()
            assertThat(contents[3])
                .contains("I_AM_CONSTANT:/* ${LINE_FOUR_COL_THIRTY_THREE.codeLine} */      String")
        }
    }
}
