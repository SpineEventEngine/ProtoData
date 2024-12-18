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

import io.spine.base.Mistake
import io.spine.environment.Tests
import java.io.File
import kotlin.system.exitProcess

/**
 * Provides functions to report compilation errors and warnings.
 *
 * The object has two modes of handling the compilation [error].
 * In the production mode, the [error] method prints an error message to [System.err] and
 * exits the process with [ERROR_EXIT_CODE].
 *
 * In the testing mode, the [error] method throws [Compilation.Error] exception with
 * the same error message as printed to the console in the production mode.
 *
 * The execution mode is [detected][Tests.enabled] via the [Tests] environment type.
 *
 * @see Tests
 */
public object Compilation {

    /**
     * Tells if the code generation is performed under tests.
     */
    private val underTests: Boolean
        get() = Tests.type().enabled()

    /**
     * The exit code for a compilation error.
     */
    public const val ERROR_EXIT_CODE: Int = -1

    /**
     * Prints the error diagnostics to [System.err] and terminates the compilation.
     *
     * The termination of the compilation in the production mode is done by
     * exiting the process with [ERROR_EXIT_CODE].
     *
     * If the code is run [under tests][Tests] the method throws [Compilation.Error].
     *
     * @param file The file in which the error occurred.
     * @param line The one-based number of the line with the error.
     * @param column The one-based number of the column with the error.
     * @param message The description of what went wrong.
     * @throws Compilation.Error exception when called under tests.
     */
    @Suppress("TooGenericExceptionThrown") // False positive from detekt.
    public fun error(file: File, line: Int, column: Int, message: String): Nothing {
        val output = errorMessage(file, line, column, message)
        System.err.println(output)
        if (underTests) {
            throw Error(output)
        } else {
            exitProcess(ERROR_EXIT_CODE)
        }
    }

    private fun errorMessage(
        file: File,
        line: Int,
        column: Int,
        message: String
    ) = "e: $file:$line:$column: $message"

    /**
     * Prints the warning diagnostics to [System.out].
     *
     * The method returns the string printed to the console so that it could be also
     * put into logging output by the calling code.
     *
     * @param file The file which causes the warning.
     * @param line The one-based number of the line with the questionable code.
     * @param column The one-based number of the column with the questionable code.
     * @param message The description of the warning.
     * @return the string printed to the console.
     */
    public fun warning(file: File, line: Int, column: Int, message: String): String {
        val output = "w: $file:$line:$column: $message"
        println(output)
        return output
    }

    /**
     * The exception thrown by [Compilation.error] when the testing mode is on.
     */
    public class Error(message: String) : Mistake(message) {
        public companion object {
            private const val serialVersionUID: Long = 0L
        }
    }
}
