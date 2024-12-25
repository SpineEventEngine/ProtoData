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

package io.spine.protodata.ast

import io.spine.protodata.util.Format
import io.spine.protodata.util.extensions
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

/**
 * Converts the given path to a [File] message containing an absolute version of this path.
 */
public fun Path.toProto(): File = file {
    path = absolutePathString()
}

/**
 * Converts this path to a [Directory] message.
 */
public fun Path.toDirectory(): Directory = toFile().toDirectory()

/**
 * Converts this instance of [java.io.File] to a [File] message with an absolute path.
 */
public fun java.io.File.toProto(): File = toPath().toProto()

/**
 * Converts this instance of [java.io.File] to a [Directory] message.
 */
public fun java.io.File.toDirectory(): Directory =
    directory { this@directory.path = this@toDirectory.path }

/**
 * Converts the given [File] message to a [Path].
 */
public fun File.toPath(): Path = Path(path)

/**
 * The suffix of `pb.json` files including the leading dot.
 */
private val PB_JSON_SUFFIX = ".${Format.PROTO_JSON.extensions[0]}"

/**
 * Returns the name of this file without an extension.
 *
 * Takes care of the special case for "pb.json" quasi-extension.
 *
 * @see File.name
 */
public val File.nameWithoutExtension: String
    get() = if (path.endsWith(PB_JSON_SUFFIX))
        Path(path.removeSuffix(PB_JSON_SUFFIX)).name
    else
        Path(path).nameWithoutExtension

/**
 * Obtains the last component of the file path, which includes
 * the name and extension of this file.
 *
 * @see File.nameWithoutExtension
 */
public val File.name: String
    get() = toPath().fileName.toString()
