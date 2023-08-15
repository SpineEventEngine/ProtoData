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
import io.spine.protodata.TypeName
import io.spine.protodata.renderer.SourceFileSet.Companion.from
import io.spine.protodata.type.TypeConvention
import io.spine.protodata.type.TypeNameElement
import io.spine.server.query.Querying
import io.spine.string.ti
import io.spine.util.theOnly
import java.nio.charset.Charset
import java.nio.file.Files.walk
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.text.Charsets.UTF_8
import io.spine.tools.code.Language

/**
 * A mutable set of source files that participate in code generation workflow.
 *
 * The initial set of [files] is obtained when the source set is [loaded][from]
 * the [inputRoot] directory.
 *
 * The code generation process may [add new files][createFile] to the set, or
 * delete existing ones.
 *
 * The resulting files are written to the [outputRoot] directory.
 *
 * When files are [deleted][delete], they still remain in the set,
 * but are marked as deleted. Actual deletion happens when
 * the set is [written][write].
 *
 * The source set can be configured to perform some [actions][prepareCode]
 * before reading the files.
 *
 * @see SourceFile
 */
@Suppress("TooManyFunctions") // All part of the public API.
public class SourceFileSet
internal constructor(
    files: Set<SourceFile>,

    /**
     * A common root directory for all the files in this source set.
     *
     * Paths of the files must be either absolute or relative to this directory.
     *
     * @see from
     * @see outputRoot
     */
    @get:JvmName("inputRoot")
    public val inputRoot: Path,

    /**
     * A directory where the source set should be placed after code generation.
     *
     * @see from
     * @see inputRoot
     */
    @get:JvmName("outputRoot")
    public val outputRoot: Path
) : Iterable<SourceFile> {

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
         * Collects a source set from the given [input][inputRoot], assigning
         * the [output][outputRoot].
         *
         * @param inputRoot
         *         the directory from which to read the source files.
         * @param outputRoot
         *         the directory to which to write the processed files.
         *         If same as the [sourceRoot], all files **will be overwritten**.
         *         If different from the `sourceRoot`, the files in `sourceRoot`
         *         will not be changed.
         */
        public fun from(inputRoot: Path, outputRoot: Path): SourceFileSet {
            val source = inputRoot.canonical()
            val target = outputRoot.canonical()
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
         * Creates an empty source set which can be appended with new files and
         * written to the given target directory.
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
    public val isEmpty: Boolean
        get() = this.files.isEmpty()

    /**
     * Obtains the number of files in this set.
     */
    public val size: Int
        get() = this.files.size

    /**
     * Looks up a file by its path and throws an `IllegalArgumentException` if not found.
     *
     * The [path] may be absolute or relative to the source root.
     */
    public fun file(path: Path): SourceFile {
        val found = find(path)
        require(found != null) {
            """
            File not found: `$path`.
            Input root: `$inputRoot`.
            Output root: `$outputRoot`."
            """.ti()
        }
        return found
    }

    /**
     * Looks up a file by its path.
     *
     * The [path] may be absolute or relative to the source root.
     *
     * @return the source file or `null` if the file is missing from this set.
     */
    public fun find(path: Path): SourceFile? {
        val file = files[path]
        if (file != null) {
            return file
        }
        val filtered = files.filterKeys { path.endsWith(it) }
        return if (filtered.isEmpty()) null else filtered.entries.theOnly().value
    }

    /**
     * Same as [find], but returns an `Optional` instead of `null` for
     * convenience in Java code.
     *
     * @see find
     */
    public fun findFile(path: Path): Optional<SourceFile> =
        Optional.ofNullable(find(path))

    /**
     * Starts a file lookup for the given type name.
     */
    public fun fileFor(typeName: TypeName): FileLookup = FileLookup(this, typeName)

    public fun createFileFor(typeName: TypeName): FileCreation = FileCreation(this, typeName)

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
     * Does not delete the file from the file system. All the FS operations are
     * performed in the [write] method.
     */
    internal fun delete(file: Path) {
        val sourceFile = file(file)
        files.remove(sourceFile.relativePath)
        deletedFiles.add(sourceFile)
    }

    /**
     * Writes this source set to the file system.
     *
     * The sources existing on the file system at the moment are deleted,
     * along with the whole directory structure and the new files are written.
     */
    public fun write(charset: Charset = UTF_8) {
        deletedFiles.forEach {
            it.rm(rootDir = outputRoot)
        }
        outputRoot.toFile().mkdirs()
        val forceWriteFiles = inputRoot != outputRoot
        files.values.forEach {
            it.write(outputRoot, charset, forceWriteFiles)
        }
    }

    /**
     * Applies the given [action] to all the code files which are accessed by a [Renderer].
     *
     * When a file's code is first accessed, runs the given action.
     * The action may change the code if necessary, for example,
     * by adding insertion points.
     */
    internal fun prepareCode(action: (SourceFile) -> Unit) {
        files.values.forEach {
            it.whenRead(action)
        }
        preReadActions.add(action)
    }

    /**
     * Merges the [other] source set into this one.
     */
    internal fun mergeBack(other: SourceFileSet) {
        files.putAll(other.files)
        deletedFiles.addAll(other.deletedFiles)
        other.deletedFiles.forEach {
            files.remove(it.relativePath)
        }
    }

    /**
     * Initializes this set with a [Querying] instance for performing internal checks.
     */
    internal fun prepareForQueries(querying: Querying) {
        this.querying = querying
    }

    override fun iterator(): Iterator<SourceFile> =
        files.values.iterator()

    /**
     * Returns a comma-separated list of all the files in this set.
     */
    override fun toString(): String = toList().joinToString()
}

/**
 * Creates a subset of this source set which contains only the files
 * matching the given [predicate].
 */
internal fun SourceFileSet.subsetWhere(predicate: (SourceFile) -> Boolean) =
    SourceFileSet(this.filter(predicate).toSet(), inputRoot, outputRoot)

/**
 * Obtains absolute [normalized][normalize] version of this path.
 */
private fun Path.canonical(): Path {
    return toAbsolutePath().normalize()
}

/**
 * Ensures that the target directory is empty.
 *
 * Throws an exception if the given path is a directory, which exists and is not empty.
 * Or, if the path exists, but is not a directory.
 */
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

/**
 * A marker interface for fluent API operation classes for creating or searching for a file.
 */
public sealed interface FileOperation

/**
 * Part of the fluent API for finding source files.
 */
public class FileLookup(
    private val sources: SourceFileSet,
    private val name: TypeName
) : FileOperation {

    /**
     * Searches for a source file with for the given Proto type generated according to
     * the given [convention].
     */
    public fun <L : Language, N : TypeNameElement<L>> namedUsing(
        convention: TypeConvention<L, N>
    ): SourceFile? {
        val declaration = convention.declarationFor(name)
        val path = declaration?.path
        return path?.let { sources.find(it) }
    }
}

/**
 * Part of the fluent API for creating new source files.
 */
public class FileCreation(
    private val sources: SourceFileSet,
    private val name: TypeName
) : FileOperation {

    /**
     * Attempts to create a file path for the given type name using the given [convention].
     *
     * If the convention does not define a declaration for the given type, returns `null`.
     */
    public fun <L : Language, N : TypeNameElement<L>> namedUsing(
        convention: TypeConvention<L, N>
    ): FileCreationWithPath? {
        val declaration = convention.declarationFor(name)
        val path = declaration?.path
        return path?.let { FileCreationWithPath(sources, path) }
    }
}

/**
 * Part of the fluent API for creating new source files.
 */
public class FileCreationWithPath(
    private val sources: SourceFileSet,
    private val file: Path
) : FileOperation {

    /**
     * Writes the given [code] into the provided file.
     *
     * @return the new source file
     */
    public fun withCode(code: String): SourceFile = sources.createFile(file, code)
}
