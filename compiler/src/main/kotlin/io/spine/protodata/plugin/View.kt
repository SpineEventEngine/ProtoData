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
import io.spine.server.DefaultRepository
import io.spine.server.projection.Projection
import io.spine.server.projection.ProjectionRepository
import io.spine.server.projection.model.ProjectionClass
import io.spine.server.route.EventRoute
import io.spine.server.route.EventRouting
import io.spine.validate.ValidatingBuilder

/**
 * A view on the Protobuf sources.
 *
 * A view helps prepare and re-organize data so that code generation may easily access it.
 *
 * A state of a view is defined via Protobuf. The first field of the state must be the ID of
 * the view. Example:
 * ```protobuf
 * import "spine/options.proto";
 * import "spine/protodata/ast.proto";
 *
 * // ...
 *
 * message ShortFieldName {
 *     option (entity).kind = PROJECTION; // Shows that this is a view state.
 *
 *     spine.protodata.FieldName name = 1; // Also, the ID of the view.
 *
 *     spine.protodata.TypeName where_declared = 2;
 * }
 * ```
 *
 * The Java class representing the `ShortFieldName` message must implement [EntityState].
 * The `(entity)` option ensures that is the case.
 *
 * The state of a view is constructed based on events, which are produced either by
 * the Protobuf Compiler or by a [Policy].
 *
 * To listen to the events, define single parameter methods annotated with
 * [@Subscribe][io.spine.core.Subscribe]. In these methods, change the state of the view via
 * the `builder()`, `update { }`, or `alter { }` methods.
 *
 * We recommend to use `internal` access for the subscriber methods in Kotlin and package-private —
 * in Java. The methods are not `private`, as they are invoked by the framework, and not `public`,
 * as they must not be called directly.
 *
 * Events from the Protobuf compiler should be marked as [@External][io.spine.core.External], while
 * events from policies should not. See the whole list of Protobuf compiler events
 * in `spine/protodata/events.proto`.
 *
 * Example:
 * ```
 * class MyView : View<FieldName, ShortFieldName, ShortFieldName.Builder> {
 *
 *     @Subscribe
 *     internal fun on(event: SomethingHappened) {
 *         // Change the view via `builder()`.
 *     }
 *
 *     @Subscribe
 *     internal fun on(@External event: FieldOptionDiscovered) {
 *         // Change the view via `builder()`.
 *     }
 * }
 * ```
 *
 * Views have repositories which are responsible for storing states and for delivering correct
 * events to the correct views. See [ViewRepository] for more.
 *
 * @param I the type of the ID of the view; can be a Protobuf message, a String, an int, or a long
 * @param M the type of the view's state; must be a Protobuf message implementing [EntityState]
 * @param B the type of the view's state builder; must match `<M>`
 */
public open class View<I, M : EntityState<I>, B : ValidatingBuilder<M>> : Projection<I, M, B>()

/**
 * A repository responsible for a certain type of [View]s.
 *
 * By default, when an event is headed towards a [View], we try to match the first field of
 * the event with the ID of the view. This is called routing. If the types match, the event is
 * routed to the view with the matching ID. Otherwise, an error occurs.
 *
 * To change the rules of event routing, override the [ViewRepository.setupEventRouting] method.
 *
 * If no customization is required from a `ViewRepository`, users should prefer
 * [ViewRepository.default] to creating custom repository types.
 */
public open class ViewRepository<I, V : View<I, S, *>, S : EntityState<I>>
    : ProjectionRepository<I, V, S>() {

    internal companion object {

        @Suppress("UNCHECKED_CAST")
        fun default(cls: Class<out View<*, *, *>>): ViewRepository<*, *, *> {
            val cast = cls as Class<View<Any, EntityState<Any>, *>>
            return DefaultViewRepository(cast)
        }
    }

    override fun setupEventRouting(routing: EventRouting<I>) {
        super.setupEventRouting(routing)
        routing.replaceDefault(EventRoute.byFirstMessageField(idClass()))
    }
}

/**
 * A default [ViewRepository].
 *
 * A [ViewRepository] can be customized in case event routing must be adjusted. Otherwise, users
 * should use `DefaultViewRepository` by calling [ViewRepository.default].
 */
internal class DefaultViewRepository(
    private val cls: Class<View<Any, EntityState<Any>, *>>
) : ViewRepository<Any, View<Any, EntityState<Any>, *>, EntityState<Any>>(), DefaultRepository {

    override fun entityModelClass(): ProjectionClass<View<Any, EntityState<Any>, *>> =
        ProjectionClass.asProjectionClass(cls)

    override fun logName(): String =
        "${ViewRepository::class.simpleName}.default()"
}
