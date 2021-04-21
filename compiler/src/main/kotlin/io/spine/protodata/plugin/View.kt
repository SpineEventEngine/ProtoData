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

package io.spine.protodata.plugin

import io.spine.base.EntityState
import io.spine.protobuf.ValidatingBuilder
import io.spine.server.DefaultRepository
import io.spine.server.entity.model.EntityClass
import io.spine.server.projection.Projection
import io.spine.server.projection.ProjectionRepository
import io.spine.server.projection.model.ProjectionClass
import io.spine.server.route.EventRoute
import io.spine.server.route.EventRouting
import kotlin.reflect.KClass

public open class View<I, M : EntityState, B : ValidatingBuilder<M>> : Projection<I, M, B>()

public open class ViewRepository<I, V : View<I, S, *>, S : EntityState>
    : ProjectionRepository<I, V, S>() {

    public companion object {

        @JvmStatic
        public fun <I, V : View<I, S, *>, S : EntityState> default(cls: Class<V>)
                : ViewRepository<I, V, S> =
            DefaultViewRepository(cls)


        public fun <I, V : View<I, S, *>, S : EntityState> default(cls: KClass<V>)
                : ViewRepository<I, V, S> =
            default(cls.java)
    }

    override fun setupEventRouting(routing: EventRouting<I>) {
        super.setupEventRouting(routing)
        routing.replaceDefault(EventRoute.byFirstMessageField(idClass()))
    }
}

internal class DefaultViewRepository<I, V : View<I, S, *>, S : EntityState>(
    private val cls: Class<V>
) : ViewRepository<I, V, S>(), DefaultRepository {

    override fun entityModelClass(): EntityClass<V> {
        return ProjectionClass.asProjectionClass(cls)
    }

    override fun logName(): String {
        return "${ViewRepository::class.simpleName}.default()"
    }
}
