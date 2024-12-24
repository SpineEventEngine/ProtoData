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

package io.spine.protodata.cli.app

import io.spine.protodata.plugin.Plugin
import io.spine.reflect.Factory
import io.spine.string.Separator.Companion.nl
import io.spine.string.pi
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * The factory for creating [Plugin]s by the names of their classes.
 *
 * If a class could not be found, the factory reports the error using the [callback][printError]
 * passed to the constructor, and then terminates the process with exit code [1][EXIT_CODE].
 *
 * @param classLoader The loader for the classes of the plugins.
 * @param classpath The user classpath used for finding plugin classes.
 * @param printError The function given to the factory for reporting errors.
 */
internal class PluginFactory(
    classLoader: ClassLoader,
    private val classpath: List<Path>?,
    private val printError: (String?) -> Unit
): Factory<Plugin>(classLoader) {

    /**
     * Creates plugins by their class names.
     */
    fun load(classNames: List<String>): List<Plugin> {
        return classNames.map { tryCreate(it) }
    }

    private fun tryCreate(className: String): Plugin {
        try {
            return create(className)
        } catch (e: ClassNotFoundException) {
            printError(e.stackTraceToString())
            printAddingToClasspath(className)
            exitProcess(EXIT_CODE)
        }
    }

    private fun printAddingToClasspath(className: String) {
        printError("Please add the required class `$className` to the user classpath.")
        if (classpath == null) {
            printError("No user classpath was provided.")
            return
        }

        printError("Provided user classpath:")
        val cp = classpath
        val cpStr = cp.joinToString(separator = nl()).pi(indent = " ".repeat(2))
        printError(cpStr)
    }

    private companion object {
        /**
         * The value for the exit code of the process in case of an error during plugin creation.
         */
        const val EXIT_CODE = 1
    }
}
