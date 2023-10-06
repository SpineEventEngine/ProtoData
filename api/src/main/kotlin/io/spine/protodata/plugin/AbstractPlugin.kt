/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.protodata.plugin

import io.spine.protodata.renderer.Renderer

/**
 * A default abstract implementation of the [Plugin] interface.
 *
 * This class is intended to be extended by the plugins which do not need to override
 * the methods of the [Plugin] interface. That's why the methods of this class are `final`.
 */
public abstract class AbstractPlugin(
    private val renderers: Iterable<Renderer<*>> = listOf(),
    private val views: Set<Class<out View<*, *, *>>> = setOf(),
    private val viewRepositories: Set<ViewRepository<*, *, *>> = setOf(),
    private val policies: Set<Policy<*>> = setOf(),
): Plugin {

    final override fun renderers(): List<Renderer<*>> = renderers.toList()

    final override fun viewRepositories(): Set<ViewRepository<*, *, *>> = viewRepositories

    final override fun views(): Set<Class<out View<*, *, *>>> = views

    final override fun policies(): Set<Policy<*>> = policies
}
