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

package io.spine.protodata

import io.grpc.stub.StreamObserver
import io.spine.base.EntityState
import io.spine.base.Identifier
import io.spine.client.ActorRequestFactory
import io.spine.client.QueryResponse
import io.spine.core.UserId
import io.spine.protobuf.AnyPacker
import io.spine.server.BoundedContext

/**
 * A builder for queries to the projections defined on top of the Protobuf compiler events.
 */
public class QueryBuilder<T : EntityState>
internal constructor(
    private val context: BoundedContext,
    private val type: Class<T>,
    subscriberName: String
) {

    private val actor = UserId.newBuilder()
        .setValue(subscriberName)
        .build()
    private val factory = ActorRequestFactory.newBuilder()
        .setActor(actor)
        .build()

    private var id: Any? = null

    /**
     * Selects a projection by its ID.
     */
    public fun withId(id: Any): QueryBuilder<T> {
        this.id = Identifier.checkSupported(id.javaClass)
        return this
    }

    /**
     * Runs the query and obtains the results.
     */
    public fun execute(): Set<T> {
        val queries = factory.query()
        val query = if (id == null) {
            queries.byIds(type, setOf(id))
        } else {
            queries.all(type)
        }
        val observer = Observer(type)
        context.stand().execute(query, observer)
        return observer.foundResult().toSet()
    }

    /**
     * A [StreamObserver] which listens to a single [QueryResponse].
     *
     * The observer persists the [found result][foundResult] as a list of messages.
     */
    private class Observer<T : EntityState>(
        private val type: Class<T>
    ) : StreamObserver<QueryResponse> {

        private var result: List<T>? = null

        override fun onNext(response: QueryResponse?) {
            response!!
            result = response.messageList.map {
                AnyPacker.unpack(it.state, type)
            }
        }

        override fun onError(e: Throwable?) {
            throw e!!
        }

        override fun onCompleted() {}

        /**
         * Obtains the found result or throws an `IllegalStateException` if the result has not been
         * received.
         */
        fun foundResult(): List<T> {
            return result ?: throw IllegalStateException("Query has not yielded any result yet.")
        }
    }
}
