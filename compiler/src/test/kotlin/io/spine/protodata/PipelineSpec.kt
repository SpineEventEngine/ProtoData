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

import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.codeGeneratorRequest
import io.spine.protodata.config.Configuration
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.renderer.codeLine
import io.spine.protodata.test.AnnotationInsertionPointPrinter
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
import io.spine.protodata.test.NullableAnnotationRenderer
import io.spine.protodata.test.PlainStringRenderer
import io.spine.protodata.test.PrependingRenderer
import io.spine.protodata.test.TestPlugin
import io.spine.protodata.test.UnderscorePrefixRenderer
import io.spine.testing.assertDoesNotExist
import io.spine.testing.assertExists
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

@Suppress("TooManyFunctions") // This class has many test cases.
@DisplayName("`Pipeline` should")
class PipelineSpec {

    private lateinit var srcRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var sourceFile: Path
    private lateinit var request: CodeGeneratorRequest
    private lateinit var renderer: UnderscorePrefixRenderer
    private lateinit var overwritingSourceSet: SourceFileSet

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        // Correctness of the Java source code is of no importance for this test suite.
        sourceFile = write("SourceCode.java", """
            ${Journey::class.simpleName} worth taking
        """.trimIndent())

        val descriptor = DoctorProto.getDescriptor()
        request = codeGeneratorRequest {
            protoFile += descriptor.toProto()
            fileToGenerate += descriptor.name
        }
        codegenRequestFile.writeBytes(request.toByteArray())
        renderer = UnderscorePrefixRenderer()

        overwritingSourceSet = SourceFileSet.from(srcRoot)
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
            plugin = TestPlugin(),
            renderer = renderer,
            sources = overwritingSourceSet,
            request
        )()
        assertTextIn(sourceFile).isEqualTo("_Journey worth taking")
    }

    @Test
    fun `generate new files`() {
        Pipeline(
            plugin = TestPlugin(),
            renderer = InternalAccessRenderer(),
            sources = overwritingSourceSet,
            request
        )()
        val newClass = srcRoot.resolve("spine/protodata/test/JourneyInternal.java")
        assertExists(newClass)
        assertTextIn(newClass).contains("class JourneyInternal")
    }

    @Test
    fun `delete files`() {
        val sourceFile = write("io/spine/protodata/test/DeleteMe_.java", "foo bar")
        Pipeline(
            plugin = TestPlugin(),
            renderer = DeletingRenderer(),
            sources = SourceFileSet.from(srcRoot),
            request
        )()
        assertDoesNotExist(sourceFile)
    }

    @Test
    fun `write into insertion points`() {
        val initialContent = "foo bar"
        val sourceFile = write("io/spine/protodata/test/DeleteMe_.java", initialContent)
        val renderer = PrependingRenderer()
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                JavaGenericInsertionPointPrinter(),
                renderer
            ),
            sources = SourceFileSet.from(srcRoot),
            request
        )()

        assertTextIn(sourceFile).run {
            contains("/* INSERT:'file_start' */")
            contains("Hello from ${renderer.javaClass.name}")
            contains("/* INSERT:'file_middle' */")
            contains(initialContent)
            contains("/* INSERT:'file_end' */")
        }
    }

    @Test
    fun `write into inline insertion points`() {
        val sourceFile = write("ClassWithMethod.java", """
            class ClassWithMethod {
                public java.lang.String foo() {
                    return "happy halloween";
                }
            }
        """.trimIndent())
        Pipeline(
            plugins = listOf(),
            renderers = listOf(AnnotationInsertionPointPrinter(), NullableAnnotationRenderer()),
            sources = listOf(SourceFileSet.from(srcRoot)),
            request = CodeGeneratorRequest.getDefaultInstance()
        )()
        assertTextIn(sourceFile)
            .contains("@Nullable String")
    }

    @Test
    fun `use different renderers for different files`() {
        val jsSource = write("test/source.js", "alert('Hello')")
        val ktSource = write("corp/acme/test/Source.kt", "println(\"Hello\")")
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                JsRenderer(),
                KtRenderer()
            ),
            sources = SourceFileSet.from(srcRoot),
            request
        )()
        assertTextIn(jsSource).contains("Hello JavaScript")
        assertTextIn(ktSource).contains("Hello Kotlin")
    }

    @Test
    fun `add insertion points`() {
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                JavaGenericInsertionPointPrinter(),
                CatOutOfTheBoxEmancipator()
            ),
            sources = overwritingSourceSet,
            request
        )()
        assertTextIn(sourceFile).run {
            startsWith("/* ${FILE_START.codeLine} */")
            endsWith("/* ${FILE_END.codeLine} */")
            doesNotContain(OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `not add insertion points if nobody touches the file contents`() {
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                JavaGenericInsertionPointPrinter(),
                JsRenderer()
            ),
            sources = overwritingSourceSet,
            request
        )()
        assertTextIn(sourceFile).run {
            doesNotContain(FILE_START.codeLine)
            doesNotContain(FILE_END.codeLine)
            doesNotContain(OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `write code into different destination`(@TempDir destination: Path) {
        Pipeline(
            plugin = TestPlugin(),
            renderer = InternalAccessRenderer(),
            sources = SourceFileSet.from(srcRoot, destination),
            request
        )()

        val path = "spine/protodata/test/JourneyInternal.java"
        val newClass = destination.resolve(path)

        assertExists(newClass)
        assertTextIn(newClass).contains("class JourneyInternal")

        val newClassInSourceRoot = srcRoot.resolve(path)
        assertDoesNotExist(newClassInSourceRoot)
    }

    @Test
    fun `copy all sources into the new destination`(@TempDir destination: Path) {
        Pipeline(
            TestPlugin(),
            NoOpRenderer(),
            SourceFileSet.from(srcRoot, destination),
            request
        )()

        assertExists(sourceFile)
        assertExists(destination.resolve(sourceFile.fileName))
    }

    @Nested
    inner class `When given multiple source file sets` {

        @Test
        fun `preserve source set when copying files`(
            @TempDir source2: Path,
            @TempDir destination1: Path,
            @TempDir destination2: Path
        ) {
            checkTemps(source2, destination1, destination2)

            val secondSourceFile = source2 / "second.txt"
            secondSourceFile.createFile().writeText("foo bar")

            Pipeline(
                TestPlugin(),
                NoOpRenderer(),
                listOf(
                    SourceFileSet.from(srcRoot, destination1),
                    SourceFileSet.from(source2, destination2)
                ),
                request
            )()

            assertExists(sourceFile)
            assertExists(destination1.resolve(sourceFile.fileName))
            assertExists(destination2.resolve(secondSourceFile.fileName))

            assertTextIn(destination2.resolve(secondSourceFile.fileName))
                .isEqualTo(secondSourceFile.readText())

            assertDoesNotExist(destination1.resolve(secondSourceFile.fileName))
            assertDoesNotExist(destination2.resolve(sourceFile.fileName))
        }

        @Test
        fun `generate new files by relative path`(
            @TempDir source2: Path,
            @TempDir destination1: Path,
            @TempDir destination2: Path
        ) {
            checkTemps(source2, destination1, destination2)

            val expectedContent = "123456789"
            Pipeline(
                plugin = TestPlugin(),
                renderer = PlainStringRenderer(),
                listOf(
                    SourceFileSet.from(srcRoot, destination1),
                    SourceFileSet.from(source2, destination2)
                ),
                request,
                Configuration.rawValue(expectedContent, PLAIN)
            )()

            val firstFile = destination1.resolve(ECHO_FILE)
            val secondFile = destination2.resolve(ECHO_FILE)
            assertExists(firstFile)
            assertExists(secondFile)

            assertTextIn(firstFile).isEqualTo(expectedContent)
            assertTextIn(secondFile).isEqualTo(expectedContent)
        }

        @Test
        fun `change files using insertion points`(
            @TempDir source2: Path,
            @TempDir destination1: Path,
            @TempDir destination2: Path
        ) {
            val expectedContent = "0987654321"
            val existingFilePath = "io/spine/protodata/test/OnlyInFirstDir_.java"
            write(existingFilePath, expectedContent)

            Pipeline(
                plugins = listOf(TestPlugin()),
                renderers = listOf(
                    JavaGenericInsertionPointPrinter(),
                    PrependingRenderer()
                ),
                sources = listOf(
                    SourceFileSet.from(srcRoot, destination1),
                    SourceFileSet.from(source2, destination2)
                ),
                request
            )()

            assertDoesNotExist(destination2.resolve(existingFilePath))

            val writtenFile = destination1.resolve(existingFilePath)
            assertExists(writtenFile)
            assertTextIn(writtenFile).contains(expectedContent)
        }
    }

    @Nested
    inner class `Fail to construct if` {

        @Test
        fun `a policy handles too many events at once`() {
            val policy = GreedyPolicy()
            val pipeline = Pipeline(
                plugin = DocilePlugin(policies = setOf(policy)),
                renderer = renderer,
                sources = overwritingSourceSet,
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
                plugin = DocilePlugin(
                    views = setOf(viewClass),
                    viewRepositories = setOf(DeletedTypeRepository())
                ),
                renderer = renderer,
                sources = overwritingSourceSet,
                request
            )
            val error = assertThrows<ConfigurationError> { pipeline() }
            assertThat(error)
                .hasMessageThat()
                .contains(viewClass.name)
        }
    }
}

private fun assertTextIn(file: Path): StringSubject =
    assertThat(file.readText())

/**
 * Ensures that paths generated by `@TempDir` parameter annotation are not
 * the same, as it used to be prior to JUnit 5.8.
 *
 * @see <a href="https://github.com/junit-team/junit5/issues/1967">Resolved JUnit issue</a>
 */
private fun checkTemps(vararg path: Path) {
    path.toList().zipWithNext().forEach {
        assertThat(it.first).isNotEqualTo(it.second)
    }
}
