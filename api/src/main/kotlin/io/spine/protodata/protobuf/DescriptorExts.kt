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

package io.spine.protodata.protobuf

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.coordinates
import io.spine.protodata.ast.documentation
import io.spine.protodata.ast.messageType
import io.spine.protodata.ast.toList
import io.spine.protodata.ast.type
import io.spine.string.camelCase

/**
 * Obtains the name of this message type as a [TypeName].
 */
public fun Descriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Obtains only descriptors of message types declared under the message represented
 * by this descriptor.
 *
 * The method filters synthetic descriptors created for map fields.
 * A descriptor of a map field entry is named after the name of the field
 * with the `"Entry"` suffix.
 * We use this convention for filtering [Descriptor.nestedTypes] returned by Protobuf API.
 *
 * @see <a href="https://protobuf.dev/programming-guides/proto3/#maps-features">
 *     Protobuf documentation</a>
 */
public fun Descriptor.realNestedTypes(): List<Descriptor> {
    val mapEntryTypes = fields.filter { it.isMapField }
        .map { it.name.camelCase() + "Entry" }.toList()
    return nestedTypes.filter { !mapEntryTypes.contains(it.name) }
}

/**
 * Converts the receiver `Descriptor` into a [MessageType].
 */
public fun Descriptor.toMessageType(): MessageType =
    messageType {
        name = name()
        file = getFile().file()
        val self = this@toMessageType
        option.addAll(options.toList(self))
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        oneofGroup.addAll(realOneofs.map { it.toOneOfGroup() })
        field.addAll(fields.mapped())
        nestedMessages.addAll(realNestedTypes().map { it.name() })
        nestedEnums.addAll(enumTypes.map { it.name() })
        doc = documentation().forMessage(self)
        span = coordinates().forMessage(self)
    }

/**
 * Converts the `Descriptor` into [Type] instance with the [message][Type.message]
 * property initialized.
 */
public fun Descriptor.toType(): Type = type {
    message = name()
}

/**
 * Obtains a field with the given [name].
 *
 * @throws IllegalStateException If there is no such a field in this message type.
 */
public fun Descriptor.field(name: String): Field {
    val field: FieldDescriptor? = findFieldByName(name)
    check(field != null) {
        "Unable to find the field named `$name` in the message type `${this.fullName}`."
    }
    val result = field.toField()
    return result
}

/**
 * Create a type name for a type with the given [simpleName].
 *
 * @param simpleName The simple name of the type.
 * @param file The file in which the type is declared.
 * @param containingDeclaration If specified, a descriptor of a message type under
 *   which the type is declared.
 */
internal fun buildTypeName(
    simpleName: String,
    file: FileDescriptor,
    containingDeclaration: Descriptor?
): TypeName {
    val nestingNames = mutableListOf<String>()
    var parent = containingDeclaration
    while (parent != null) {
        nestingNames.add(0, parent.name)
        parent = parent.containingType
    }
    val typeName = TypeName.newBuilder()
        .setSimpleName(simpleName)
        .setPackageName(file.`package`)
        .setTypeUrlPrefix(file.typeUrlPrefix)
    if (nestingNames.isNotEmpty()) {
        typeName.addAllNestingTypeName(nestingNames)
    }
    return typeName.build()
}

/**
 * Produces a sequence by walking through all the nested message definitions staring with [type].
 *
 * @param type The message definition which may contain a nested message definition
 *   to walk through.
 * @param extractorFun The function that, given a message definition, extracts
 *   the items of interest.
 * @return results of the calls to [extractorFun] flattened into one sequence.
 */
internal fun <T> walkMessage(
    type: Descriptor,
    extractorFun: (Descriptor) -> Iterable<T>,
): Sequence<T> {
    val queue = ArrayDeque<Descriptor>()
    queue.add(type)
    return sequence {
        while (queue.isNotEmpty()) {
            val msg = queue.removeFirst()
            yieldAll(extractorFun(msg))
            queue.addAll(msg.realNestedTypes())
        }
    }
}
