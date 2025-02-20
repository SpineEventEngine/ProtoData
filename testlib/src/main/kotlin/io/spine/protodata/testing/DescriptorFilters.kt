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

package io.spine.protodata.testing

import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import io.spine.protodata.backend.DescriptorFilter

/**
 * Creates a predicate accepting only the given [descriptor] of
 * a Protobuf declaration and the descriptor of the file in
 * which declaration was made, so that a [Pipeline][io.spine.protodata.backend.Pipeline]
 * can get down to the [descriptor] of interest.
 *
 * If the given [descriptor] is [FileDescriptor] the predicate accepts
 * the file itself, all the declarations made in this file.
 */
public fun acceptingOnly(descriptor: GenericDescriptor): DescriptorFilter {
    return if (descriptor is FileDescriptor) {
        /* The descriptor tested by the predicate */ d ->
        if (d is FileDescriptor) {
            descriptor.fullName == d.fullName
        } else {
            descriptor.fullName == d.file.fullName
        }
    } else { /* `descriptor` of interest is a message, an enum, or a service. */
        /* The descriptor tested by the predicate */ d ->
        if (d is FileDescriptor) {
            descriptor.file.fullName == d.fullName
        } else {
            descriptor.fullName == d.fullName
        }
    }
}

/**
 * Creates a predicate that accepts descriptors that are not present in the given list.
 */
public fun excluding(excludedDescriptors: List<GenericDescriptor>): DescriptorFilter = {
    excludedDescriptors.find { d -> d.fullName == it.fullName } == null
}
