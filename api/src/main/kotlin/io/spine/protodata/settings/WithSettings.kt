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

package io.spine.protodata.settings

/**
 * A ProtoData plugin component which can [load][loadSettings] its settings.
 *
 * It is the API user's responsibility to know the format of the settings and provide
 * an appropriate class passed as the argument to the [loadSettings] method.
 *
 * For Protobuf messages, encoded either in binary or in the Protobuf JSON format, the class
 * passed as the parameter to the [loadSettings] method must be a subtype of
 * [com.google.protobuf.Message] and must be able to deserialize from the given binary/JSON.
 *
 * For JSON/YAML configuration, we use [Jackson](https://github.com/FasterXML/jackson) to
 * deserialize values. Users may use Jackson's API, such as annotations and modules,
 * to define classes to represent the configuration. Modules will be included automatically via
 * classpath scanning.
 *
 * In Kotlin, the simplest way to define a type compatible with a configuration is a data class.
 * Jackson is capable of working with Kotlin `val`-s, so the data class can be immutable.
 * In Java, Jackson is capable of working with immutable types as well.
 * However, it may require some annotations to be added to the class.
 * Please see the Jackson's documentation for more info.
 */
public interface WithSettings {

    /**
     * Obtains the settings as an instance of the given class.
     *
     * @throws IllegalStateException if no settings are available.
     */
    public fun <T: Any> loadSettings(cls: Class<T>): T

    /**
     * Checks if settings were supplied.
     *
     * @return `true` if the caller provided the settings, `false` otherwise
     */
    public fun settingsAvailable(): Boolean
}

/**
 * Obtains the settings provided by the user as an instance of the given class.
 *
 * This is the Kotlin-specific convenience API.
 * See [the general API][WithSettings.loadSettings] for details.
 */
public inline fun <reified T: Any> WithSettings.loadSettings(): T =
    loadSettings(T::class.java)
