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

package io.spine.protodata.subscriber

import com.google.common.collect.ImmutableSet
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.core.UserId
import io.spine.protodata.QueryBuilder
import io.spine.server.BoundedContext
import io.spine.server.event.EventDispatcher
import io.spine.server.type.EventClass
import io.spine.server.type.EventEnvelope

/**
 * A subscriber to a compiler event.
 *
 * Instances of `Subscriber`s are created via reflection. It is required that the concrete classes
 * have a `public` no-argument constructor.
 */
public abstract class Subscriber<E : EventMessage>(

    /**
     * The class of the event to which to subscribe.
     */
    private val eventClass: Class<E>
) : EventDispatcher {

    private val actor = UserId
        .newBuilder()
        .setValue(this.javaClass.name)
        .build()

    private val enhancements: MutableList<CodeEnhancement> = mutableListOf()

    /**
     * Obtains the [CodeEnhancement]s produced by this subscriber so far.
     */
    internal val producedEnhancements: List<CodeEnhancement>
        get() = enhancements.toList()

    internal lateinit var protoDataContext: BoundedContext

    /**
     * Receives the event and produces a number of [CodeEnhancement]s based on the the event.
     *
     * May produce no enhancements, one, or many enhancements on a single event.
     *
     * The ordering of the resulting enhancements may not be preserved. Likewise, no deduplication
     * will be performed on the enhancements, regardless if they come from one or different
     * `Subscriber`s
     */
    public abstract fun process(event: E): Iterable<CodeEnhancement>

    /**
     * Creates a [QueryBuilder] to find projections.
     *
     * If any extra context is needed, additionally to the data provided in the event, projections
     * are the way to assemble such context.
     *
     * Projections are built on top of the same events that are given for processing to
     * the subscribers. However, guaranteed to be built completely, i.e. receive all the events,
     * before the subscribers start receiving the events. This means that the subscribers can see
     * the "full picture" via projections.
     *
     * Users may create their own projections and register them in
     * the [Pipeline][io.spine.protodata.Pipeline] via their repositories.
     */
    protected fun <P : EntityState> select(type: Class<P>): QueryBuilder<P> {
        return QueryBuilder(protoDataContext, type, javaClass.name)
    }

    final override fun dispatch(envelope: EventEnvelope) {
        checkClass(envelope.messageClass())
        val message = envelope.message()
        @Suppress("UNCHECKED_CAST") val event = message as E
        val codeEnhancements = process(event)
        enhancements.addAll(codeEnhancements)
    }

    private fun checkClass(cls: EventClass) {
        if (eventClass != cls.value()) {
            throw IllegalArgumentException(
                "Expected event of type `${eventClass.name}`, but got `${cls.value().name}`"
            )
        }
    }

    final override fun messageClasses(): ImmutableSet<EventClass> =
        externalEventClasses()

    final override fun externalEventClasses(): ImmutableSet<EventClass> =
        EventClass.setOf(eventClass)

    final override fun domesticEventClasses(): ImmutableSet<EventClass> =
        ImmutableSet.of()
}
