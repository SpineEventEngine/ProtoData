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

package io.spine.protodata.plugin

import io.spine.annotation.Internal
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.type.TypeSystem
import io.spine.server.BoundedContextBuilder
import io.spine.server.entity.Entity
import kotlin.reflect.KClass

/**
 * A plugin into the code generation process.
 *
 * ProtoData uses the reactive approach to handling Protobuf source info.
 * We handle events which describe a Protobuf source
 * set via a set of [views][View] and [policies][Policy].
 *
 * Users may want to define bespoke [views][View] and [policies][Policy] based
 * on the Protobuf compiler events.
 * To do so, define your handlers and events and expose the components via
 * [Plugin.viewRepositories], [Plugin.views], and [Plugin.policies] properties.
 *
 * Implementing classes must provide a parameterless constructor so that
 * ProtoData can instantiate a plugin via its fully qualified class name.
 *
 * @property renderers The [renderers][Renderer] added by this plugin.
 *   The renderers are guaranteed to be called in the order of their declaration in the plugin.
 *
 * @property views The [views][View] added by this plugin represented via their classes.
 *   A [View] may require a repository to route events.
 *   In such a case, please use [Plugin.viewRepositories] instead.
 *
 * @property viewRepositories The [views][View] added by this plugin represented via their
 *   [repositories][ViewRepository].
 *   If passing events to a [View] does not require custom routing,
 *   the view may not have a need for repository.
 *   In such a case, please use [Plugin.views] instead.
 *
 * @property policies The [policies][Policy] added by this plugin.
 */
public abstract class Plugin(
    public val renderers: List<Renderer<*>> = listOf(),
    public val views: Set<Class<out View<*, *, *>>> = setOf(),
    public val viewRepositories: Set<ViewRepository<*, *, *>> = setOf(),
    public val policies: Set<Policy<*>> = setOf(),
) {
    /**
     * Extends the given bounded context being built with additional functionality.
     *
     * This callback is invoked after all the views, policies, and view repositories are
     * added to the context. The primary purpose of this method is to allow extending classes
     * to add additional components to the context.
     *
     * For example, a plugin may add a custom
     * [ProcessManager][io.spine.server.procman.ProcessManager] which
     * [reacts][io.spine.server.event.React] on `FileEntered` and `FileExited` events
     * to perform actions on the whole content of a proto file.
     *
     * @param context The `BoundedContextBuilder` to extend.
     */
    public open fun extend(context: BoundedContextBuilder) {
        // No-op by default.
    }
}

/**
 * Applies the receiver plugin to the given bounded context.
 *
 * The function populates the [context] with the views, repositories, and other features
 * that the plugin needs for the code generation it provides.
 *
 * This function verifies that the plugin does not expose the same view via both [Plugin.views]
 * and [Plugin.viewRepositories] methods. If such duplication is detected, the function throws
 * [ConfigurationError] exception.
 *
 * If no duplication is detected, the function adds the plugin's views and repositories to
 * the context being built. Then, it adds the plugin's policies to the context. Finally, it
 * calls [Plugin.extend] method to allow the plugin to add additional components to the context.
 */
@Internal
public fun Plugin.applyTo(context: BoundedContextBuilder, typeSystem: TypeSystem) {
    val repos = viewRepositories.toMutableList()
    val defaultRepos = views.map { ViewRepository.default(it) }
    repos.addAll(defaultRepos)
    checkNoViewRepoDuplication(repos)
    repos.forEach(context::add)
    policies.forEach {
        context.addEventDispatcher(it)
        it.use(typeSystem)
    }
    extend(context)
}

/**
 * Renders source code via this Plugin's [Renderer]s.
 *
 * The renderers are guaranteed to be called in the order of their declaration in the plugin.
 */
@Internal
public fun Plugin.render(
    codegenContext: CodegenContext,
    sources: Iterable<SourceFileSet>
) {
    renderers.forEach { r ->
        r.registerWith(codegenContext)
        sources.forEach(r::renderSources)
    }
}

private fun Plugin.checkNoViewRepoDuplication(repos: MutableList<ViewRepository<*, *, *>>) {
    val repeatedView = repos.map { it.entityClass() }
        .groupingBy { it }
        .eachCount()
        .filter { it.value > 1 }
        .keys
        .firstOrNull()
    if (repeatedView != null) {
        val pluginClass = this::class.qualifiedName
        throw ConfigurationError(
            "The plugin `$pluginClass` exposes the `${repeatedView.name}` class" +
                    " via the `views()` method and via " +
                    " the `${ViewRepository::class.qualifiedName}` returned by" +
                    " the `viewRepositories()` method." +
                    " Please submit either a repository OR a class of the view."
        )
    }
}

/**
 * Adds the specified view class to this `MutableSet` which represents a set of views
 * exposed by a `Plugin`.
 *
 * Usage scenario:
 * ```kotlin
 * override fun views(): Set<Class<out View<*, *, *>>> = buildSet {
 *     add(MyView::class)
 * }
 * ```
 */
public fun MutableSet<Class<out View<*,  *, *>>>.add(view: KClass<out View<*, *, *>>) {
    add(view.java)
}

/**
 * Adds specified entity class to this `BoundedContextBuilder`.
 *
 * A default repository instance will be created for this class.
 * This instance will be added to the repository registration list for
 * the bounded context being built.
 *
 * @param I the type of entity identifiers.
 * @param E the type of entities.
 */
public inline fun <reified I, reified E : Entity<I, *>>
        BoundedContextBuilder.add(entity: KClass<out E>) {
    add(entity.java)
}
