/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import io.spine.protodata.Field.CardinalityCase
import io.spine.protodata.type.TypeSystem

/**
 * Contains information about [MessageType]s on which the given [MessageType] depends either
 * directly through its immediate fields, or through the fields of its immediate dependencies.
 *
 * The given [messageType] is not considered as dependency (for itself), unless it is used
 * recursively in one of the immediate or nested fields.
 *
 * @param messageType
 *         the type for which we collect dependencies.
 * @param cardinality
 *         the cardinality of fields taken into account when traversing the types.
 *         `null` value means that all fields, including `repeated` and `map` ones will be
 *         taken into account when collecting types.
 * @param typeSystem
 *         the type system to obtain a `MessageType` by its name.
 */
public class MessageTypeDependencies(
    private val messageType: MessageType,
    private val cardinality: CardinalityCase?,
    private val typeSystem: TypeSystem
) {
    /**
     * The guard set against recursive type definitions.
     */
    private val encountered = mutableSetOf<MessageType>()

    /**
     * Obtains the dependencies found in the [messageType].
     */
    public fun asSet(): Set<MessageType> {
        val seq = walkMessage(messageType) {
            it.matchingFieldTypes()
        }
        return seq.toSet()
    }

    /**
     * Obtains the dependencies found in the [messageType].
     */
    @Deprecated(
        message = "Please use `asSet()` instead.",
        replaceWith = ReplaceWith("asSet()")
    )
    public val set: Set<MessageType> by lazy {
        asSet()
    }

    /**
     * Obtains the dependencies found in the [messageType].
     */
    @Deprecated(
        message = "Please use `asSet()` instead.",
        replaceWith = ReplaceWith("asSet()")
    )
    public fun scan(): Set<MessageType> = asSet()

    private fun MessageType.matchingFieldTypes(): Iterable<MessageType> =
        fieldList.asSequence()
            .filter { it.matchesCardinality() }
            .map { it.type }
            .filter { it.isMessage }
            .map { it.toMessageType(typeSystem) }
            .toSet()

    private fun Field.matchesCardinality(): Boolean =
        if (cardinality != null) {
            cardinalityCase == cardinality
        } else {
            true
        }

    private fun <T> walkMessage(
        type: MessageType,
        extractorFun: (MessageType) -> Iterable<T>
    ): Sequence<T> {
        val queue = ArrayDeque<MessageType>()
        queue.add(type)
        return sequence {
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                encountered.add(current)
                yieldAll(extractorFun(current))
                current.matchingFieldTypes()
                    .filter { !encountered.contains(it) }
                    .forEach(queue::add)
            }
        }
    }
}

