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

package io.spine.protodata.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.common.io.ByteSource
import com.google.common.io.Files
import com.google.protobuf.Message
import io.spine.protobuf.defaultInstance
import io.spine.protodata.util.Format.JSON
import io.spine.protodata.util.Format.PLAIN
import io.spine.protodata.util.Format.PROTO_BINARY
import io.spine.protodata.util.Format.PROTO_JSON
import io.spine.protodata.util.Format.RCF_UNKNOWN
import io.spine.protodata.util.Format.UNRECOGNIZED
import io.spine.protodata.util.Format.YAML
import io.spine.type.fromJson
import java.nio.charset.Charset.defaultCharset

/**
 * Parses the given file loading the instance of the given class.
 *
 * The format of the file is determined by the extension of the file.
 */
public fun <T : Any> parseFile(file: java.io.File, cls: Class<T>): T {
    val path = file.toPath()
    val format = formatOf(path)
    val bytes = Files.asByteSource(path.toFile())
    return format.parser.parse(bytes, cls)
}

/**
 * A parser for files in one of the supported [formats][Format].
 */
internal sealed interface Parser {

    /**
     * Attempts to deserialize the given settings value into the given class.
     */
    fun <T> parse(source: ByteSource, cls: Class<T>): T
}

/**
 * Obtains a [Parser] for this format.
 *
 * @throws IllegalStateException If the format is a non-value.
 */
private val Format.parser: Parser
    get() = when(this) {
        PROTO_BINARY -> ProtoBinaryParser
        PROTO_JSON -> ProtoJsonParser
        JSON -> JsonParser
        YAML -> YamlParser
        PLAIN -> PlainParser
        UNRECOGNIZED, RCF_UNKNOWN ->
            error("Unable to parse settings: unknown format `${this.name}`.")
    }

/**
 * Settings parser for Protobuf messages.
 */
private sealed class ProtobufParser : Parser {

    final override fun <T> parse(source: ByteSource, cls: Class<T>): T {
        require(Message::class.java.isAssignableFrom(cls)) {
            "Expected a message class but got `${cls.canonicalName}`."
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
 * Settings parser for Protobuf messages encoded in the Protobuf binary format.
 */
private object ProtoBinaryParser : ProtobufParser() {

    override fun doParse(source: ByteSource, cls: Class<out Message>): Message {
        val builder = cls.defaultInstance.toBuilder()
        builder.mergeFrom(source.read())
        return builder.build()
    }
}

/**
 * Settings parser for Protobuf messages encoded in the Protobuf JSON format.
 */
private object ProtoJsonParser : ProtobufParser() {

    override fun doParse(source: ByteSource, cls: Class<out Message>): Message {
        val charSource = source.asCharSource(defaultCharset())
        val json = charSource.read()
        return cls.fromJson(json)
    }
}

/**
 * Settings parser for text-based formats.
 */
private sealed class JacksonParser : Parser {

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
 * Settings parser for JSON.
 */
private object JsonParser : JacksonParser() {
    private val commonJsonFactory = JsonFactory()
    override val factory: JsonFactory by this::commonJsonFactory
}

/**
 * Settings parser for YAML.
 */
private object YamlParser : JacksonParser() {
    private val commonYamlFactory = YAMLFactory()
    override val factory: JsonFactory by this::commonYamlFactory
}

/**
 * A parser for the plain string settings.
 *
 * This object does not parse, but simply reads the given `source` as `java.lang.String` value.
 * To ensure the type safety, the `cls` parameter is checked to be `java.lang.String`.
 * If the type is wrong, throws a [IllegalStateException].
 */
private object PlainParser : Parser {
    override fun <T> parse(source: ByteSource, cls: Class<T>): T {
        if (cls != String::class.java) {
            error("Expected settings of type `${cls.canonicalName}` but got a plain string.")
        }
        val value = source.asCharSource(defaultCharset()).read()
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}
