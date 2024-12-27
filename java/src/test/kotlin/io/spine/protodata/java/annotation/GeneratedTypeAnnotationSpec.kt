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

import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import io.spine.base.Time
import io.spine.protodata.Constants.CLI_APP_CLASS
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.java.JAVA_FILE
import io.spine.protodata.java.WithSourceFileSet
import io.spine.protodata.java.annotation.GeneratedTypeAnnotation.Companion.currentDateTime
import io.spine.protodata.render.SourceFile
import io.spine.protodata.settings.SettingsDirectory
import io.spine.time.testing.FrozenMadHatterParty
import io.spine.time.toTimestamp
import java.nio.file.Path
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.Path
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`GeneratedTypeAnnotation` renderer should")
internal class GeneratedTypeAnnotationSpec : WithSourceFileSet() {

    companion object {

        lateinit var settings: SettingsDirectory

        @BeforeAll
        @JvmStatic
        fun createSettings(@TempDir dir: Path) {
            settings = SettingsDirectory(dir)
        }
    }

    @Test
    fun `add the annotation, assuming 'PROTODATA_CLI' as the default generator`() {
        createPipelineWith(GeneratedTypeAnnotation())
        assertGenerated(
            "@javax.annotation.processing.Generated(\"$CLI_APP_CLASS\")"
        )
    }

    @Test
    fun `use given generator value`() {
        createPipelineWith(GeneratedTypeAnnotation(javaClass.name))
        assertGenerated(
            "@javax.annotation.processing.Generated(\"${javaClass.name}\")"
        )
    }

    @Nested
    inner class TimestampTests {

        private var frozenTime: ZonedDateTime? = null

        @BeforeEach
        fun freezeTime() {
            // Have time shifted, event when testing at UTC.
            frozenTime = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"))
            val timeProvider = FrozenPartyAtTimezone(frozenTime!!)
            Time.setProvider(timeProvider)
        }

        @AfterEach
        fun unfreezeTime() {
            Time.resetProvider()
            frozenTime = null
        }

        @Test
        fun `produce timestamp of code generation`() {
            createPipelineWith(GeneratedTypeAnnotation(
                generator = javaClass.name,
                addTimestamp = true
            ))

            val expectedDate = currentDateTime()

            val expectedCode = """
                 @javax.annotation.processing.Generated(
                     value = "${javaClass.name}",
                     date = "$expectedDate"
                 )
                 """.trimIndent().replace("\n", System.lineSeparator())

            val assertCode = assertCode()
            assertCode.contains(expectedCode)
            assertCode.contains("+03:00\"") // Istanbul zone offset
        }
    }

    @Test
    fun `adds comments for the given file`() {
        val addFileName : (SourceFile<*>) -> String = {
            "file://${it.relativePath}"
        }
        createPipelineWith(GeneratedTypeAnnotation(
            generator = javaClass.name,
            commenter = addFileName
        ))
        val assertCode = assertCode()
        assertCode.contains(
            "    comments = \"file://"
        )
    }

    private fun assertGenerated(expectedCode: String) {
        val assertThat = assertCode()
        assertThat.contains(expectedCode)
    }

    private fun assertCode(): StringSubject {
        val code = generatedCode()
        return assertThat(code)
    }

    private fun generatedCode() = sources.first()
        .file(Path(JAVA_FILE))
        .code()

    private fun createPipelineWith(generatedTypeAnnotation: GeneratedTypeAnnotation) {
        Pipeline(
            params = io.spine.protodata.params.PipelineParameters.getDefaultInstance(),
            plugins = listOf(generatedTypeAnnotation.toPlugin()),
            sources = this.sources,
        )()
    }
}

private class FrozenPartyAtTimezone(private val dateTime: ZonedDateTime) :
    FrozenMadHatterParty(dateTime.toInstant().toTimestamp()) {

    override fun currentZone(): ZoneId {
        return dateTime.zone
    }
}
