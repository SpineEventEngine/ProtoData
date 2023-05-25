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

package io.spine.protodata.renderer

import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.util.*
import java.util.Optional.empty
import kotlin.io.path.Path
import kotlin.io.path.div
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`SourceFileSet` should")
class SourceFileSetSpec {

    private lateinit var set: SourceFileSet
    private lateinit var existingSourceFile: Path
    private lateinit var existingSourceFileAbsolute: Path

    @BeforeEach
    fun createSet(@TempDir tempDir: Path) {
        existingSourceFile = Path("pkg/example/foo.txt")
        existingSourceFileAbsolute = tempDir / existingSourceFile
        existingSourceFileAbsolute.parent.toFile().mkdirs()
        existingSourceFileAbsolute.toFile().let { textFile ->
            textFile.createNewFile()
            textFile.writeText("this is a non-empty file")
        }
        set = SourceFileSet.from(tempDir)
    }

    @Test
    fun `find existing file by relative path`() {
        val found = set.findFile(existingSourceFile)
        found shouldBePresent {
            it.relativePath shouldBe existingSourceFile
        }
    }

    @Test
    fun `find existing file by absolute path`() {
        val found = set.findFile(existingSourceFileAbsolute)
        found shouldBePresent {
            it.relativePath shouldBe existingSourceFile
        }
    }

    @Test
    fun `not find a non-existing file`() {
        val found = set.findFile(Path("non/existing/file.txt"))
        found shouldBe empty()
    }

    @Test
    fun `fail when a non-existing file must be present`() {
        assertThrows(IllegalArgumentException::class.java) {
            set.file(Path("i/don/t/exis.txt"))
        }
    }
}
