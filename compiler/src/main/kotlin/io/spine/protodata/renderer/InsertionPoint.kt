/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.renderer

import com.google.common.flogger.StackSize.FULL
import com.google.protobuf.Empty
import io.spine.annotation.Internal
import io.spine.logging.Logging.loggerFor
import io.spine.protodata.FileCoordinates
import io.spine.protodata.FileCoordinates.SpecCase.END_OF_FILE
import io.spine.protodata.FileCoordinates.SpecCase.INLINE
import io.spine.protodata.FileCoordinates.SpecCase.NOT_IN_FILE
import io.spine.protodata.FileCoordinates.SpecCase.WHOLE_LINE
import io.spine.protodata.TypeName
import io.spine.protodata.fileCoordinates
import io.spine.protodata.qualifiedName
import io.spine.text.Position
import io.spine.text.Text
import io.spine.text.TextFactory.text

/**
 * A point is a source file, where more code may be inserted.
 */
public interface InsertionPoint : CoordinatesFactory {

    public companion object {

        /**
         * The number of characters which right-pad an insertion point [codeLine] when it is added
         * to an existing line of code.
         *
         * Since insertion points are added as comments, the comments must be closed in order for
         * the code after the insertion point to be compilable. This number of characters includes
         * any comment-closing syntax (e.g. the asterisk and slash in Kotlin, Java, and
         * some other languages). After the comment-closing syntax follow while-space characters.
         *
         * If the comment-closing characters are too many, an error occurs.
         *
         * This padding is constant for all languages and all renderers. When inserting code into
         * an exiting line, a renderer will always respect this padding.
         */
        internal const val COMMENT_PADDING_LENGTH: Int = 8
    }

    /**
     * The name of this insertion point.
     */
    public val label: String

    /**
     * Locates the line number where the insertion point should be added.
     *
     * An insertion point should only appear once in a file.
     *
     * @param
     *     lines a list of code lines in a source file
     * @return the line number at which the insertion point should be added.
     * @see LineNumber
     */
    @Deprecated("Use locate(Text) instead.")
    public fun locate(lines: List<String>): LineNumber {
        val coords = locate(text(lines))
        return when (coords.specCase) {
            WHOLE_LINE -> LineNumber.at(coords.wholeLine)
            INLINE -> {
                loggerFor(InsertionPoint::class.java)
                    .atWarning()
                    .withStackTrace(FULL)
                    .log("`locate(List<String>)` does not support inline insertion. " +
                            "A whole line insertion will be generated.")
                LineNumber.at(coords.inline.line)
            }
            END_OF_FILE -> LineNumber.endOfFile()
            NOT_IN_FILE -> LineNumber.notInFile()
            else -> error("Unexpected file coordinates `$coords`.")
        }
    }

    /**
     * Locates the site where the insertion point should be added.
     *
     * An insertion point should only appear once in a file.
     *
     * @param text the existing code
     * @return the coordinates in the file where the insertion point should be added
     * @see SourceFile.at
     * @see SourceFile.atInline
     */
    public fun locate(text: Text): FileCoordinates
}

/**
 * A factory of [FileCoordinates] instances.
 *
 * This interface serves as a trait for the [InsertionPoint] type. The methods it provides are meant
 * to be used by the authors of custom insertion points.
 */
@Internal
public sealed interface CoordinatesFactory {

    /**
     * Creates coordinates pointing at a specific line and column in the file.
     */
    public fun at(line: Int, column: Int): FileCoordinates = fileCoordinates {
        inline = Position.newBuilder()
            .setLine(line)
            .setColumn(column)
            .build()
    }

    /**
     * Creates coordinates pointing at a specific line in the file.
     */
    public fun atLine(line: Int): FileCoordinates = fileCoordinates {
        wholeLine = line
    }

    /**
     * Creates coordinates pointing at the first line in the file.
     */
    public fun startOfFile(): FileCoordinates =
        atLine(0)

    /**
     * Creates coordinates pointing at the point after the last line in the text.
     */
    public fun endOfFile(): FileCoordinates = fileCoordinates {
        endOfFile = Empty.getDefaultInstance()
    }

    /**
     * Creates coordinates that do not point at anywhere in the file.
     */
    public fun nowhere(): FileCoordinates = fileCoordinates {
        notInFile = Empty.getDefaultInstance()
    }
}

/**
 * The marker representing this [InsertionPoint] in the code.
 *
 * This property is an extension rather than a normal property because we don't want users
 * to override it.
 */
public val InsertionPoint.codeLine: String
    get() = if (this is ProtocInsertionPoint) {
        protocStyleCodeLine
    } else {
        "INSERT:'${label}'"
    }

/**
 * An [InsertionPoint] already generated by the Protobuf compiler.
 *
 * The user is responsible for ensuring that the insertion point exists and the label is
 * spelled correctly.
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
    public override val label: String,
) : InsertionPoint {

    /**
     * Creates a Protobuf compiler-style insertion point by the standard formula:
     * ```
     * <scope>:<qualified type name>
     * ```
     */
    public constructor(scope: String, type: TypeName) : this("$scope:${type.qualifiedName()}")

    override fun locate(text: Text): FileCoordinates =
        nowhere()

    /**
     * The code line in the Protobuf compiler style.
     */
    public val protocStyleCodeLine: String
        get() = "@@protoc_insertion_point($label)"
}

/**
 * A pointer to a line in a source file.
 */
@Deprecated("User Protobuf-based `FileCoordinates` instead.")
public sealed class LineNumber {

    public companion object {

        /**
         * Creates a `LineNumber` pointing at a given line.
         */
        @JvmStatic
        public fun at(number: Int): LineNumber = LineIndex(number)

        /**
         * Creates a `LineNumber` pointing at the start of the file.
         *
         * This is a convenience method equivalent to calling `at(0)`.
         */
        @JvmStatic
        public fun startOfFile(): LineNumber = at(0)

        /**
         * Creates a `LineNumber` pointing at the end of the file, no matter the index of the actual
         * line.
         */
        @JvmStatic
        public fun endOfFile(): LineNumber = EndOfFile

        /**
         * Creates a `LineNumber` not pointing at any line.
         */
        @JvmStatic
        public fun notInFile(): LineNumber = Nowhere
    }
}

/**
 * A [LineNumber] pointing at a particular line.
 *
 * Implementation note. We do not use unsigned integers here by design. `UInt`s in Kotlin are
 * designed to only provide the whole bit range, not to insure invariants.
 * See [this thread](https://youtrack.jetbrains.com/issue/KT-46144) for more details.
 */
@Deprecated("Use Protobuf-based `FileCoordinates.whole_line` instead.")
internal data class LineIndex constructor(val value: Int) : LineNumber() {
    init {
        if (value < 0) {
            throw IndexOutOfBoundsException("Invalid line number: `$value`.")
        }
    }
}


@Deprecated("Use Protobuf-based `FileCoordinates.end_of_file` instead.")
internal object EndOfFile : LineNumber()

/**
 * A [LineNumber] representing that the looked up line is nowhere to be found in the file.
 */
@Deprecated("Use Protobuf-based `FileCoordinates.not_in_file` instead.")
internal object Nowhere : LineNumber()
