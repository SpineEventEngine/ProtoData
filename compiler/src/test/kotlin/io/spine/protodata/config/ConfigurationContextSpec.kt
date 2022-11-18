/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.config

import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import io.spine.base.EventMessage
import io.spine.protodata.context.ConfigurationContext
import io.spine.protodata.config.event.FileConfigDiscovered
import io.spine.protodata.config.event.RawConfigDiscovered
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder
import io.spine.server.event.EventDispatcher
import io.spine.server.type.EventClass
import io.spine.server.type.EventEnvelope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ConfigurationContext` should")
class ConfigurationContextSpec {

    private lateinit var context: BoundedContext
    private lateinit var subscriber: TestSubscriber

    @BeforeEach
    fun prepareReceiverContext() {
        subscriber = TestSubscriber()
        context = BoundedContextBuilder.assumingTests()
            .addEventDispatcher(subscriber)
            .build()
    }

    @AfterEach
    fun closeContext() {
        context.close()
    }

    @Test
    fun `emit file configuration event`() {
        val event = FileConfigDiscovered.newBuilder()
            .setFile(ConfigFile.newBuilder().setPath("foo/bar.bin"))
            .build()
        checkEvent(event)
    }

    @Test
    fun `emit raw configuration event`() {
        val raw = RawConfig
            .newBuilder()
            .setFormat(ConfigurationFormat.JSON)
            .setValue("{}")
            .build()
        val event = RawConfigDiscovered
            .newBuilder()
            .setConfig(raw)
            .build()
        checkEvent(event)
    }

    private fun checkEvent(event: EventMessage) {
        ConfigurationContext().use {
            it.emitted(event)
        }
        assertThat(subscriber.receivedEvents)
            .containsExactly(event)
    }
}

private class TestSubscriber : EventDispatcher {

    val receivedEvents = mutableListOf<EventMessage>()

    override fun messageClasses(): ImmutableSet<EventClass> = externalEventClasses()

    override fun dispatch(envelope: EventEnvelope) {
        receivedEvents.add(envelope.message())
    }

    override fun externalEventClasses(): ImmutableSet<EventClass> =
        EventClass.setOf(FileConfigDiscovered::class.java, RawConfigDiscovered::class.java)

    override fun domesticEventClasses(): ImmutableSet<EventClass> =
        EventClass.emptySet()
}
