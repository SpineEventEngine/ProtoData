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

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Splitter
import io.spine.logging.Logging
import io.spine.protodata.InsertedPoint
import io.spine.protodata.InsertedPoints
import io.spine.protodata.filePath
import io.spine.protodata.splitLines
import io.spine.server.query.select
import io.spine.text.Text
import io.spine.text.TextFactory.checkNoSeparator
import io.spine.text.TextFactory.text
import io.spine.util.interlaced
import java.lang.System.lineSeparator
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.util.regex.Pattern
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * A file with source code.
 *
 * This file is a part of a source set. It should be treated as a part of a software module rather
 * than a file system object. One `SourceFile` may reflect multiple actual FS files. For example,
 * a `SourceFile` may be read from one location on the FS and written into another location.
 */
@Suppress("TooManyFunctions") /* Those functions constitute the primary API and
                                         should not be represented as extensions. */
public class SourceFile
private constructor(

    /**
     * The source code.
     */
    private var code: String,

    /**
     * The FS path to the file relative to the source root.
     */
    public val relativePath: Path,

    private var changed: Boolean = false
) {

    private lateinit var sources: SourceFileSet
    private val preReadActions = mutableListOf<(SourceFile) -> Unit>()
    private var alreadyRead = false

    public companion object {

        /**
         * A splitter to divide code fragments into lines.
         *
         * Uses a regular expression to match line breaks, with or without carriage returns.
         */
        public val lineSplitter: Splitter = Splitter.on(Pattern.compile("\r?\n"))

        /**
         * Reads the file from the given FS location.
         */
        internal fun read(
            relativePath: Path,
            sourceRoot: Path,
            charset: Charset = Charsets.UTF_8
        ): SourceFile {
            val absolute = sourceRoot / relativePath
            return SourceFile(absolute.readText(charset), relativePath)
        }

        /**
         * Constructs a file from source code.
         *
         * @param relativePath the FS path for the file relative to the source root; the file might
         *             not exist on the file system
         * @param code the source code
         */
        internal fun fromCode(relativePath: Path, code: String): SourceFile =
            SourceFile(code, relativePath, changed = true)
    }

    /**
     * Creates a new fluent builder for adding code at the given [insertionPoint].
     *
     * If the [insertionPoint] is not found in the code, no action will be performed as the result.
     * If there are more than one instances of the same insertion point, the code will be added to
     * all of them.
     *
     * Insertion points should be marked with comments of special format. The added code is always
     * inserted after the line with the comment, and the line with the comment is preserved.
     *
     * @see atInline
     */
    public fun at(insertionPoint: InsertionPoint): SourceAtPoint =
        SourceAtPoint(this, insertionPoint)

    /**
     * Creates a new fluent builder for adding code at the given inline [insertionPoint].
     *
     * If the [insertionPoint] is not found in the code, no action will be performed as the result.
     * If there are more than one instances of the same insertion point, the code will be added to
     * all of them.
     *
     * Code inserted via the resulting fluent builder, unlike code inserted via [SourceFile.at],
     * will be placed right into the line that contains the insertion point. Authors of [Renderer]s
     * may choose to either use insertion points in the whole-line mode or in the inline mode. Also,
     * the same insertion point may be used in both modes.
     *
     * @see at
     */
    public fun atInline(insertionPoint: InsertionPoint): SourceAtPointInline {
        val points = sources.querying.select<InsertedPoints>()
            .find(filePath { value = relativePath.toString() })
            .orElseThrow()
        val point = points.pointList.filter { it.label == insertionPoint.label }.firstOrNull()
        return if (point != null) {
            Located(this@SourceFile, point)
        } else {
            Unlocated
        }
    }

    /**
     * Deletes this file from the source set.
     *
     * As the result of this method, the associated source file will be eventually removed from
     * the file system.
     *
     * If the file was created earlier (by the same or a different [Renderer]), the file will not
     * be written to the file system.
     *
     * After this method, the file will no longer be accessible via associated the `SourceSet`.
     */
    public fun delete() {
        sources.delete(relativePath)
    }

    /**
     * Changes the contents of this file to the provided [newCode].
     *
     * **Note.** This method may overwrite the work of other [Renderer]s, as well as remove
     * the insertion points from the file. Use with caution. Prefer using [at(InsertionPoint)][at]
     * when possible.
     */
    public fun overwrite(newCode: String) {
        this.code = newCode
        this.changed = true
    }

    /**
     * Overwrites the code in this file line by line.
     */
    internal fun updateLines(newCode: List<String>) {
        overwrite(newCode.joinToString(lineSeparator()))
    }

    /**
     * Injects the given [sources].
     */
    internal fun attachTo(sources: SourceFileSet) {
        this.sources = sources
    }

    /**
     * Writes the source code into the file on the file system.
     *
     * It may be the case that the file is read from one directory (source) and written into another
     * directory (target). Thus, the initial path from where the file is read may not coincide with
     * the path from where the file is written.
     *
     * @param rootDir the directory into which the file should be written;
     *                this file's [relativePath] is resolved upon this directory
     * @param charset the charset to use to write the file; UTF-8 is the default
     * @param forceWrite if `true`, this file must be written to the FS even if no changes have been
     *                   done upon it; otherwise, the file may not be written to avoid unnecessary
     *                   file system operations
     */
    internal fun write(
        rootDir: Path,
        charset: Charset = Charsets.UTF_8,
        forceWrite: Boolean = false
    ) {
        if (changed || forceWrite) {
            val targetPath = rootDir / relativePath
            targetPath.toFile()
                .parentFile
                .mkdirs()
            targetPath.writeText(code, charset, WRITE, TRUNCATE_EXISTING, CREATE)
        }
    }

    /**
     * Deletes this source file from the file system.
     *
     * It may be the case that the file is read from one directory (source) and changed in another
     * directory (target). Thus, the initial path from where the file is read may not coincide with
     * the path from where the file is deleted.
     *
     * @param rootDir the root directory where the file lies; the [relativePath] is resolved upon
     *                this directory
     * @see write
     */
    internal fun rm(rootDir: Path) {
        val targetPath = rootDir / relativePath
        targetPath.toFile().deleteRecursively()
    }

    /**
     * Obtains the entire content of this file.
     */
    @Deprecated("Use `text()` instead.", ReplaceWith("text()"))
    public fun code(): String {
        return text().value
    }

    /**
     * Obtains the entire content of this file as a list of lines.
     */
    public fun lines(): List<String> {
        return text().splitLines()
    }

    /**
     * Obtains the text content of this source file.
     */
    public fun text(): Text {
        initializeCode()
        return text(code)
    }

    internal fun whenRead(action: (SourceFile) -> Unit) {
        preReadActions.add(action)
        if (alreadyRead) {
            action(this)
        }
    }

    private fun initializeCode() {
        if (!alreadyRead) {
            alreadyRead = true
            preReadActions.forEach { it(this) }
        }
    }

    override fun toString(): String = relativePath.toString()
}

/**
 * A fluent builder for inserting code into pre-prepared insertion points.
 *
 * @see SourceFile.at
 */
public class SourceAtPoint
internal constructor(
    private val file: SourceFile,
    private val point: InsertionPoint
) {

    private var indentLevel: Int = 0

    /**
     * Specifies extra indentation to be added to inserted code lines
     *
     * Each unit adds four spaces.
     */
    public fun withExtraIndentation(level: Int): SourceAtPoint {
        checkArgument(level >= 0, "Indentation level cannot be negative.")
        indentLevel = level
        return this
    }

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *      code lines
     */
    public fun add(vararg lines: String): Unit =
        add(lines.toList())

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *      code lines
     */
    public fun add(lines: Iterable<String>) {
        val sourceLines = file.lines()
        val updatedLines = ArrayList(sourceLines)
        val pointMarker = point.codeLine
        val newCode = lines.linesToCode(indentLevel)
        sourceLines.mapIndexed { index, line -> index to line }
                   .filter { (_, line) -> line.contains(pointMarker) }
                   .map { it.first + 1 }
                   .forEach { index -> updatedLines.add(index, newCode) }
        file.updateLines(updatedLines)
    }
}


public sealed interface SourceAtPointInline {
    /**
     * Adds the specified code fragment into the insertion point.
     *
     * When called multiple times for the same insertion point, the code that is added last
     * will appear first in the file, since new code fragments are always added right after
     * the insertion point regardless of if it's been used before.
     */
    public fun add(codeFragment: String)
}

/**
 * A [SourceAtPointInline] at a point that could not be located.
 *
 * No code is generated by this implementation.
 */
internal object Unlocated : SourceAtPointInline, Logging {
    override fun add(codeFragment: String) {
//        throw IllegalStateException("!!!")
    }
}

/**
 * A fluent builder for inserting code into pre-prepared inline insertion points.
 *
 * @see SourceFile.atInline
 */
internal class Located(
    private val file: SourceFile,
    private val point: InsertedPoint
) : SourceAtPointInline {

    override fun add(codeFragment: String) {
        checkNoSeparator(codeFragment)
        val sourceLines = file.lines()
        val updatedLines = ArrayList(sourceLines)
        sourceLines.asSequence()
            .mapIndexed { index, line -> CodeLine(index, line) }
            .map { line -> line.insertInline(point, codeFragment) }
            .forEach { (index, line) -> updatedLines[index] = line }
        file.updateLines(updatedLines)
    }
}

private fun String.splitByIndexes(indexes: List<Int>): List<String> = buildList {
    val idxs = buildList(indexes.size + 2) {
        add(0)
        addAll(indexes)
        add(length)
    }
    idxs.forEachIndexed { listIndex, stringIndex ->
        if (listIndex < idxs.size - 1) {
            val nextIndex = idxs[listIndex + 1]
            add(substring(stringIndex, nextIndex))
        }
    }
}

private fun String.findInsertionIndexes(point: InsertedPoint): List<Int> {
    val comment = point.representationInCode
    var index = 0
    return buildList {
        while (true) {
            val rawIndex = indexOf(comment, startIndex = index)
            if (rawIndex >= 0) {
                index = rawIndex + comment.length
                add(index)
            } else {
                break
            }
        }
    }
}

private fun Iterable<String>.linesToCode(indentLevel: Int): String =
    joinToString(lineSeparator()) {
        INDENT.repeat(indentLevel) + it
    }

private const val INDENT: String = "    "

/**
 * A numbered line of code in a file.
 */
private data class CodeLine(val lineIndex: Int, val content: String) {

    /**
     * Inserts the `newCode` at the given `insertionPoint` into this line and obtains the resulting
     * line of code.
     *
     * The index of the new line is always the same as the index of the old line.
     */
    fun insertInline(point: InsertedPoint, newCode: String): CodeLine {
        val indexes = content.findInsertionIndexes(point).toList()
        if (indexes.isEmpty()) {
            return this
        }
        val parts = content.splitByIndexes(indexes)
        val newLine = parts.interlaced(newCode).joinToString(separator = "")
        return CodeLine(lineIndex, newLine)
    }
}
