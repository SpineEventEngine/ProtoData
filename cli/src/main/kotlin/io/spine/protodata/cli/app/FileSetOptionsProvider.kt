/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.cli.app

import com.google.protobuf.ExtensionRegistry
import io.spine.code.proto.FileSet
import io.spine.option.OptionsProvider
import io.spine.protobuf.outerClass
import io.spine.protobuf.registerAllExtensions

/**
 * An [OptionsProvider] which provides all the options defined in a set of Protobuf files.
 */
internal class FileSetOptionsProvider(
    private val files: FileSet
) : OptionsProvider {

    /**
     * Supplies the given [registry] with the options from the associated descriptors
     * of the proto files.
     *
     * The outer classes for the files must be present in the classpath. Files whose outer classes
     * are not found will be ignored.
     */
    override fun registerIn(registry: ExtensionRegistry) {
        files.files()
            .filter { it.extensions.isNotEmpty() }
            .filter { it.outerClass != null }
            .forEach { it.registerAllExtensions(registry) }
    }
}
