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

package io.spine.protodata.backend

import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.protodata.CodegenContext
import io.spine.protodata.ConfigurationError
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.renderer.codeLine
import io.spine.protodata.settings.Format
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.settings.defaultConsumerId
import io.spine.protodata.test.AnnotationInsertionPointPrinter
import io.spine.protodata.test.CatOutOfTheBoxEmancipator
import io.spine.protodata.test.DeletedTypeRepository
import io.spine.protodata.test.DeletedTypeView
import io.spine.protodata.test.DeletingRenderer
import io.spine.protodata.test.DocilePlugin
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.test.ECHO_FILE
import io.spine.protodata.test.GenericInsertionPoint
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
import io.spine.tools.code.AnyLanguage
import io.spine.tools.code.Java
import io.spine.tools.code.JavaScript
import io.spine.tools.code.Kotlin
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
import org.junit.jupiter.api.assertDoesNotThrow
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
    private lateinit var overwritingSourceSet: SourceFileSet<*>

    @BeforeEach
    fun prepareSources(@TempDir sandbox: Path) {
        srcRoot = sandbox.resolve("src")
        srcRoot.toFile().mkdirs()
        targetRoot = sandbox.resolve("target")
        targetRoot.toFile().mkdirs()
        codegenRequestFile = sandbox.resolve("code-gen-request.bin")

        // The correctness of the Java source code is of no importance for this test suite.
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

        overwritingSourceSet = SourceFileSet.create(Java, srcRoot, targetRoot)
        targetFile = targetRoot.resolve(sourceFileName)
    }

    @CanIgnoreReturnValue
    private fun write(path: String, code: String) {
        val file = srcRoot.resolve(path)
        file.parent.toFile().mkdirs()
        file.writeText(code)
    }

    @Test
    fun `render enhanced code`(@TempDir settingsDir: Path) {
        Pipeline(
            plugins = listOf(TestPlugin(), renderer),
            sources = listOf(overwritingSourceSet),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        assertTextIn(targetFile).isEqualTo("_Journey worth taking")
    }

    @Test
    fun `generate new files`(@TempDir settingsDir: Path) {
        Pipeline(
            plugins = listOf(TestPlugin(), InternalAccessRenderer()),
            sources = listOf(overwritingSourceSet),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        val newClass = targetRoot.resolve("spine/protodata/test/JourneyInternal.java")
        assertExists(newClass)
        assertTextIn(newClass).contains("class JourneyInternal")
    }

    @Test
    fun `delete files`(@TempDir settingsDir: Path) {
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, "foo bar")
        Pipeline(
            plugins = listOf(TestPlugin(), DeletingRenderer()),
            sources = listOf(SourceFileSet.create(Java, srcRoot, targetRoot)),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        assertDoesNotExist(targetRoot / path)
    }

    @Test
    fun `write into insertion points`(@TempDir settingsDir: Path) {
        val initialContent = "foo bar"
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, initialContent)
        val renderer = PrependingRenderer()
        Pipeline(
            plugin = TestPlugin(
                JavaGenericInsertionPointPrinter(),
                renderer
            ),
            sources = SourceFileSet.create(Java, srcRoot, targetRoot),
            request,
            settings = SettingsDirectory(settingsDir)
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
    fun `not write into non-existing insertion points`(@TempDir settingsDir: Path) {
        val initialContent = "foo bar"
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, initialContent)
        val renderer = PrependingRenderer(NonExistingPoint)
        Pipeline(
            plugin = TestPlugin(renderer),
            sources = SourceFileSet.create(Java, srcRoot, targetRoot),
            request,
            settings = SettingsDirectory(settingsDir)
        )()
        textIn(targetRoot / path) shouldBe textIn(srcRoot / path)
    }

    @Test
    fun `write into inline insertion points`(@TempDir settingsDir: Path) {
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
                Java,
                renderers = listOf(
                    AnnotationInsertionPointPrinter(),
                    NullableAnnotationRenderer()
                )
            )),
            sources = listOf(SourceFileSet.create(Java, srcRoot, targetRoot)),
            request = CodeGeneratorRequest.getDefaultInstance(),
            settings = SettingsDirectory(settingsDir)
        )()
        assertTextIn(targetRoot / path)
            .contains("@Nullable String")
    }

    @Test
    fun `not write into non-existing inline insertion points`(@TempDir settingsDir: Path) {
        val initialContent = "foo bar"
        val path = "io/spine/protodata/test/DeleteMe_.java"
        write(path, initialContent)
        val renderer = PrependingRenderer(NonExistingPoint, inline = true)
        Pipeline(
            plugin = TestPlugin(renderer),
            sources = SourceFileSet.create(Java, srcRoot, targetRoot),
            request,
            settings = SettingsDirectory(settingsDir)
        )()
        textIn(targetRoot / path) shouldBe textIn(srcRoot / path)
    }

    @Test
    fun `use different renderers for different files`(@TempDir settingsDir: Path) {
        val jsPath = "test/source.js"
        val ktPath = "corp/acme/test/Source.kt"
        write(jsPath, "alert('Hello')")
        write(ktPath, "println(\"Hello\")")
        Pipeline(
            plugins = listOf(
                ImplicitPluginWithRenderers(JavaScript, JsRenderer()),
                ImplicitPluginWithRenderers(Kotlin, KtRenderer()),
            ),
            sources = listOf(
                SourceFileSet.create(JavaScript, srcRoot, targetRoot),
                SourceFileSet.create(Kotlin, srcRoot, targetRoot)
            ),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        assertTextIn(targetRoot / jsPath).contains("Hello JavaScript")
        assertTextIn(targetRoot / ktPath).contains("Hello Kotlin")
    }

    @Test
    fun `add insertion points`(@TempDir settingsDir: Path) {
        Pipeline(
            plugins = listOf(
                ImplicitPluginWithRenderers(Java, JavaGenericInsertionPointPrinter()),
                ImplicitPluginWithRenderers(AnyLanguage, CatOutOfTheBoxEmancipator()),
            ),
            sources = listOf(overwritingSourceSet),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        assertTextIn(targetFile).run {
            startsWith("/* ${GenericInsertionPoint.FILE_START.codeLine} */")
            endsWith("/* ${GenericInsertionPoint.FILE_END.codeLine} */")
            doesNotContain(GenericInsertionPoint.OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `not add insertion points if nobody touches the file contents`(@TempDir settingsDir: Path) {
        Pipeline(
            plugins = listOf(
                ImplicitPluginWithRenderers(Java, JavaGenericInsertionPointPrinter()),
                ImplicitPluginWithRenderers(JavaScript, JsRenderer())
            ),
            sources = listOf(overwritingSourceSet),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        assertTextIn(targetFile).run {
            doesNotContain(GenericInsertionPoint.FILE_START.codeLine)
            doesNotContain(GenericInsertionPoint.FILE_END.codeLine)
            doesNotContain(GenericInsertionPoint.OUTSIDE_FILE.codeLine)
        }
    }

    @Test
    fun `write code into different destination`(
        @TempDir settingsDir: Path,
        @TempDir destination: Path) {
        Pipeline(
            plugins = listOf(TestPlugin(), InternalAccessRenderer()),
            sources = listOf(SourceFileSet.create(Java, srcRoot, destination)),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()

        val path = "spine/protodata/test/JourneyInternal.java"
        val newClass = destination.resolve(path)

        assertExists(newClass)
        assertTextIn(newClass).contains("class JourneyInternal")

        val newClassInSourceRoot = srcRoot.resolve(path)
        assertDoesNotExist(newClassInSourceRoot)
    }

    @Test
    fun `copy all sources into the new destination`(@TempDir settingsDir: Path) {
        Pipeline(
            plugins = listOf(TestPlugin(), NoOpRenderer()),
            sources = listOf(SourceFileSet.create(AnyLanguage, srcRoot, targetRoot)),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )()
        assertExists(targetFile)
    }

    @Nested
    inner class `When given multiple source file sets` {

        @Test
        fun `preserve source set when copying files`(
            @TempDir settingsDir: Path,
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
                sources = listOf(
                    SourceFileSet.create(AnyLanguage, srcRoot, destination1),
                    SourceFileSet.create(AnyLanguage, source2, destination2)
                ),
                request = request,
                settings = SettingsDirectory(settingsDir)
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
            @TempDir settingsDir: Path,
            @TempDir source2: Path,
            @TempDir destination1: Path,
            @TempDir destination2: Path
        ) {
            checkTemps(source2, destination1, destination2)
            val expectedContent = "123456789"
            val settings = SettingsDirectory(settingsDir)
            settings.write(PlainStringRenderer::class.java.defaultConsumerId,
                Format.PLAIN,
                expectedContent
            )
            Pipeline(
                plugins = listOf(
                    TestPlugin(),
                    PlainStringRenderer()
                ),
                sources = listOf(
                    SourceFileSet.create(AnyLanguage, srcRoot, destination1),
                    SourceFileSet.create(AnyLanguage, source2, destination2)
                ),
                request = request,
                settings = settings
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
            @TempDir settingsDir: Path,
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
                    ImplicitPluginWithRenderers(Java, listOf(
                            JavaGenericInsertionPointPrinter(),
                            PrependingRenderer()
                    ))
                ),
                sources = listOf(
                    SourceFileSet.create(Java, srcRoot, destination1),
                    SourceFileSet.create(Java, source2, destination2)
                ),
                request = request,
                settings = SettingsDirectory(settingsDir)
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
        fun `view is already registered`(@TempDir settingsDir: Path) {
            val viewClass = DeletedTypeView::class.java
            val pipeline = Pipeline(
                plugins = listOf(
                    DocilePlugin(
                        language = AnyLanguage,
                        views = setOf(viewClass),
                        viewRepositories = setOf(DeletedTypeRepository())
                    ),
                    renderer
                ),
                sources = listOf(overwritingSourceSet),
                request = request,
                settings = SettingsDirectory(settingsDir)
            )
            val error = assertThrows<ConfigurationError> { pipeline() }
            error.message shouldContain(viewClass.name)
        }
    }

    @Test
    fun `expose 'codegenContext' property for testing purposes`(@TempDir settingsDir: Path) {
        val pipeline = Pipeline(
            plugins = listOf(TestPlugin(), renderer),
            sources = listOf(overwritingSourceSet),
            request = request,
            settings = SettingsDirectory(settingsDir)
        )

        // Ensure that the lazily evaluated property is created successfully.
        val codegenContext: CodegenContext = assertDoesNotThrow {
            pipeline.codegenContext
        }

        // We do not expose the type behind the `codegenContext` property for additional
        // safety of the usage. Knowing of the underlying type is used by the testing utility
        // `io.spine.protodata.testing.PipelineSetup.createPipelineAndBlackbox()`.
        // Please see the `testlib` module for details.
        (codegenContext is CodeGenerationContext) shouldBe true
    }
}

private fun assertTextIn(file: Path): StringSubject =
    assertThat(file.readText())

private fun textIn(file: Path) = file.readText()

/**
 * Ensures that paths generated by `@TempDir` parameter annotation are
 * different, as it used to be prior to JUnit 5.8.
 *
 * @see <a href="https://github.com/junit-team/junit5/issues/1967">Resolved JUnit issue</a>
 */
private fun checkTemps(vararg path: Path) {
    path.toList().zipWithNext().forEach {
        assertThat(it.first).isNotEqualTo(it.second)
    }
}
