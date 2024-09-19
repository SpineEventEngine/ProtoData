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

package io.spine.protodata.protobuf

import com.google.protobuf.Descriptors.FileDescriptor

/**
 * Obtains direct and indirect dependencies (imports) for Protobuf [files]
 * specified by the given descriptors.
 *
 * The resulting [list][asList] contains all the imports of the [files], and files themselves.
 */
public class FileDependencies(
    private val files: Iterable<FileDescriptor>
) {
    private val encountered = mutableSetOf<FileDescriptor>()

    /**
     * Obtains dependencies as a list sorted by the number of dependencies, and
     * then by the file names.
     *
     * Files with fewer dependencies are coming earlier in the list.
     * If the number of dependencies is the same, the file, which does not depend on another,
     * comes earlier in the list.
     * If two files do not depend on each other, they are sorted alphabetically by
     * their [names][FileDescriptor.getName].
     */
    public fun asList(): List<FileDescriptor> {
        val seq = files.flatMap<FileDescriptor, FileDescriptor> {
            walkFile(it) { f -> f.dependencies + f }
        }
        try {
            val result = mutableListOf<FileDescriptor>()
            result.run {
                addAll(seq.toSet())
                sortWith(FdComparator())
                return toList()
            }
        } finally {
            encountered.clear()
        }
    }

    private fun <T> walkFile(
        file: FileDescriptor,
        extractorFun: (FileDescriptor) -> Iterable<T>
    ): Sequence<T> {
        val queue = ArrayDeque<FileDescriptor>()
        queue.add(file)
        return sequence {
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                encountered.add(current)
                yieldAll(extractorFun(current))
                current.dependencies
                    .filter { !encountered.contains(it) }
                    .forEach(queue::add)
            }
        }
    }
}

/**
 * Compares [FileDescriptor] instances by their dependencies, and then by their names.
 *
 * @see FileDependencies.asList
 */
private class FdComparator : Comparator<FileDescriptor> {

    @Suppress("ReturnCount")
    override fun compare(f1: FileDescriptor, f2: FileDescriptor): Int {
        // Sort by the number of dependencies first.
        var result = f1.importCount.compareTo(f2.importCount)
        if (result != 0) return result

        // Then pull files that imported by others earlier.
        result =
            if (f1.isImportedBy(f2)) -1
            else if (f2.isImportedBy(f1)) 1
            else 0
        if (result != 0) return result

        // Then compare by names.
        result = f1.name.compareTo(f2.name)
        return result
    }
}
