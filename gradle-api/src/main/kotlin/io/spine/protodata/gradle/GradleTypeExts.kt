/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.protodata.gradle

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceSet

import io.spine.protodata.Constants.LOGGING_PREFIX

/**
 * Obtains the name of this source set with the first letter capitalized.
 */
internal val SourceSet.capitalizedName: String
    get() = name.replaceFirstChar { it.uppercase() }

/**
 * Logs the given message if the `DEBUG` level is enabled.
 *
 * The message will get the [LOGGING_PREFIX].
 */
private fun String.withPrefix(): String = "$LOGGING_PREFIX$this"

public fun Logger.debug(message: () -> String) {
    if (isDebugEnabled) {
        debug(message().withPrefix())
    }
}

/**
 * Logs the given message if the `ERROR` level is enabled.
 *
 * The message will get the [LOGGING_PREFIX].
 */
public fun Logger.error(message: () -> String) {
    if (isErrorEnabled) {
        error(message().withPrefix())
    }
}

/**
 * Logs the given message if the `INFO` level is enabled.
 *
 * The message will get the [LOGGING_PREFIX].
 */
public fun Logger.info(message: () -> String) {
    if (isInfoEnabled) {
        info(message().withPrefix())
    }
}
