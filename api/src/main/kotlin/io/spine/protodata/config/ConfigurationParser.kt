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

package io.spine.protodata.config

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.common.io.ByteSource
import com.google.protobuf.Message
import io.spine.json.Json.fromJson
import io.spine.protobuf.Messages
import io.spine.protodata.ConfigurationError
import io.spine.protodata.config.ConfigurationFormat.JSON
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.config.ConfigurationFormat.PROTO_BINARY
import io.spine.protodata.config.ConfigurationFormat.PROTO_JSON
import io.spine.protodata.config.ConfigurationFormat.RCF_UNKNOWN
import io.spine.protodata.config.ConfigurationFormat.UNRECOGNIZED
import io.spine.protodata.config.ConfigurationFormat.YAML
import java.nio.charset.Charset.defaultCharset

/**
 * A parser for the ProtoData user-provided configuration.
 */
internal sealed interface ConfigurationParser {

    /**
     * Attempts to deserialize the given configuration value into the given class.
     */
    fun <T> parse(source: ByteSource, cls: Class<T>): T
}

/**
 * A configuration parser for Protobuf messages.
 */
private sealed class ProtobufParser : ConfigurationParser {

    final override fun <T> parse(source: ByteSource, cls: Class<T>): T {
        if (!Message::class.java.isAssignableFrom(cls)) {
            error("Expected a message class but got `${cls.canonicalName}`.")
        }
        @Suppress("UNCHECKED_CAST")
        return doParse(source, cls as Class<out Message>) as T
    }

    /**
     * Deserializes the given bytes into a message with the given class.
     */
    abstract fun doParse(source: ByteSource, cls: Class<out Message>): Message
}

/**
 * A configuration parser for Protobuf messages encoded in the Protobuf binary format.
 */
private object ProtoBinaryParser : ProtobufParser() {

    override fun doParse(source: ByteSource, cls: Class<out Message>): Message {
        val builder = Messages.builderFor(cls)
        builder.mergeFrom(source.read())
        return builder.build()
    }
}

/**
 * A configuration parser for Protobuf messages encoded in the Protobuf JSON format.
 */
private object ProtoJsonParser : ProtobufParser() {

    override fun doParse(source: ByteSource, cls: Class<out Message>): Message {
        val charSource = source.asCharSource(defaultCharset())
        val json = charSource.read()
        return fromJson(json, cls)
    }
}

/**
 * A configuration parser for text-based formats.
 */
private sealed class JacksonParser : ConfigurationParser {

    protected abstract val factory: JsonFactory

    final override fun <T> parse(source: ByteSource, cls: Class<T>): T {
        val mapper = ObjectMapper(factory).findAndRegisterModules()
        val charSource = source.asCharSource(defaultCharset())
        return charSource.openBufferedStream().use {
            mapper.readValue(it, cls)
        }
    }
}

/**
 * A configuration parser for JSON.
 */
private object JsonParser : JacksonParser() {

    private val commonJsonFactory = JsonFactory()
    override val factory: JsonFactory by this::commonJsonFactory
}

/**
 * A configuration parser for YAML.
 */
private object YamlParser : JacksonParser() {

    private val commonYamlFactory = YAMLFactory()
    override val factory: JsonFactory by this::commonYamlFactory
}

/**
 * A parser for the plain string configuration.
 *
 * Does not actually parse anything. Simply checks that the expected type of
 * the configuration is `java.lang.String`. If the type is wrong, throws a [ConfigurationError].
 */
private object PlainParser : ConfigurationParser {

    override fun <T> parse(source: ByteSource, cls: Class<T>): T {
        if (cls != String::class.java) {
            throw ConfigurationError(
                "Expected configuration of type `${cls.canonicalName}` but got a plain string."
            )
        }
        val value = source.asCharSource(defaultCharset()).read()
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}

/**
 * Obtains a [ConfigurationParser] for this format.
 *
 * @throws ConfigurationError if the format is a non-value
 */
internal val ConfigurationFormat.parser: ConfigurationParser
    get() = when(this) {
        PROTO_BINARY -> ProtoBinaryParser
        PROTO_JSON -> ProtoJsonParser
        JSON -> JsonParser
        YAML -> YamlParser
        PLAIN -> PlainParser
        UNRECOGNIZED, RCF_UNKNOWN -> throw ConfigurationError(
            "Unable to parse configuration: unknown format."
        )
    }
