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

package io.spine.protodata.renderer

import io.spine.protodata.subscriber.CodeEnhancement
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility

public interface Renderer {

    public var enhancements: List<CodeEnhancement>

    public fun render(sources: SourceSet): SourceSet
}

public class RendererBuilder
private constructor() {

    private val enhancements: MutableList<CodeEnhancement> = mutableListOf()

    public infix fun and(enhancement: CodeEnhancement) : RendererBuilder {
        enhancements.add(enhancement)
        return this
    }

    public infix fun and(newEnhancements: Iterable<CodeEnhancement>) : RendererBuilder {
        enhancements.addAll(newEnhancements)
        return this
    }

    public fun <R: Renderer> create(cls: KClass<R>): R {
        val ctor = cls.constructors.find {
            it.visibility == KVisibility.PUBLIC && it.parameters.isEmpty()
        } ?: throw IllegalStateException(
            "Renderer `${cls.qualifiedName} should have a public zero-parameter constructor.`"
        )
        val renderer = ctor.call()
        renderer.enhancements = enhancements.toList()
        return renderer
    }
}
