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
import io.kotest.matchers.shouldNotBe
import io.spine.protodata.renderer.given.PlainTextConvention
import io.spine.protodata.type.GeneratedDeclaration
import io.spine.protodata.type.TypeNameConvention
import io.spine.protodata.typeName
import java.nio.file.Path
import java.util.*
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
    private lateinit var existingSourceFiles: List<Path>
    private lateinit var existingSourceFilesAbsolute: List<Path>

    @BeforeEach
    fun createSet(@TempDir tempDir: Path) {
        existingSourceFiles = listOf(
            Path("pkg/example/foo.txt"),
            Path("another/sample/bar.txt"),
            Path("src/main/app.kt")
        )
        existingSourceFilesAbsolute = existingSourceFiles.map { tempDir / it }
        existingSourceFilesAbsolute.forEach {
            it.parent.toFile().mkdirs()
            it.toFile().let { textFile ->
                textFile.createNewFile()
                textFile.writeText("this is a non-empty file")
            }
        }
        set = SourceFileSet.from(tempDir)
    }

    @Test
    fun `find existing file by relative path`() {
        val found = set.findFile(existingSourceFiles[0])
        found shouldBePresent {
            it.relativePath shouldBe existingSourceFiles[0]
        }
    }

    @Test
    fun `find existing file by absolute path`() {
        val found = set.findFile(existingSourceFilesAbsolute[0])
        found shouldBePresent {
            it.relativePath shouldBe existingSourceFiles[0]
        }
    }

    @Test
    fun `find existing file by naming convention`() {
        val found = set.fileFor(typeName {
            packageName = "pkg.example"
            simpleName = "Foo"
        }).namedUsing(PlainTextConvention)
        found shouldNotBe null
        found!!.relativePath shouldBe existingSourceFiles[0]
    }

    @Test
    fun `not find a non-existing file`() {
        val found = set.find(Path("non/existing/file.txt"))
        found shouldBe null
    }

    @Test
    fun `not find a non-existing by naming convention`() {
        val found = set.fileFor(typeName {
            packageName = "pkg.example.foobar"
            simpleName = "IamNot"
        }).namedUsing(PlainTextConvention)
        found shouldBe null
    }

    @Test
    fun `fail when a non-existing file is referenced`() {
        assertThrows(IllegalArgumentException::class.java) {
            set.file(Path("i/don/t/exis.txt"))
        }
    }

    @Test
    fun `iterate over actual files`() {
        set.size shouldBe 3

        // This should delete the second file from the set.
        set.file(existingSourceFiles[1]).delete()

        set.size shouldBe 2
        set.find(existingSourceFiles[1]) shouldBe null
    }
}
