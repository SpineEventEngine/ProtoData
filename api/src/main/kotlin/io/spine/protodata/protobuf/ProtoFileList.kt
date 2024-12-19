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

import io.spine.string.Separator
import java.io.File
import java.nio.file.Path

/**
 * A list of Protobuf files compiled by `protoc`.
 *
 * The list is stored in a file passed to the constructor.
 * Each line of the file is a full name of the Protobuf file.
 *
 * @property files The references to Protobuf files compiled by `protoc`.
 */
public class ProtoFileList(public val files: List<File>) {

    public companion object {

        /**
         * Obtains a name for the file listing Protobuf files in the source set
         * with the given name.
         */
        public fun fileFor(sourceSetName: String): File =
            File("${sourceSetName}-proto-files.txt")

        /**
         * Creates the file in the given directory which lists the specified files.
         *
         * @param dir The directory to place the file. The name of the file [depends][fileFor]
         *  upon the name of the source set.
         * @param sourceSetName The name of the source set which contains the files.
         * @param files The list of files names of which are to be stored in the created file.
         */
        public fun create(dir: Path, sourceSetName: String, files: List<File>) {
            val targetFile = dir.resolve(fileFor(sourceSetName).toPath()).toFile()
            // Use the `LF` separator for compatibility with the Kotlin runtime for reading.
            targetFile.writeText(files.joinToString(Separator.LF.value))
        }

        /**
         * Loads the list from the file with the given name.
         */
        public fun load(file: File): ProtoFileList {
            val content = file.readText()
            val files = content.lines().map { line -> File(line) }
            return ProtoFileList(files)
        }
    }
}
