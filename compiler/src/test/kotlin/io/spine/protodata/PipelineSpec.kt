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
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.codeGeneratorRequest
import io.spine.protodata.config.Configuration
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.renderer.codeLine
import io.spine.protodata.test.CatOutOfTheBoxEmancipator
import io.spine.protodata.test.DeletedTypeRepository
import io.spine.protodata.test.DeletedTypeView
import io.spine.protodata.test.DeletingRenderer
import io.spine.protodata.test.DocilePlugin
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.test.ECHO_FILE
import io.spine.protodata.test.GenericInsertionPoint.FILE_END
import io.spine.protodata.test.GenericInsertionPoint.FILE_START
import io.spine.protodata.test.GenericInsertionPoint.OUTSIDE_FILE
import io.spine.protodata.test.GreedyPolicy
import io.spine.protodata.test.InternalAccessRenderer
import io.spine.protodata.test.JavaGenericInsertionPointPrinter
import io.spine.protodata.test.Journey
import io.spine.protodata.test.JsRenderer
import io.spine.protodata.test.KtRenderer
import io.spine.protodata.test.NoOpRenderer
import io.spine.protodata.test.PlainStringRenderer
import io.spine.protodata.test.PrependingRenderer
import io.spine.protodata.test.TestPlugin
import io.spine.protodata.test.TestRenderer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@DisplayName("`Pipeline` should")
class PipelineSpec {

    private lateinit var srcRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var sourceFile: Path
    private lateinit var request: CodeGeneratorRequest
    private lateinit var renderer: TestRenderer

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        sourceFile = write("SourceCode.java", """
            ${Journey::class.simpleName} worth taking
        """.trimIndent())

        val descriptor = DoctorProto.getDescriptor()
        request = codeGeneratorRequest {
            protoFile += descriptor.toProto()
            fileToGenerate += descriptor.name
        }
        codegenRequestFile.writeBytes(request.toByteArray())
        renderer = TestRenderer()
    }

    @CanIgnoreReturnValue
    private fun write(path: String, code: String): Path {
        val file = srcRoot.resolve(path)
        file.parent.toFile().mkdirs()
        file.writeText(code)
        return file
    }

    @Test
    fun `render enhanced code`() {
        Pipeline(
            listOf(TestPlugin()),
            listOf(renderer),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()
        assertThat(sourceFile.readText())
            .isEqualTo("_Journey worth taking")
    }

    @Test
    fun `generate new files`() {
        Pipeline(
            listOf(TestPlugin()),
            listOf(InternalAccessRenderer()),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()
        val newClass = srcRoot.resolve("spine/protodata/test/JourneyInternal.java")
        assertThat(newClass.exists())
            .isTrue()
        assertThat(newClass.readText())
            .contains("class JourneyInternal")
    }

    @Test
    fun `delete files`() {
        val sourceFile = write("io/spine/protodata/test/DeleteMe_.java", "foo bar")
        Pipeline(
            listOf(TestPlugin()),
            listOf(DeletingRenderer()),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()
        assertThat(sourceFile.exists())
            .isFalse()
    }

    @Test
    fun `write into insertion points`() {
        val initialContent = "foo bar"
        val sourceFile = write("io/spine/protodata/test/DeleteMe_.java", initialContent)
        val renderer = PrependingRenderer()
        Pipeline(
            listOf(TestPlugin()),
            listOf(JavaGenericInsertionPointPrinter(), renderer),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()

        assertThat(sourceFile.readText()).run {
            contains("/* INSERT:'file_start' */")
            contains("Hello from ${renderer.javaClass.name}")
            contains("/* INSERT:'file_middle' */")
            contains(initialContent)
            contains("/* INSERT:'file_end' */")
        }
    }

    @Test
    fun `use different renderers for different files`() {
        val jsSource = write("test/source.js", "alert('Hello')")
        val ktSource = write("corp/acme/test/Source.kt", "println(\"Hello\")")
        Pipeline(
            listOf(TestPlugin()),
            listOf(JsRenderer(), KtRenderer()),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()
        assertThat(jsSource.readText()).contains("Hello JavaScript")
        assertThat(ktSource.readText()).contains("Hello Kotlin")
    }

    @Test
    fun `add insertion points`() {
        Pipeline(
            listOf(TestPlugin()),
            listOf(JavaGenericInsertionPointPrinter(), CatOutOfTheBoxEmancipator()),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()
        assertThat(sourceFile.readText()).run {
            startsWith("/* ${FILE_START.codeLine} */")
            endsWith("/* ${FILE_END.codeLine} */")
            doesNotContain(OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `not add insertion points if nobody touches the file contents`() {
        Pipeline(
            listOf(TestPlugin()),
            listOf(JavaGenericInsertionPointPrinter(), JsRenderer()),
            listOf(SourceFileSet.from(srcRoot)),
            request
        )()
        assertThat(sourceFile.readText()).run {
            doesNotContain(FILE_START.codeLine)
            doesNotContain(FILE_END.codeLine)
            doesNotContain(OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `write code into different destination`() {
        val destination = tempDir()
        Pipeline(
            listOf(TestPlugin()),
            listOf(InternalAccessRenderer()),
            listOf(SourceFileSet.from(srcRoot, destination)),
            request
        )()

        val path = "spine/protodata/test/JourneyInternal.java"
        val newClass = destination.resolve(path)
        assertThat(newClass.exists())
            .isTrue()
        assertThat(newClass.readText())
            .contains("class JourneyInternal")
        val newClassInSourceRoot = srcRoot.resolve(path)
        assertThat(newClassInSourceRoot.exists())
            .isFalse()
    }

    @Test
    fun `copy all sources into the new destination`() {
        val destination = tempDir()
        Pipeline(
            listOf(TestPlugin()),
            listOf(NoOpRenderer()),
            listOf(SourceFileSet.from(srcRoot, destination)),
            request
        )()

        assertThat(sourceFile.exists())
            .isTrue()
        assertThat(destination.resolve(sourceFile.fileName).exists())
            .isTrue()
    }

    /**
     * Creates a new unique temp directory.
     *
     * JUnit reuses @TempDir from @BeforeEach, and we need a fresh temp directory.
     * See [the JUnit issue](https://github.com/junit-team/junit5/issues/1967).
     */
    private fun tempDir() = Files.createTempDirectory("destination")

    @Nested
    inner class `When given multiple source file sets` {

        @Test
        fun `preserve source set when copying files`() {
            val destination1 = tempDir()
            val destination2 = tempDir()
            val source2 = tempDir()
            val secondSourceFile = source2 / "second.txt"
            secondSourceFile.createFile().writeText("foo bar")

            Pipeline(
                listOf(TestPlugin()),
                listOf(NoOpRenderer()),
                listOf(
                    SourceFileSet.from(srcRoot, destination1),
                    SourceFileSet.from(source2, destination2)
                ),
                request
            )()

            assertThat(sourceFile.exists())
                .isTrue()
            assertThat(destination1.resolve(sourceFile.fileName).exists())
                .isTrue()
            assertThat(destination2.resolve(secondSourceFile.fileName).exists())
                .isTrue()
            assertThat(destination2.resolve(secondSourceFile.fileName).readText())
                .isEqualTo(secondSourceFile.readText())

            assertThat(destination1.resolve(secondSourceFile.fileName).exists())
                .isFalse()
            assertThat(destination2.resolve(sourceFile.fileName).exists())
                .isFalse()
        }

        @Test
        fun `generate new files by relative path`() {
            val destination1 = tempDir()
            val destination2 = tempDir()
            val source2 = tempDir()

            val expectedContent = "123456789"
            Pipeline(
                listOf(TestPlugin()),
                listOf(PlainStringRenderer()),
                listOf(
                    SourceFileSet.from(srcRoot, destination1),
                    SourceFileSet.from(source2, destination2)
                ),
                request,
                Configuration.rawValue(expectedContent, PLAIN)
            )()

            val firstFile = destination1.resolve(ECHO_FILE)
            val secondFile = destination2.resolve(ECHO_FILE)
            assertThat(firstFile.exists())
                .isTrue()
            assertThat(secondFile.exists())
                .isTrue()
            assertThat(firstFile.readText())
                .isEqualTo(expectedContent)
            assertThat(secondFile.readText())
                .isEqualTo(expectedContent)
        }
        @Test
        fun `change files using insertion points`() {
            val destination1 = tempDir()
            val destination2 = tempDir()
            val source2 = tempDir()

            val expectedContent = "0987654321"
            val existingFilePath = "io/spine/protodata/test/OnlyInFirstDir_.java"
            write(existingFilePath, expectedContent)

            Pipeline(
                listOf(TestPlugin()),
                listOf(
                    JavaGenericInsertionPointPrinter(),
                    PrependingRenderer()
                ),
                listOf(
                    SourceFileSet.from(srcRoot, destination1),
                    SourceFileSet.from(source2, destination2)
                ),
                request
            )()

            destination2.toFile().walkTopDown().forEach { println(it.name) }
            println(destination2.resolve(existingFilePath))
            assertThat(destination2.resolve(existingFilePath).exists())
                .isFalse()
            val writtenFile = destination1.resolve(existingFilePath)
            assertThat(writtenFile.exists())
                .isTrue()
            assertThat(writtenFile.readText())
                .contains(expectedContent)
        }
    }

    @Nested
    inner class `Fail to construct if` {

        @Test
        fun `a policy handles too many events at once`() {
            val policy = GreedyPolicy()
            val pipeline = Pipeline(
                listOf(DocilePlugin(policies = setOf(policy))),
                listOf(renderer),
                listOf(SourceFileSet.from(srcRoot)),
                request
            )
            val error = assertThrows<IllegalStateException> { pipeline() }
            assertThat(error)
                .hasMessageThat()
                .contains(policy.javaClass.name)
        }

        @Test
        fun `view is already registered`() {
            val viewClass = DeletedTypeView::class.java
            val pipeline = Pipeline(
                listOf(DocilePlugin(
                    views = setOf(viewClass),
                    viewRepositories = setOf(DeletedTypeRepository())
                )),
                listOf(renderer),
                listOf(SourceFileSet.from(srcRoot)),
                request
            )
            val error = assertThrows<ConfigurationError> { pipeline() }
            assertThat(error)
                .hasMessageThat()
                .contains(viewClass.name)
        }
    }
}
