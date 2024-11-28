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

package io.spine.protodata.gradle.plugin

import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.GenerateProtoTask.PluginOptions
import io.spine.string.simply
import org.gradle.api.NamedDomainObjectContainer

/**
 * Obtains names of [built-ins][GenerateProtoTask.getBuiltinsForCaching] of this task.
 */
internal fun GenerateProtoTask.builtins(): List<String> = collectNames("getBuiltinsForCaching")

/**
 * Obtains names of [plugins][GenerateProtoTask.getPluginsForCaching] of this task.
 */
internal fun GenerateProtoTask.plugins(): List<String> = collectNames("getPluginsForCaching")

/**
 * Reflectively calls the method obtaining a collection of [PluginOptions].
 *
 * The methods we call reflectively are `protected` in `GenerateProtoTask`.
 * We cannot call public methods for accessing `built-ins` or `plugins` because of
 * the call to `GenerateProtoTask.checkCanConfig()` precondition check called by those methods.
 * The check prevents calling after the task is already configured.
 * There are no other means of obtaining built-ins or plugins at the time of writing.
 *
 * @see GenerateProtoTask.getBuiltins
 * @see GenerateProtoTask.getPlugins
 */
private fun GenerateProtoTask.collectNames(methodName: String): List<String> {
    val method = this::class.java.getDeclaredMethod(methodName)
    try {
        val success = method.trySetAccessible()
        if (!success) {
            val methodRef = "${simply<GenerateProtoTask>()}.$methodName"
            error("Unable to make the method `$methodRef` accessible.")
        }
        @Suppress("UNCHECKED_CAST")
        val collection = method.invoke(this) as NamedDomainObjectContainer<PluginOptions>
        return collection.map { it.name }
    } finally {
        method.isAccessible = false
    }
}
