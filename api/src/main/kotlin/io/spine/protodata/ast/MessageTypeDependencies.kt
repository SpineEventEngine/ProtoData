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

package io.spine.protodata.ast

import io.spine.protodata.type.TypeSystem

/**
 * Contains information about [MessageType]s on which the given [MessageType] depends either
 * directly through its immediate fields, or through the fields of its immediate dependencies.
 *
 * The given [messageType] is not considered as dependency (for itself), unless it is used
 * recursively in one of the immediate or nested fields.
 *
 * @param messageType The type for which we collect dependencies.
 * @param cardinalities The cardinalities of fields taken into account when traversing the types.
 *    Empty set means that all fields, including `repeated` and `map` ones will be
 *    taken into account when collecting types.
 * @param typeSystem The type system to obtain a `MessageType` by its name.
 */
public class MessageTypeDependencies(
    private val messageType: MessageType,
    private val cardinalities: Set<Cardinality>,
    private val typeSystem: TypeSystem
) {

    /**
     * Creates an instance for collecting all the dependencies of the given
     * [messageType] using the given [typeSystem].
     */
    public constructor(messageType: MessageType, typeSystem: TypeSystem) :
            this(messageType, emptySet(), typeSystem)

    /**
     * Creates an instance for collecting the dependencies of the given
     * [messageType] for fields with the specified [cardinality] using
     * the given [typeSystem].
     */
    public constructor(
        messageType: MessageType,
        cardinality: Cardinality,
        typeSystem: TypeSystem
    ) : this(messageType, setOf(cardinality), typeSystem)

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

    private fun MessageType.matchingFieldTypes(): Iterable<MessageType> =
        fieldList.asSequence()
            .filter { it.matchesCardinality() }
            .map { it.type }
            .mapNotNull { it.extractMessageType(typeSystem) }
            .toSet()

    private fun Field.matchesCardinality(): Boolean =
        if (cardinalities.isNotEmpty()) {
            cardinalities.contains(type.cardinality)
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

/**
 * Converts this field type into [MessageType] or `null`
 * if this field type is not a message, or if it does not refer to message being a list or a map.
 */
private fun FieldType.extractMessageType(typeSystem: TypeSystem): MessageType? = when {
    isMessage -> message.toMessageType(typeSystem)
    isList -> list.maybeMessageType(typeSystem)
    isMap -> map.valueType.maybeMessageType(typeSystem)
    else -> null
}

/**
 * Optionally converts this type into [MessageType] if this type is a message.
 */
private fun Type.maybeMessageType(typeSystem: TypeSystem): MessageType? =
    if (isMessage) toMessageType(typeSystem) else null
