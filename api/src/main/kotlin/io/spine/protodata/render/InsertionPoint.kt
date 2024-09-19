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

package io.spine.protodata.render

import io.spine.logging.WithLogging
import io.spine.protodata.TypeName
import io.spine.protodata.qualifiedName
import io.spine.text.Text
import io.spine.text.TextCoordinates

/**
 * A point is a source file, where more code may be inserted.
 */
public interface InsertionPoint : io.spine.protodata.render.CoordinatesFactory, WithLogging {

    /**
     * The name of this insertion point.
     *
     * The name of the insertion point is used to formally identify the place
     * where the code should be inserted. It could be a place in the code marked
     * by [Protobuf compiler][ProtocInsertionPoint], or a custom insertion point added
     * by [InsertionPointPrinter].
     *
     * Classes implementing custom insertion points may use an empty string if
     * no printing of the insertion point is required.
     *
     * @see ProtocInsertionPoint
     * @see [codeLine]
     */
    public val label: String

    /**
     * Locates the sites where the insertion point should be added.
     *
     * An insertion point can appear multiple times in a given code file.
     *
     * @param text
     *         the existing code.
     * @return the coordinates in the text where the insertion point should be added.
     * @see SourceFile.at
     * @see SourceFile.atInline
     */
    public fun locate(text: String): Set<TextCoordinates>

    @Deprecated(
        message = "Use `locate(String)` instead.",
        replaceWith = ReplaceWith("locate(text.value)")
    )
    public fun locate(text: Text): Set<TextCoordinates> = locate(text.value)

    private fun logUnsupportedKind() =
        logger.atWarning().log {
            "`locate(List<String>)` does not support inline insertion. " +
                    "A whole line insertion will be generated."
        }
}

/**
 * An insertion point that can only occur once per code file.
 *
 * Implementations should use [locateOccurrence] instead of [locate].
 */
public interface NonRepeatingInsertionPoint : io.spine.protodata.render.InsertionPoint {

    /**
     * Locates the site where the insertion point should be added.
     *
     * This insertion point should only appear once in a file.
     *
     * If the insertion point is not found, implementation must return
     * [nowhere][CoordinatesFactory.nowhere].
     *
     * @param text
     *         the existing code.
     * @return the coordinates in the text where the insertion point should be added, or
     *         [nowhere][CoordinatesFactory.nowhere] if the insertion point is not found.
     * @see SourceFile.at
     * @see SourceFile.atInline
     */
    public fun locateOccurrence(text: String): TextCoordinates

    @Deprecated(
        message = "Use `locateOccurrence(String)` instead.",
        replaceWith = ReplaceWith("locateOccurrence(text.value)")
    )
    public fun locateOccurrence(text: Text): TextCoordinates = locateOccurrence(text.value)

    /**
     * Locates the site where the insertion point should be added.
     *
     * The default implementation returns a set with one element obtained from [locateOccurrence].
     * Implementing classes should avoid overriding the default implementation, to preserve
     * the semantic of non-repeating offered by this interface.
     *
     * @see locateOccurrence
     */
    override fun locate(text: String): Set<TextCoordinates> =
        setOf(locateOccurrence(text))
}

/**
 * The marker representing this [InsertionPoint] in the code.
 *
 * This property is an extension rather than a normal property because
 * we don't want users to override it.
 *
 * @return the code line that represents this insertion point or empty string if
 *         the label of the insertion point is empty.
 */
public val io.spine.protodata.render.InsertionPoint.codeLine: String
    get() = if (this is io.spine.protodata.render.ProtocInsertionPoint) {
        protocStyleCodeLine
    } else {
        if (label.isEmpty()) "" else "INSERT:'${label}'"
    }

/**
 * An [InsertionPoint] already generated by the Protobuf compiler.
 *
 * The user is responsible for ensuring that the insertion point exists and
 * the label is spelled correctly.
 *
 * [InsertionPointPrinter]s will never add such a point to exising code.
 * However, if Protoc generates one, the users may base their code generation on it.
 *
 * @property label The name of the insertion point. Typically, starts with the identifier of
 * the scope, followed by a colon, followed by the type name. For example:
 * ```
 * message_scope:acme.corp.user.UserName
 * ```
 */
public class ProtocInsertionPoint(
    public override val label: String
) : io.spine.protodata.render.InsertionPoint {

    /**
     * Creates a Protobuf compiler-style insertion point by the standard formula:
     * ```
     * <scope>:<qualified type name>
     * ```
     */
    public constructor(scope: String, type: TypeName) : this("$scope:${type.qualifiedName}")

    override fun locate(text: String): Set<TextCoordinates> = buildSet {
        text.lines().mapIndexed { index, line ->
            if (line.contains(codeLine)) {
                add(atLine(index + 1))
            }
        }
    }

    /**
     * The code line in the Protobuf compiler style.
     */
    public val protocStyleCodeLine: String
        get() = "@@protoc_insertion_point($label)"
}
