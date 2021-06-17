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

package io.spine.protodata.cli

import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.ExtensionRegistry
import io.spine.protodata.camelCase
import io.spine.protodata.option.OptionsProvider

/**
 * An [OptionsProvider] which provides all the options defined in a single Protobuf file.
 */
public class FileOptionsProvider(
    private val descriptor: FileDescriptor
) : OptionsProvider {

    override fun dumpTo(registry: ExtensionRegistry) {
        val outerClassName = outerClassName()
        val classLoader = this.javaClass.classLoader
        val optionsClass = classLoader.loadClass(outerClassName)
        val method = optionsClass.getDeclaredMethod(
            "registerAllExtensions", ExtensionRegistry::class.java
        )
        method.invoke(null, registry)
    }

    private fun outerClassName(): String {
        var simpleClassName = descriptor.options.javaOuterClassname
        if (simpleClassName.isEmpty()) {
            val name = descriptor.name
            val startIndex = name.lastIndexOf('/') + 1
            val endIndex = name.lastIndexOf('.')
            simpleClassName = name.substring(startIndex, endIndex).camelCase()
        }
        var javaPackage = descriptor.options.javaPackage
        if (javaPackage.isEmpty()) {
            javaPackage = descriptor.`package`
        }
        return "$javaPackage.$simpleClassName"
    }
}
