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

import io.spine.server.BoundedContextBuilder

/**
 * An plugin into the code generation process.
 *
 * ProtoData uses the reactive approach to handling Protobuf source info. We handle events which
 * describe a Protobuf source set via a set of [views][View] and [policies][Policy].
 *
 * Users may want to define bespoke [views][View] and [policies][Policy] based on the Protobuf
 * compiler events. To do so, define your handlers and events and expose the components via
 * [Plugin.viewRepositories] and [Plugin.policies] properties.
 */
public interface Plugin {

    /**
     * The [views][View] added by this plugin represented via their [repositories][ViewRepository].
     *
     * A [View] may not have a need for repository. In such case, use [Plugin.views] instead.
     */
    public val viewRepositories: Set<ViewRepository<*, *, *>>
        get() = setOf()

    /**
     * The [views][View] added by this plugin represented via their classes.
     *
     * A [View] may require a repository to route events. In such case, use
     * [Plugin.viewRepositories] instead.
     */
    public val views: Set<Class<View<*, *, *>>>
        get() = setOf()

    /**
     * The [policies][Policy] added by this plugin.
     */
    public val policies: Set<Policy>
        get() = setOf()
}

/**
 * Applies the given plugin to the receiver bounded context.
 */
internal fun BoundedContextBuilder.apply(plugin: Plugin) {
    plugin.views.forEach {
        add(ViewRepository.default(it))
    }
    plugin.viewRepositories.forEach {
        add(it)
    }
    plugin.policies.forEach {
        addEventDispatcher(it)
    }
}
