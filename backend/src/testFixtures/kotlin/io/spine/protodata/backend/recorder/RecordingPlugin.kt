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

package io.spine.protodata.backend.recorder

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.protodata.ast.event.EnumEntered
import io.spine.protodata.ast.event.ServiceEntered
import io.spine.protodata.ast.event.TypeEntered
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFileSet
import io.spine.server.entity.alter
import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.server.route.EventRouting
import io.spine.tools.code.Java
import io.spine.validate.ValidatingBuilder

/**
 * The diagnostics plugin that gathers names of message types, enum types, or services
 * processed by a pipeline.
 */
class RecordingPlugin : Plugin(
    viewRepositories = setOf(MessageView.Repo(), EnumView.Repo(), ServicesView.Repo()),
    renderers = listOf(Query())) {

    fun query() = renderers.first() as Query
}

private abstract class RecordingView<S : DeclarationViewState, B: ValidatingBuilder<S>> :
    View<String, S, B>() {

    companion object {
        @Suppress("ConstPropertyName") // for readability.
        const val singletonId: String = "SINGLETON"
    }
}

private abstract class RecordingViewRepo<S : DeclarationViewState, V : RecordingView<S, *>> :
    ViewRepository<String, V, S>() {

    override fun setupEventRouting(routing: EventRouting<String>) {
        super.setupEventRouting(routing)
        routing.unicast<TypeEntered> { e, _ -> RecordingView.singletonId }
    }
}

/**
 * A no-action renderer which serves the querying capabilities.
 */
class Query: Renderer<Java>(Java) {

    fun messageTypeNames(): List<String> = findNames<MessageTypes>()

    fun enumTypeNames(): List<String> = findNames<EnumTypes>()

    fun serviceNames(): List<String> = findNames<Services>()

    private inline fun <reified S : DeclarationViewState> findNames(): List<String> {
        val v = (this as Querying).select<S>().findById(RecordingView.singletonId)
        return v?.getNameList() ?: emptyList()
    }

    override fun render(sources: SourceFileSet) {
        // Do nothing
    }
}

private class MessageView : RecordingView<MessageTypes, MessageTypes.Builder>() {

    init {
        println("`MessageView` class created.`")
    }

    @Subscribe
    fun on(@External e: TypeEntered) = alter {
        id = singletonId
        addName(e.type.qualifiedName)
    }

    class Repo : RecordingViewRepo<MessageTypes, MessageView>()
}

private class EnumView : RecordingView<EnumTypes, EnumTypes.Builder>() {

    @Subscribe
    fun on(@External e: EnumEntered) = alter {
        id = singletonId
        addName(e.type.qualifiedName)
    }

    class Repo : RecordingViewRepo<EnumTypes, EnumView>()
}

private class ServicesView : RecordingView<Services, Services.Builder>() {

    @Subscribe
    fun on(@External e: ServiceEntered) = alter {
        id = singletonId
        addName(e.service.qualifiedName)
    }

    class Repo : RecordingViewRepo<Services, ServicesView>()
}
