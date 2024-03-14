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

package io.spine.protodata.renderer

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Splitter
import io.spine.protodata.InsertedPoints
import io.spine.protodata.file
import io.spine.server.query.select
import io.spine.text.Text
import io.spine.text.TextFactory.text
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
 * This file is a part of a [source set][SourceFileSet]. It should be treated as
 * a part of a software module rather than a file system object.
 * One `SourceFile` may reflect multiple actual files on a file system. For example,
 * a `SourceFile` may be read from one location on the file system and written
 * into another location.
 *
 * @see SourceFileSet
 */
@Suppress(
    "TooManyFunctions"
    /* Those functions constitute the primary API and should not be represented as extensions. */
)
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

    /**
     * The full path to the file created in the [output root][SourceFileSet.outputRoot] of
     * the source file set to which this file belongs.
     */
    public val outputPath: Path by lazy {
        sources.outputRoot / relativePath
    }

    public companion object {

        /**
         * A splitter to divide code fragments into lines.
         *
         * Uses a regular expression to match line breaks, with or without carriage returns.
         */
        @Deprecated("Please use `CharSequence.lines()` instead.")
        public val lineSplitter: Splitter = Splitter.on(Pattern.compile("\r?\n"))

        /**
         * Reads the file from the given FS location.
         */
        internal fun read(
            sourceRoot: Path,
            relativePath: Path,
            charset: Charset = Charsets.UTF_8
        ): SourceFile {
            val absolute = sourceRoot / relativePath
            val code = absolute.readText(charset)
            return SourceFile(code, relativePath)
        }

        /**
         * Constructs a file from source code.
         *
         * @param relativePath
         *         the FS path for the file relative to the source root; the file might
         *         not exist on the file system.
         * @param code
         *         the source code.
         */
        @VisibleForTesting
        public fun fromCode(relativePath: Path, code: String): SourceFile =
            SourceFile(code, relativePath, changed = true)
    }

    /**
     * Creates a new fluent builder for adding code at the given [insertionPoint].
     *
     * If the [insertionPoint] is not found in the code, no action will be
     * performed as the result. If there is more than one instance of
     * the same insertion point, the code will be added to all of them.
     *
     * Insertion points should be marked with comments of special format.
     * The added code is always inserted after the line with the comment, and
     * the line with the comment is preserved.
     *
     * @see atInline
     */
    public fun at(insertionPoint: InsertionPoint): SourceAtLine =
        SourceAtLine(this, insertionPoint)

    /**
     * Creates a new fluent builder for adding code at the given inline [insertionPoint].
     *
     * If the [insertionPoint] is not found in the code, no action will be performed as the result.
     * If there is more than one instance of the same insertion point, the code will be added to
     * all of them.
     *
     * Code inserted via the resulting fluent builder, unlike code inserted via [SourceFile.at],
     * will be placed right into the line that contains the insertion point. Authors of [Renderer]s
     * may choose to either use insertion points in the whole-line mode or in the inline mode. Also,
     * the same insertion point may be used in both modes.
     *
     * @see at
     */
    public fun atInline(insertionPoint: InsertionPoint): SourceAtPoint {
        val points = sources.querying.select<InsertedPoints>()
            .findById(file { path = relativePath.toString() })
        val point = points?.pointList?.firstOrNull { it.label == insertionPoint.label }
        return if (point != null) {
            SpecificPoint(this@SourceFile, point)
        } else {
            NoOp
        }
    }

    /**
     * Deletes this file from the source set.
     *
     * As a result of this method, the associated source file will be
     * eventually removed from the file system.
     *
     * If the file was created earlier (by the same or a different [Renderer]),
     * the file will not be written to the file system.
     *
     * After this method, the file will no longer be accessible via
     * the associated `SourceSet`.
     */
    public fun delete() {
        sources.delete(relativePath)
    }

    /**
     * Changes the contents of this file to the provided [newCode].
     *
     * **Note** This method may overwrite the work of other [Renderer]s, as well
     * as remove the insertion points from the file. Use with caution.
     * Prefer using [at(InsertionPoint)][at] when possible.
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
     * It may be the case that the file is read from one directory (source) and
     * written into another directory (target). Thus, the initial path from where
     * the file is read may not coincide with the path from where the file is written.
     *
     * @param baseDir
     *         the directory into which the file should be written;
     *         this file's [relativePath] is resolved upon this directory.
     * @param charset
     *         the charset to use to write the file; UTF-8 is the default.
     * @param forceWrite
     *         if `true`, this file must be written to the FS even if no changes have been
     *         done upon it; otherwise, the file may not be written to avoid unnecessary
     *         file system operations.
     */
    internal fun write(
        baseDir: Path,
        charset: Charset = Charsets.UTF_8,
        forceWrite: Boolean = false
    ) {
        if (changed || forceWrite) {
            val targetPath = baseDir / relativePath
            targetPath.toFile()
                .parentFile
                .mkdirs()
            targetPath.writeText(code, charset, WRITE, TRUNCATE_EXISTING, CREATE)
        }
    }

    /**
     * Deletes this source file from the file system.
     *
     * It may be the case that the file is read from one directory (source) and
     * changed in another directory (target). Thus, the initial path from where
     * the file is read may not coincide with the path from where the file is deleted.
     *
     * @param rootDir
     *         the root directory where the file lies; the [relativePath] is resolved
     *         upon this directory.
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
        return text().lines()
    }

    /**
     * Obtains the text content of this source file.
     */
    public fun text(): Text {
        initializeCode()
        return text(code)
    }

    /**
     * Adds an action to be executed before obtaining the [text] of this source file.
     */
    internal fun beforeRead(action: (SourceFile) -> Unit) {
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
