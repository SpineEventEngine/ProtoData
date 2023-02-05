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

import com.google.common.annotations.VisibleForTesting
import io.spine.base.Time
import io.spine.base.Time.currentTime
import io.spine.base.Time.currentTimeZone
import io.spine.protodata.codegen.java.annotation.GeneratedTypeAnnotation.Companion.PROTODATA_CLI
import io.spine.protodata.renderer.SourceFile
import io.spine.time.toInstant
import java.time.OffsetDateTime
import javax.annotation.processing.Generated

/**
 * Adds the [javax.annotation.Generated] annotation to the top-level declaration of each Java file
 * in the source set.
 *
 * Deriving classes are likely to inherit the class to pass values to the properties, providing
 * a no-argument constructor, as required for a [Renderer][io.spine.protodata.renderer.Renderer].
 *
 * @see io.spine.protodata.codegen.java.annotation.TypeAnnotation
 */
public open class GeneratedTypeAnnotation(

    /**
     * The name of the code generator as required in [Generated.value].
     *
     * The default value for this parameter is [PROTODATA_CLI].
     */
    protected val generator: String = PROTODATA_CLI,

    /**
     * Tells if the annotated code should have [Generated.date] parameter.
     * If `true`, the value will be set to the moment at local time when
     * the annotation was generated.
     */
    protected val addTimestamp: Boolean = false,

    /**
     * A callback for obtaining an argument for the [Generated.comments] parameter
     * by the given [SourceFile]. The default value is `null`.
     * If not specified, the [comments][Generated.comments] parameter will non be used.
     */
    protected val commenter: ((SourceFile) -> String)? = null

) : TypeAnnotation<Generated>(Generated::class.java) {

    override fun renderAnnotationArguments(file: SourceFile): String {
        if (!addTimestamp && commenter == null) {
            return "\"$generator\""
        }
        return multiLineArguments(file)
    }

    private fun multiLineArguments(file: SourceFile): String {
        val date = date()
        val comments = renderComments(file)
        val nl = System.lineSeparator()
        val params = buildList {
            fun String.enter() {
                if (isNotBlank()) this@buildList.add(this@enter)
            }
            "value = \"$generator\"".enter()
            date.enter()
            comments.enter()
        }.joinToString(separator = ",$nl")
        return nl + params.prependIndent(INDENT) + nl
    }

    /**
     * Generates the value of the [Generated.date] parameter using
     * the current time with offset, as defined in the documentation for
     * the [Generated.date] parameter.
     */
    private fun date(): String {
        return if (addTimestamp) {
            "date = \"${currentDateTime()}\""
        } else {
            ""
        }
    }

    private fun renderComments(file: SourceFile): String {
        return if (commenter != null) {
            val value = commenter.invoke(file)
            "comments = \"$value\""
        } else {
            ""
        }
    }

    public companion object {

        private const val INDENT_SIZE = 4

        internal val INDENT = " ".repeat(INDENT_SIZE)

        /**
         * The fully qualified name of the ProtoData command-line application main class.
         */
        public const val PROTODATA_CLI: String = "io.spine.protodata.cli.app.Main"

        /**
         * Obtains the representation of the current time with the offset,
         * as defined in the documentation of [Generated.date] parameter.
         *
         * The curren time is obtained via [Time.currentTime] so that tests can supply
         * custom [io.spine.base.Time.Provider].
         */
        @VisibleForTesting
        public fun currentDateTime(): String {
            val now = currentTime().toInstant()
            val dateTime = OffsetDateTime.ofInstant(now, currentTimeZone())
            return dateTime.toString()
        }
    }
}
