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

package io.spine.protodata.config

/**
 * A ProtoData component which obtains the configuration provided by the user.
 */
public interface Configured {

    /**
     * Obtains the configuration provided by the user as an instance of the given class.
     *
     * It is the API user's responsibility to know the format of the configuration and provide
     * an appropriate class.
     *
     * For Protobuf messages, encoded either in binary or in the Protobuf JSON format, the [cls]
     * must be a subtype of [com.google.protobuf.Message] and must be able to deserialize from
     * the given binary/JSON.
     *
     * For JSON/YAML configuration, we use [Jackson](https://github.com/FasterXML/jackson) to
     * deserialize values. Users may use Jackson's API, such as annotations and modules to define
     * classes to represent the configuration. Modules will be included automatically via classpath
     * scanning.
     *
     * In Kotlin, the simplest way to define a type compatible with a configuration is a data class.
     * Jackson is capable of working with Kotlin `val`-s, so the data class can be immutable.
     * In Java, Jackson is capable of working with immutable types as well, however it may require
     * some annotations to be added to the class. See the Jackson doc for more info.
     *
     * @throws io.spine.protodata.ConfigurationError if no configuration is provided to ProtoData
     */
    public fun <T> configAs(cls: Class<T>): T
}

/**
 * Obtains the configuration provided by the user as an instance of the given class.
 *
 * This is Kotlin-specific convenience API. See [the general API][Configured.configAs] for more.
 */
public inline fun <reified T> Configured.configAs(): T =
    configAs(T::class.java)
