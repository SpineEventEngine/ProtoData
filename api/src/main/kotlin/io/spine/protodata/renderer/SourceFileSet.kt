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

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableSet.toImmutableSet
import io.spine.annotation.Internal
import io.spine.server.query.Querying
import io.spine.util.theOnly
import java.nio.charset.Charset
import java.nio.file.Files.walk
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.text.Charsets.UTF_8

/**
 * A set of source files.
 */
public class SourceFileSet
internal constructor(
    files: Set<SourceFile>,

    /**
     * A common root directory for all the files in this source set.
     *
     * Paths of the files must be either absolute or relative to this directory.
     */
    public val sourceRoot: Path,

    /**
     * A directory where the source set should be placed after code generation.
     *
     * If same as the `sourceRoot`, all files will be overwritten.
     *
     * If different from the `sourceRoot`, the files in `sourceRoot` will not be changed.
     */
    public val targetRoot: Path
) : Iterable<SourceFile> by files {

    private val files: MutableMap<Path, SourceFile>
    private val deletedFiles = mutableSetOf<SourceFile>()
    private val preReadActions = mutableListOf<(SourceFile) -> Unit>()
    internal lateinit var querying: Querying

    init {
        val map = HashMap<Path, SourceFile>(files.size)
        this.files = files.associateByTo(map) { it.relativePath }
        this.files.values.forEach { it.attachTo(this) }
    }

    @Internal
    public companion object {

        /**
         * Collects a source set from a given root directory.
         */
        public fun from(sourceRoot: Path, targetRoot: Path): SourceFileSet {
            val source = sourceRoot.canonical()
            val target = targetRoot.canonical()
            if (source != target) {
                checkTarget(target)
            }
            val files = walk(source)
                .filter { it.isRegularFile() }
                .map { SourceFile.read(source.relativize(it), source) }
                .collect(toImmutableSet())
            return SourceFileSet(files, source, target)
        }

        /**
         * Creates an empty source set which can be appended with new files and written to
         * the given target directory.
         */
        public fun empty(target: Path): SourceFileSet {
            checkTarget(target)
            val files = setOf<SourceFile>()
            return SourceFileSet(files, target, target)
        }

        @VisibleForTesting
        public fun from(sourceAndTarget: Path): SourceFileSet =
            from(sourceAndTarget, sourceAndTarget)
    }

    /**
     * Returns `true` if this source set does not contain any files, `false` otherwise.
     */
    public val isEmpty: Boolean = files.isEmpty()

    /**
     * Obtains the number of files in this set.
     */
    public val size: Int = files.size

    /**
     * Looks up a file by its path and throws an `IllegalArgumentException` if not found.
     *
     * The [path] may be absolute or relative to the source root.
     */
    public fun file(path: Path): SourceFile =
        findFile(path).orElseThrow {
            IllegalArgumentException(
                "File not found: `$path`. Source root: `$sourceRoot`. Target root: `$targetRoot`."
            )
        }

    /**
     * Looks up a file by its path.
     *
     * The [path] may be absolute or relative to the source root.
     *
     * @return the source file or an `Optional.empty()` if the file is missing from this set.
     */
    public fun findFile(path: Path): Optional<SourceFile> {
        val file = files[path]
        if (file != null) {
            return Optional.of(file)
        }
        val filtered = files.filterKeys { path.endsWith(it) }
        return if (filtered.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(filtered.entries.theOnly().value)
        }
    }

    /**
     * Creates a new source file at the given [path] and contains the given [code].
     */
    public fun createFile(path: Path, code: String): SourceFile {
        val file = SourceFile.fromCode(path, code)
        files[file.relativePath] = file
        file.attachTo(this)
        preReadActions.forEach {
            file.whenRead(it)
        }
        return file
    }

    /**
     * Delete the given [file] from the source set.
     *
     * Does not delete the file from the file system. All the FS operations are performed in
     * the [write] method.
     */
    internal fun delete(file: Path) {
        val sourceFile = file(file)
        files.remove(sourceFile.relativePath)
        deletedFiles.add(sourceFile)
    }

    /**
     * Writes this source set to the file system.
     *
     * The sources existing on the file system at the moment are deleted, along with the whole
     * directory structure and the new files are written.
     */
    public fun write(charset: Charset = UTF_8) {
        deletedFiles.forEach {
            it.rm(rootDir = targetRoot)
        }
        targetRoot.toFile().mkdirs()
        val forceWriteFiles = sourceRoot != targetRoot
        files.values.forEach {
            it.write(targetRoot, charset, forceWriteFiles)
        }
    }

    /**
     * Applies the given [action] to all the code files which are accessed by a [Renderer].
     *
     * When a file's code is first accessed, runs the given action. The action may change the code
     * if necessary, for example, by adding insertion points.
     */
    internal fun prepareCode(action: (SourceFile) -> Unit) {
        files.values.forEach {
            it.whenRead(action)
        }
        preReadActions.add(action)
    }

    internal fun subsetWhere(predicate: (SourceFile) -> Boolean) =
        SourceFileSet(this.filter(predicate).toSet(), sourceRoot, targetRoot)

    /**
     * Merges the other source set into this one.
     */
    internal fun mergeBack(other: SourceFileSet) {
        files.putAll(other.files)
        deletedFiles.addAll(other.deletedFiles)
        other.deletedFiles.forEach {
            files.remove(it.relativePath)
        }
    }

    /**
     * Initialize this set with a [Querying] instance for performing internal checks.
     */
    internal fun prepareForQueries(querying: Querying) {
        this.querying = querying
    }

    override fun toString(): String = toList().joinToString()
}

private fun Path.canonical(): Path {
    return toAbsolutePath().normalize()
}

private fun checkTarget(targetRoot: Path) {
    if (targetRoot.exists()) {
        val target = targetRoot.toFile()
        require(target.isDirectory) {
            "Target root `$targetRoot` must be a directory."
        }
        val children = target.list()!!
        require(children.isEmpty()) {
            val nl = System.lineSeparator()
            val ls = children.joinToString(
                separator = nl,
                transform = { f -> "    $f" }
            )
            "Target directory `$targetRoot` must be empty. Found inside:$nl" +
            "${ls}."
        }
    }
}
