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
    public val code: String,

    /**
     * The FS path to the file.
     */
    public val path: Path
) {

    public companion object {

        /**
         * Reads the file from the given FS location.
         */
        public fun read(path: Path, charset: Charset = Charsets.UTF_8): SourceFile =
            SourceFile(path.readText(charset), path)

        /**
         * Constructs a file from source code.
         *
         * @param path the FS path for the file; the file might not exist on the file system
         * @param code the source code
         */
        public fun fromCode(path: Path, code: String): SourceFile = SourceFile(code, path)
    }

    /**
     * Creates a new `SourceFile` with the same path as this one but with different content.
     */
    public fun overwrite(newCode: String): SourceFile = SourceFile(newCode, path)

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
}
