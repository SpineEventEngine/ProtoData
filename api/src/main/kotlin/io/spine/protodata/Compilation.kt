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

import java.io.File
import kotlin.system.exitProcess

/**
 * Provides functions to report compilation errors and warnings.
 */
public object Compilation {

    /**
     * The exit code for a compilation error.
     */
    public const val ERROR_EXIT_CODE: Int = -1

    /**
     * Prints the error diagnostics to [System.err] and exits the process with [ERROR_EXIT_CODE].
     *
     * @param file The file in which the error occurred.
     * @param line The one-based number of the line with the error.
     * @param column The one-based number of the column with the error.
     * @param message The description of what went wrong.
     */
    public fun error(file: File, line: Int, column: Int, message: String) {
        val output = "e: $file:$line:$column: $message"
        System.err.println(output)
        exitProcess(ERROR_EXIT_CODE)
    }

    /**
     * Prints the warning diagnostics to [System.out].
     *
     * The method returns the string printed to the console so that it could be also
     * put into logging output.
     *
     * @param file The file in which causes the warning.
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
}
