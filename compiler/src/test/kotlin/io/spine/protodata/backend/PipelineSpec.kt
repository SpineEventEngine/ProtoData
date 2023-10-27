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

package io.spine.protodata.backend

import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.protodata.ConfigurationError
import io.spine.protodata.backend.Pipeline.Companion.generateId
import io.spine.protodata.config.Configuration
import io.spine.protodata.config.ConfigurationFormat
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
import io.spine.protodata.test.GenericInsertionPoint
import io.spine.protodata.test.GreedyPolicy
import io.spine.protodata.test.InternalAccessRenderer
import io.spine.protodata.test.JavaGenericInsertionPointPrinter
import io.spine.protodata.test.Journey
import io.spine.protodata.test.JsRenderer
import io.spine.protodata.test.KtRenderer
import io.spine.protodata.test.NoOpRenderer
import io.spine.protodata.test.NonExistingPoint
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
import kotlin.io.path.name
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
    private lateinit var targetRoot : Path
    private lateinit var codegenRequestFile: Path
    private lateinit var targetFile: Path
    private lateinit var request: CodeGeneratorRequest
    private lateinit var renderer: UnderscorePrefixRenderer
    private lateinit var overwritingSourceSet: SourceFileSet

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        targetRoot = sandbox.resolve("target")
        targetRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        // Correctness of the Java source code is of no importance for this test suite.
        val sourceFileName = "SourceCode.java"
        write(sourceFileName, """
            ${Journey::class.simpleName} worth taking
        """.trimIndent())

        val descriptor = DoctorProto.getDescriptor()
        request = codeGeneratorRequest {
            protoFile += descriptor.toProto()
            fileToGenerate += descriptor.name
        }
        codegenRequestFile.writeBytes(request.toByteArray())
        renderer = UnderscorePrefixRenderer()

        overwritingSourceSet = SourceFileSet.create(srcRoot, targetRoot)
        targetFile = targetRoot.resolve(sourceFileName)
    }

    @CanIgnoreReturnValue
    private fun write(path: String, code: String) {
        val file = srcRoot.resolve(path)
        file.parent.toFile().mkdirs()
        file.writeText(code)
    }

    @Test
    fun `render enhanced code`() {
        Pipeline(
            plugins = listOf(TestPlugin(), renderer),
            sources = listOf(overwritingSourceSet),
            request,
        )()
        assertTextIn(targetFile).isEqualTo("_Journey worth taking")
    }

    @Test
    fun `generate new files`() {
        Pipeline(
            plugins = listOf(TestPlugin(), InternalAccessRenderer()),
            sources = listOf(overwritingSourceSet),
            request,
        )()
        val newClass = targetRoot.resolve("spine/protodata/test/JourneyInternal.java")
        assertExists(newClass)
        assertTextIn(newClass).contains("class JourneyInternal")
    }

    @Test
    fun `delete files`() {
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, "foo bar")
        Pipeline(
            plugins = listOf(TestPlugin(), DeletingRenderer()),
            sources = listOf(SourceFileSet.create(srcRoot, targetRoot)),
            request
        )()
        assertDoesNotExist(targetRoot / path)
    }

    @Test
    fun `write into insertion points`() {
        val initialContent = "foo bar"
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, initialContent)
        val renderer = PrependingRenderer()
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                JavaGenericInsertionPointPrinter(),
                renderer
            ),
            sources = SourceFileSet.create(srcRoot, targetRoot),
            request = request,
            id = generateId()
        )()

        assertTextIn(targetRoot / path).run {
            contains("/* INSERT:'file_start' */")
            contains("Hello from ${renderer.javaClass.name}")
            contains("/* INSERT:'file_middle' */")
            contains(initialContent)
            contains("/* INSERT:'file_end' */")
        }
    }

    @Test
    fun `not write into non-existing insertion points`() {
        val initialContent = "foo bar"
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, initialContent)
        val renderer = PrependingRenderer(NonExistingPoint)
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                renderer
            ),
            sources = SourceFileSet.create(srcRoot, targetRoot),
            request = request,
            id = generateId()
        )()
        textIn(targetRoot / path) shouldBe textIn(srcRoot / path)
    }

    @Test
    fun `write into inline insertion points`() {
        val path = "ClassWithMethod.java"
        write(path, """
            class ClassWithMethod {
                public java.lang.String foo() {
                    return "happy halloween";
                }
            }
        """.trimIndent())
        Pipeline(
            plugins = listOf(ImplicitPluginWithRenderers(
                renderers = listOf(
                    AnnotationInsertionPointPrinter(),
                    NullableAnnotationRenderer()
                )
            )),
            sources = listOf(SourceFileSet.create(srcRoot, targetRoot)),
            request = CodeGeneratorRequest.getDefaultInstance(),
            id = generateId()
        )()
        assertTextIn(targetRoot / path)
            .contains("@Nullable String")
    }

    @Test
    fun `not write into non-existing inline insertion points`() {
        val initialContent = "foo bar"
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, initialContent)
        val renderer = PrependingRenderer(NonExistingPoint, inline = true)
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(renderer),
            sources = SourceFileSet.create(srcRoot, targetRoot),
            request
        )()
        textIn(targetRoot / path) shouldBe textIn(srcRoot / path)
    }

    @Test
    fun `use different renderers for different files`() {
        val jsPath = "test/source.js"
        val ktPath = "corp/acme/test/Source.kt"
        write(jsPath, "alert('Hello')")
        write(ktPath, "println(\"Hello\")")
        Pipeline(
            plugin = TestPlugin(),
            renderers = listOf(
                JsRenderer(),
                KtRenderer()
            ),
            sources = SourceFileSet.create(srcRoot, targetRoot),
            request
        )()
        assertTextIn(targetRoot / jsPath).contains("Hello JavaScript")
        assertTextIn(targetRoot / ktPath).contains("Hello Kotlin")
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
        assertTextIn(targetFile).run {
            startsWith("/* ${GenericInsertionPoint.FILE_START.codeLine} */")
            endsWith("/* ${GenericInsertionPoint.FILE_END.codeLine} */")
            doesNotContain(GenericInsertionPoint.OUTSIDE_FILE.codeLine)
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
        assertTextIn(targetFile).run {
            doesNotContain(GenericInsertionPoint.FILE_START.codeLine)
            doesNotContain(GenericInsertionPoint.FILE_END.codeLine)
            doesNotContain(GenericInsertionPoint.OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `write code into different destination`(@TempDir destination: Path) {
        Pipeline(
            plugins = listOf(TestPlugin(), InternalAccessRenderer()),
            sources = listOf(SourceFileSet.create(srcRoot, destination)),
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
    fun `copy all sources into the new destination`() {
        Pipeline(
            plugins = listOf(TestPlugin(), NoOpRenderer()),
            sources = listOf(SourceFileSet.create(srcRoot, targetRoot)),
            request
        )()
        assertExists(targetFile)
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
                plugins = listOf(
                    TestPlugin(),
                    NoOpRenderer()
                ),
                listOf(
                    SourceFileSet.create(srcRoot, destination1),
                    SourceFileSet.create(source2, destination2)
                ),
                request
            )()

            assertExists(destination1.resolve(targetFile.name))
            assertExists(destination2.resolve(secondSourceFile.name))

            assertTextIn(destination2.resolve(secondSourceFile.name))
                .isEqualTo(secondSourceFile.readText())

            assertDoesNotExist(destination1.resolve(secondSourceFile.name))
            assertDoesNotExist(destination2.resolve(targetFile.name))
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
                plugins = listOf(
                    TestPlugin(),
                    PlainStringRenderer()
                ),
                sources = listOf(
                    SourceFileSet.create(srcRoot, destination1),
                    SourceFileSet.create(source2, destination2)
                ),
                request,
                Configuration.rawValue(expectedContent, ConfigurationFormat.PLAIN)
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
                plugins = listOf(
                    TestPlugin(),
                    ImplicitPluginWithRenderers(listOf(
                            JavaGenericInsertionPointPrinter(),
                            PrependingRenderer()
                    ))
                ),
                sources = listOf(
                    SourceFileSet.create(srcRoot, destination1),
                    SourceFileSet.create(source2, destination2)
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
                plugins = listOf(DocilePlugin(policies = setOf(policy)), renderer),
                sources = listOf(overwritingSourceSet),
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
                plugins = listOf(
                    DocilePlugin(
                        views = setOf(viewClass),
                        viewRepositories = setOf(DeletedTypeRepository())
                    ),
                    renderer
                ),
                sources = listOf(overwritingSourceSet),
                request
            )
            val error = assertThrows<ConfigurationError> { pipeline() }
            error.message shouldContain(viewClass.name)
        }
    }
}

private fun assertTextIn(file: Path): StringSubject =
    assertThat(file.readText())

private fun textIn(file: Path) = file.readText()

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
