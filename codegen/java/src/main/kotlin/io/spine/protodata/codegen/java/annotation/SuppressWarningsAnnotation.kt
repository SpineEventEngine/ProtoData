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

package io.spine.protodata.codegen.java.annotation

import io.spine.protodata.codegen.java.file.BeforePrimaryDeclaration
import io.spine.protodata.config.configAs

/**
 * Suppresses warnings in the generated code.
 *
 * If no configuration is provided to ProtoData, suppresses all the warnings with `"ALL"`.
 * Otherwise, parses the config as a [SuppressionSettings] and suppresses only the specified warnings.
 *
 * Warnings in the generated code do no good for the user, as they cannot be fixed without changing
 * the code generation logic. We recommend suppressing them.
 *
 * @see io.spine.protodata.codegen.java.annotation.TypeAnnotation
 */
public class SuppressWarningsAnnotation :
    TypeAnnotation<SuppressWarnings>(SuppressWarnings::class.java) {

    public companion object {
        public val ALL_WARNINGS: List<String> = listOf("ALL")
    }

    override fun renderAnnotationArguments(): String = "{${warningList()}}"

    /**
     * Obtains the code for suppressing configured warnings.
     *
     * If [SuppressionSettings] are not available, or the list of warnings is empty,
     * the method assumes [ALL_WARNINGS] are to be suppressed.
     *
     * If settings are given, takes the list of warnings from the instance, removing single-
     * or double quotes that could be in the values
     *
     * **NOTE:** [ALL_WARNINGS] are assumed to avoid the case of working with a default instance
     * of [SuppressionSettings] (e.g. when it was loaded from a file). Obviously, the user did
     * not indent to suppress an empty list of warnings, if [SuppressionSettings] instance was added
     * to ProtoData settings but no specific warnings were specified.
     */
    private fun warningList(): String {
        val warnings = if (!configIsPresent()) {
            ALL_WARNINGS
        } else {
            val configured = configAs<SuppressionSettings>().warnings.valueList
            if (configured.isEmpty()) {
                ALL_WARNINGS
            } else {
                val withQuotesStripped = configured.map {
                    it.replace("\"", "").replace("\'", "")
                }
                withQuotesStripped
            }
        }
        val warningsList = warnings.joinToString { '"' + it + '"' }
        return warningsList
    }
}
