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

package io.spine.protodata.java.annotation

import io.kotest.matchers.string.shouldContain
import io.spine.protodata.ast.toDirectory
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.java.JAVA_FILE
import io.spine.protodata.java.WithSourceFileSet
import io.spine.protodata.params.PipelineParameters
import io.spine.protodata.params.WorkingDirectory
import io.spine.protodata.settings.defaultConsumerId
import io.spine.protodata.testing.parametersWithSettingsDir
import io.spine.protodata.util.Format.PROTO_JSON
import io.spine.string.ti
import java.nio.file.Path
import kotlin.io.path.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`SuppressRenderer` should")
internal class SuppressWarningsAnnotationSpec : WithSourceFileSet() {

    private fun loadCode() = sources.first()
        .file(Path(JAVA_FILE))
        .code()

    @Nested
    inner class `suppress ALL warnings ` {

        @Test
        fun `if no settings are passed`() {
            Pipeline(
                params = PipelineParameters.getDefaultInstance(),
                plugins = listOf(SuppressWarningsAnnotation.Plugin()),
                sources = this@SuppressWarningsAnnotationSpec.sources,
            )()
            val code = loadCode()
            assertContainsSuppressionAll(code)
        }

        @Test
        fun `if settings contain an empty list of suppressions`(@TempDir dir: Path) {
            val settings = WorkingDirectory(dir).settingsDirectory
            settings.write(SuppressWarningsAnnotation::class.java.defaultConsumerId,
                PROTO_JSON, """
                    {"warnings": {"value": []}} 
                """.ti()
            )
            val params = parametersWithSettingsDir(settings.path)

            Pipeline(
                params = params,
                plugins = listOf(SuppressWarningsAnnotation.Plugin()),
                sources = this@SuppressWarningsAnnotationSpec.sources,
            )()
            val code = loadCode()
            assertContainsSuppressionAll(code)
        }

        private fun assertContainsSuppressionAll(code: String) {
            code shouldContain "@SuppressWarnings({\"ALL\"})"
        }
    }

    @Test
    fun `suppress only selected warnings`(@TempDir dir: Path) {
        val settings = WorkingDirectory(dir).settingsDirectory
        val deprecation = "deprecation"
        val stringEqualsEmptyString = "StringEqualsEmptyString"
        settings.write(SuppressWarningsAnnotation::class.java.defaultConsumerId,
            PROTO_JSON, """
                {"warnings": {"value": ["$deprecation", "$stringEqualsEmptyString"]}} 
            """.ti()
        )
        val params = parametersWithSettingsDir(settings.path)

        Pipeline(
            params = params,
            plugins = listOf(SuppressWarningsAnnotation.Plugin()),
            sources = sources,
        )()
        val code = loadCode()

        code shouldContain """@SuppressWarnings({"deprecation", "StringEqualsEmptyString"})"""
    }
}
