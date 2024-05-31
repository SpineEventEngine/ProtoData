/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:JvmName("Plugin")

package io.spine.protodata.protoc

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.spine.io.replaceExtension
import io.spine.type.toJson
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.util.Base64
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.text.Charsets.UTF_8

/**
 * Stores received `CodeGeneratorRequest` message to the file the name of which is passed as
 * the value of the [parameter][CodeGeneratorRequest.getParameter] property of the request.
 *
 * The name of the file is [Base64] encoded.
 *
 * The function returns empty [CodeGeneratorRequest] written to [System.out]
 * according to the `protoc` plugin
 * [protocol](https://protobuf.dev/reference/cpp/api-docs/google.protobuf.compiler.plugin.pb/).
 */
public fun main() {
    val request = CodeGeneratorRequest.parseFrom(System.`in`)
    val requestFile = Path(request.parameter.decodeBase64())

    val targetDir = requestFile.toFile().parentFile
    targetDir.mkdirs()

    requestFile.writeBytes(request.toByteArray(), CREATE, TRUNCATE_EXISTING)

    val requestFileInJson = requestFile.replaceExtension("pb.json")
    val json = request.toJson()
    requestFileInJson.writeText(json)

    val emptyResponse = CodeGeneratorResponse.getDefaultInstance()
    emptyResponse.writeTo(System.out)
}

/**
 * Decodes a UTF-8 string encoded in Base64 in this string.
 */
private fun String.decodeBase64(): String {
    val bytes = Base64.getDecoder().decode(this)
    return String(bytes, UTF_8)
}
