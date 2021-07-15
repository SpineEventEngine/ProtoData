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

package io.spine.protodata.test

import com.google.protobuf.StringValue
import io.spine.protobuf.AnyPacker
import io.spine.protodata.config.configAs
import io.spine.protodata.language.CommonLanguages.any
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceSet
import io.spine.protodata.tesst.Echo
import io.spine.time.toInstant
import kotlin.io.path.Path

public const val ECHO_FILE: String = "name.txt"

public class EchoRenderer : Renderer(any) {

    override fun render(sources: SourceSet) {
        val name = configAs<Name>()
        sources.createFile(Path(ECHO_FILE), name.value)
    }
}

public class ProtoEchoRenderer : Renderer(any) {

    override fun render(sources: SourceSet) {
        val echo = configAs<Echo>()
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

public class PlainStringRenderer : Renderer(any) {

    override fun render(sources: SourceSet) {
        val echo = configAs<String>()
        sources.createFile(Path(ECHO_FILE), echo)
    }
}

public data class Name(public val value: String)
