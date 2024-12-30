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

package io.spine.protodata.testing

import com.google.protobuf.AnyProto
import com.google.protobuf.ApiProto
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DurationProto
import com.google.protobuf.EmptyProto
import com.google.protobuf.FieldMaskProto
import com.google.protobuf.SourceContextProto
import com.google.protobuf.StructProto
import com.google.protobuf.TimestampProto
import com.google.protobuf.TypeProto
import com.google.protobuf.WrappersProto
import io.spine.option.OptionsProto
import io.spine.time.validation.TimeOptionsProto

/**
 * Obtains the list of file descriptors for the Google Protobuf library.
 *
 * The function is meant to be used for building
 * [CodeGeneratorRequest][com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest]
 * messages for tests:
 * ```kotlin
 * val request = codeGeneratorRequest {
 *   protoFile.addAll(
 *      myCustomProtos
 *      spineOptionProtos() +
 *      googleProtobufProtos() +
 *   )
 * }
 * ```
 * @see spineOptionProtos
 */
public fun googleProtobufProtos(): List<FileDescriptorProto> = listOf(
    AnyProto.getDescriptor(),
    ApiProto.getDescriptor(),
    DescriptorProtos.getDescriptor(),
    DurationProto.getDescriptor(),
    EmptyProto.getDescriptor(),
    FieldMaskProto.getDescriptor(),
    SourceContextProto.getDescriptor(),
    StructProto.getDescriptor(),
    TimestampProto.getDescriptor(),
    TypeProto.getDescriptor(),
    WrappersProto.getDescriptor(),
).map { it.toProto() }

/**
 * Obtains the list of file descriptors for files declaring custom options
 * introduced by Spine SDK.
 *
 * The function is meant to be used for building
 * [CodeGeneratorRequest][com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest]
 * messages for tests:
 * ```kotlin
 * val request = codeGeneratorRequest {
 *   protoFile.addAll(
 *      myCustomProtos +
 *      spineOptionProtos() +
 *      googleProtobufProtos()
 *   )
 * }
 * ```
 * @see googleProtobufProtos
 */
public fun spineOptionProtos(): List<FileDescriptorProto> = listOf(
    OptionsProto.getDescriptor(),
    TimeOptionsProto.getDescriptor()
).map { it.toProto() }
