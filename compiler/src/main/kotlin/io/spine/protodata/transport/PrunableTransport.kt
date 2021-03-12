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

package io.spine.protodata.transport

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.protobuf.Any
import io.spine.core.Ack
import io.spine.server.bus.Acks.acknowledge
import io.spine.server.integration.ExternalMessage
import io.spine.server.transport.ChannelId
import io.spine.server.transport.Publisher
import io.spine.server.transport.Subscriber
import io.spine.server.transport.TransportFactory
import io.spine.server.transport.memory.InMemoryTransportFactory

private val transportDelegate = InMemoryTransportFactory.newInstance()

/**
 * A [TransportFactory] which can clear all its channels.
 *
 * Delivery always happens in memory in the calling thread (synchronously).
 */
internal object PrunableTransport : TransportFactory by transportDelegate {

    private val subscribers: Multimap<ChannelId, Subscriber> = HashMultimap.create()

    override fun createPublisher(id: ChannelId): Publisher {
        return LocalPublisher(id, subscribers)
    }

    override fun createSubscriber(id: ChannelId): Subscriber {
        val subscriber = transportDelegate.createSubscriber(id)
        subscribers.put(id, subscriber)
        return subscriber
    }

    /**
     * Prunes all the channels previously created in the factory.
     *
     * After calling this method, all the existing connections are terminated.
     */
    fun prune() {
        subscribers.clear()
    }

    override fun close() {
        transportDelegate.close()
        prune()
    }
}

/**
 * An in-memory immediate synchronous [Publisher].
 */
private class LocalPublisher(
    private val id: ChannelId,
    private val subscribers: Multimap<ChannelId, Subscriber>
) : Publisher {

    override fun publish(messageId: Any, message: ExternalMessage): Ack {
        val subscribers = subscribers[id]
        subscribers.forEach {
            it.onMessage(message)
        }
        return acknowledge(messageId)
    }

    override fun id() = id

    override fun isStale() = false

    override fun close() {}
}
