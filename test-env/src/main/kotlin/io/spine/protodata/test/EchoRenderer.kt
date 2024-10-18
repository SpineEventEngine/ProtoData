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

package io.spine.protodata.test

import com.google.protobuf.StringValue
import io.spine.protobuf.AnyPacker
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.loadSettings
import io.spine.time.toInstant
import io.spine.tools.code.AnyLanguage
import kotlin.io.path.Path

public const val ECHO_FILE: String = "name.txt"

/**
 * Abstract base for stub renders that need to be added to a stub pipeline in tests.
 */
public abstract class StubSoloRenderer : Renderer<AnyLanguage>(AnyLanguage)

/**
 * A renderer that writes the contents of its Java-class-style configuration into a file.
 */
public class EchoRenderer : StubSoloRenderer() {

    override fun render(sources: SourceFileSet) {
        val name = loadSettings<Name>()
        sources.createFile(Path(ECHO_FILE), name.value)
    }
}

/**
 * The plugin which exposes [EchoRenderer] as the only render.
 */
public class EchoRendererPlugin : Plugin(renderers = listOf(EchoRenderer()))

/**
 * A renderer that writes the contents of its Protobuf-style configuration into a file.
 */
public class ProtoEchoRenderer : StubSoloRenderer() {

    override fun render(sources: SourceFileSet) {
        val echo = loadSettings<Echo>()
        val message = buildString {
            with(echo) {
                append(`when`.toInstant())
                append(':')
                val arg = AnyPacker.unpack(arg, StringValue::class.java)
                val formatted = message.format(arg.value)
                append(formatted)
                append(':')
                append(extraMessage.value)
            }
        }
        sources.createFile(Path(ECHO_FILE), message)
    }
}

public class ProtoEchoRendererPlugin : Plugin(
    renderers = listOf(ProtoEchoRenderer()),
)

/**
 * A renderer that writes the contents of its plain string configuration into a file.
 */
public class PlainStringRenderer : StubSoloRenderer() {

    override fun render(sources: SourceFileSet) {
        val echo = loadSettings<String>()
        sources.createFile(Path(ECHO_FILE), echo)
    }
}

/**
 * The plugin which exposes only [PlainStringRenderer].
 */
public class PlainStringRendererPlugin : Plugin(
    renderers = listOf(PlainStringRenderer())
)

/**
 * A wrapper type for a ProtoData configuration.
 */
public data class Name(public val value: String)
