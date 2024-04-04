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
    private val found = mutableSetOf<MessageType>()

    /**
     * Obtains the dependencies found the [messageType].
     */
    public val set: Set<MessageType> by lazy {
        scan()
        found
    }

    private fun scan() {
        val typesToScan = ArrayDeque<MessageType>()
        typesToScan.add(messageType)
        while (!typesToScan.isEmpty()) {
            val current = typesToScan.removeFirst()
            current.addMessageFieldTypes()
        }
    }

    /**
     * Adds [MessageType]s discovered in the fields of this type, unless they
     * are already remembered as [found].
     *
     * Only singular fields are checked.
     */
    private fun MessageType.addMessageFieldTypes() {
        fieldList.asSequence()
            .filter { it.matchesCardinality() }
            .filter { it.type.isMessage }
            .map { it.type.toMessageType(typeSystem) }
            .filter { !found.contains(it) }
            .forEach {
                found.add(it)
            }
    }

    private fun Field.matchesCardinality(): Boolean =
        if (cardinality != null) {
            cardinalityCase == cardinality
        } else {
            true
        }
}
