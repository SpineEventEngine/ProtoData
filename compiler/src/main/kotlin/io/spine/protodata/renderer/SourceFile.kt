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
 */
public class SourceFile
private constructor(

    /**
     * The source code.
     */
    code: String,

    /**
     * The FS path to the file.
     */
    public val path: Path
) {

    private lateinit var sourceSet: SourceSet

    public var code: String = code
        internal set

    internal companion object {

        /**
         * Reads the file from the given FS location.
         */
        fun read(path: Path, charset: Charset = Charsets.UTF_8): SourceFile =
            SourceFile(path.readText(charset), path)

        /**
         * Constructs a file from source code.
         *
         * @param path the FS path for the file; the file might not exist on the file system
         * @param code the source code
         */
        fun fromCode(path: Path, code: String): SourceFile =
            SourceFile(code, path)
    }

    internal fun attachTo(sourceSet: SourceSet) {
        this.sourceSet = sourceSet
    }

    /**
     * Creates a new `SourceFile` with the same path as this one but with different content.
     *
     * **Note.** This method may overwrite the work of other [Renderer]s, as well as remove
     * the insertion points from the file. Use with caution.
     */
    public fun overwrite(newCode: String) {
        this.code = newCode
    }

    public fun at(insertionsPoint: InsertionPoint): SourceAtPoint =
        SourceAtPoint(this, insertionsPoint)

    public fun delete() {
        sourceSet.delete(path)
    }

    /**
     * Writes the source code into the file on the file system.
     */
    internal fun write(charset: Charset = Charsets.UTF_8, rootDir: Path) {
        val targetPath = rootDir / path
        targetPath.toFile()
                  .parentFile
                  .mkdirs()
        targetPath.writeText(code, charset, WRITE, TRUNCATE_EXISTING, CREATE)
    }

    override fun toString(): String = path.toString()
}

public class SourceAtPoint
internal constructor(
    private val file: SourceFile,
    private val point: InsertionPoint
) {

    public fun add(vararg lines: String, extraIndentLevel: Int = 0) {
        add(lines.toList(), extraIndentLevel)
    }

    public fun add(lines: Iterable<String>, extraIndentLevel: Int = 0) {
        val sourceLines = file.code.split(System.lineSeparator())
        val updatedLines = ArrayList(sourceLines)
        val pointMarker = point.codeLine
        val newCode = lines.linesToCode(extraIndentLevel)
        sourceLines.mapIndexed { index, line -> index to line }
                   .filter { (_, line) -> line.contains(pointMarker) }
                   .map { it.first + 1 }
                   .forEach { index -> updatedLines.add(index, newCode) }
        file.code = updatedLines.joinToString(System.lineSeparator())
    }
}

private fun Iterable<String>.linesToCode(indentLevel: Int): String =
    joinToString(System.lineSeparator()) {
        (INDENT * indentLevel) + it
    }


private operator fun String.times(count: Int): String =
    buildString(capacity = length * count) {
        for (i in 0..count) {
            append(this)
        }
    }

private const val INDENT: String = "    "
