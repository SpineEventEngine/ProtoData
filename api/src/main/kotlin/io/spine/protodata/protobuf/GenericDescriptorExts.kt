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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.type.KnownTypes
import io.spine.type.TypeUrl

/**
 * Obtains the version of this descriptor with source line information loaded from
 * the descriptor set files stored in resources.
 *
 * If resources do not have the version of this descriptor, returns [this].
 *
 * @see io.spine.type.KnownTypes
 */
internal fun GenericDescriptor.withSourceLines(): GenericDescriptor {
    require(
        this is FileDescriptor
                || this is Descriptor
                || this is EnumDescriptor
                || this is ServiceDescriptor
    ) {
        "Expecting file, message, enum, or service descriptor. Got: `$this`."
    }
    if (this is FileDescriptor) {
        return withLines()
    }
    val typeUrl = TypeUrl.ofTypeOrService(this)
    return if (KnownTypes.instance().contains(typeUrl)) {
        val typeName = typeUrl.typeName()
        typeName.genericDescriptor()
    } else {
        this
    }
}

/**
 * Obtains a file descriptor with source line info via one of the types or services
 * declared in this file.
 *
 * This is a trick we need because [KnownTypes] does not provide API for obtaining loaded
 * descriptor set files.
 */
@Suppress("ReturnCount")
private fun FileDescriptor.withLines(): FileDescriptor {
    fun List<GenericDescriptor>.fileOfFirst(): FileDescriptor =
        first().withSourceLines().file
    if (messageTypes.isNotEmpty()) {
        return messageTypes.fileOfFirst()
    }
    if (enumTypes.isNotEmpty()) {
        return enumTypes.fileOfFirst()
    }
    if (services.isNotEmpty()) {
        return services.fileOfFirst()
    }
    // This is an unlikely case of a proto file without types or services.
    return this
}
