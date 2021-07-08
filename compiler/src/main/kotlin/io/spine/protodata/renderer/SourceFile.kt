/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import java.lang.System.lineSeparator
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
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

    private lateinit var sourceSet: SourceSet
    private val preReadActions = mutableListOf<(SourceFile) -> Unit>()
    private var alreadyRead = false

    internal companion object {

        /**
         * Reads the file from the given FS location.
         */
        fun read(
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
        fun fromCode(relativePath: Path, code: String): SourceFile =
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
     */
    public fun at(insertionPoint: InsertionPoint): SourceAtPoint =
        SourceAtPoint(this, insertionPoint)

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
        sourceSet.delete(relativePath)
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
     * Injects the given [sourceSet].
     */
    internal fun attachTo(sourceSet: SourceSet) {
        this.sourceSet = sourceSet
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
    public fun code(): String {
        initializeCode()
        return code
    }

    /**
     * Obtains the entire content of this file as a list of lines.
     */
    public fun lines(): List<String> {
        return code().split(lineSeparator())
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
 * @see SourceFile.at for the start of the fluent API and the detailed description of its behaviour.
 */
public class SourceAtPoint
internal constructor(
    private val file: SourceFile,
    private val point: InsertionPoint
) {

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *      code lines
     * @param extraIndentLevel
     *      extra indentation to be added to the lines; each unit adds four spaces
     */
    @JvmOverloads
    public fun add(vararg lines: String, extraIndentLevel: Int = 0) {
        add(lines.toList(), extraIndentLevel)
    }

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *      code lines
     * @param extraIndentLevel
     *      extra indentation to be added to the lines; each unit adds four spaces
     */
    @JvmOverloads
    public fun add(lines: Iterable<String>, extraIndentLevel: Int = 0) {
        val sourceLines = file.lines()
        val updatedLines = ArrayList(sourceLines)
        val pointMarker = point.codeLine
        val newCode = lines.linesToCode(extraIndentLevel)
        sourceLines.mapIndexed { index, line -> index to line }
                   .filter { (_, line) -> line.contains(pointMarker) }
                   .map { it.first + 1 }
                   .forEach { index -> updatedLines.add(index, newCode) }
        file.updateLines(updatedLines)
    }
}

private fun Iterable<String>.linesToCode(indentLevel: Int): String =
    joinToString(lineSeparator()) {
        INDENT.repeat(indentLevel) + it
    }

private const val INDENT: String = "    "
