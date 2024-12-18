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

package io.spine.protodata

import io.kotest.matchers.string.shouldContain
import io.spine.logging.testing.tapConsole
import java.io.File
import java.nio.file.Paths
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`Compilation` object should")
internal class CompilationSpec {

    @Test
    fun `throw the 'Error' exception under tests`() {
        val file = File("some.proto")
        val lineNumber = 100
        val columnNumber = 500
        val errorMessage = "Some error."
        val exception = assertThrows<Compilation.Error> {
            Compilation.error(file, lineNumber, columnNumber, errorMessage)
        }
        exception.message.let {
            it shouldContain file.path
            it shouldContain "$lineNumber:$columnNumber"
            it shouldContain errorMessage
        }
    }

    @Test
    fun `print the error message to the system error stream`() {
        val file = Paths.get("nested/dir/file.proto").toFile()
        val lineNumber = 10
        val columnNumber = 5
        val errorMessage = "Testing console output."
        val consoleOutput = tapConsole {
            assertThrows<Compilation.Error> {
                Compilation.error(file, lineNumber, columnNumber, errorMessage)
            }
        }
        consoleOutput.let {
            it shouldContain file.path
            it shouldContain "$lineNumber:$columnNumber"
            it shouldContain errorMessage
        }
    }
}
