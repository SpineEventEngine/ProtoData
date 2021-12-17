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

package io.spine.protodata.protoc

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.TimestampProto
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.Version
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class `Protobuf compiler plugin should` {

    private lateinit var requestFile: File

    private val requestFileEncoded: String
        get() {
            val path = requestFile.absolutePath
            val pathBytes = path.encodeToByteArray()
            return Base64.getEncoder().encodeToString(pathBytes)
        }

    @BeforeEach
    fun prepareFile(@TempDir dir: File) {
        requestFile = dir.resolve("request.bin")
    }

    @Test
    fun `write code generation request into the given file`() {
        checkWritesRequestToFile()
    }

    @Test
    fun `overwrite existing file`() {
        requestFile.writeText("""
               This is a placeholder designed to be longer than
               the code generation request.
        """.trimIndent().repeat(100))

        checkWritesRequestToFile()
    }

    private fun checkWritesRequestToFile() {
        val request = constructRequest()
        launchMain(request)
        val read = requestFile.readBytes()
        val restoredRequest = CodeGeneratorRequest.parseFrom(read)
        assertThat(restoredRequest)
            .isEqualTo(request)
    }

    private fun constructRequest(): CodeGeneratorRequest {
        val version = Version.newBuilder()
            .setMajor(42)
            .setMinor(314)
            .setPatch(271)
            .build()
        return CodeGeneratorRequest.newBuilder()
            .addProtoFile(TimestampProto.getDescriptor().toProto())
            .addFileToGenerate("google/protobuf/timestamp.proto")
            .setCompilerVersion(version)
            .setParameter(requestFileEncoded)
            .build()
    }

    private fun launchMain(request: CodeGeneratorRequest) {
        val requestStream = ByteArrayInputStream(request.toByteArray())
        val stdIn = System.`in`
        try {
            System.setIn(requestStream)
            main()
        } finally {
            System.setIn(stdIn)
        }
    }
}
