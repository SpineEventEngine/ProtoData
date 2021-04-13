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

import com.google.common.collect.ImmutableSet.toImmutableSet
import io.spine.protodata.theOnly
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.text.Charsets.UTF_8

/**
 * A set of source files.
 */
public class SourceSet
private constructor(files: Set<SourceFile>, internal val rootDir: Path)
    : Iterable<SourceFile> by files {

    private val files: MutableMap<Path, SourceFile>

    init {
        val map = HashMap<Path, SourceFile>(files.size)
        this.files = files.associateByTo(map) { it.path }
        this.files.values.forEach { it.attachTo(this) }
    }

    public companion object {

        /**
         * Collects a source set from a given root directory.
         */
        @JvmStatic public fun fromContentsOf(directory: Path): SourceSet {
            val files = Files
                .walk(directory)
                .filter { it.isRegularFile() }
                .map { SourceFile.read(it) }
                .collect(toImmutableSet())
            return SourceSet(files, directory)
        }
    }

    /**
     * Looks up a file by its path.
     *
     * The [path] may be a relative or an absolute path the file.
     */
    public fun file(path: Path): SourceFile {
        val file = files[path]
        if (file != null) {
            return file
        }
        val filtered = files.filterKeys { it.endsWith(path) }
        if (filtered.isEmpty()) {
            throw IllegalArgumentException("File not found: `$path`.")
        }
        return filtered.values.theOnly()
    }

    /**
     * Creates a new source file at the given [path] and contains the given [code].
     */
    public fun createFile(path: Path, code: String): SourceFile {
        val file = SourceFile.fromCode(path, code)
        files[file.path] = file
        file.attachTo(this)
        return file
    }

    /**
     * Delete the given [file] from the source set.
     *
     * Does not delete the file from the file system. All the FS operations are performed in
     * the [write] method.
     */
    internal fun delete(file: Path) {
        val value = files.remove(file)
        if (value == null) {
            throw IllegalStateException("File `$value` not found.")
        }
    }

    /**
     * Writes this source set to the file system.
     *
     * The sources existing on the file system at the moment are deleted, along with the whole
     * directory structure and the new files are written.
     */
    internal fun write(charset: Charset = UTF_8) {
        val rootDirFile = rootDir.toFile()
        if (!rootDirFile.deleteRecursively()) {
            throw IllegalStateException("Unable to delete `$rootDirFile`.")
        }
        rootDirFile.mkdirs()
        files.values.forEach {
            it.write(charset, rootDir)
        }
    }
}
