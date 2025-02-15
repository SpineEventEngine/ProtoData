/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.protodata.backend

import com.google.protobuf.Descriptors.GenericDescriptor

/**
 * The predicate to accept the descriptors of interest.
 *
 * Filtering descriptors is a test feature which allows accepting only a portion of
 * stub types when running code generation tests.
 *
 * The filtering is applied only to top-level declarations.
 * The filtering is not applied to nested message or enum types to preserve the integrity.
 *
 * ## API note
 *
 * Even though the input type of the predicate is [GenericDescriptor], which is a supertype of
 * all the descriptor classes in Protobuf, filtering is supported only for the following types:
 *  * [FileDescriptor][com.google.protobuf.Descriptors.FileDescriptor]
 *  * [Descriptor][com.google.protobuf.Descriptors.Descriptor]
 *  * [EnumDescriptor][com.google.protobuf.Descriptors.EnumDescriptor]
 *  * [ServiceDescriptor][com.google.protobuf.Descriptors.ServiceDescriptor]
 *
 * Since it is not possible to group the above classes in a type-safe manner, we have to
 * use the common supertype.
 */
public typealias DescriptorFilter = (GenericDescriptor) -> Boolean
