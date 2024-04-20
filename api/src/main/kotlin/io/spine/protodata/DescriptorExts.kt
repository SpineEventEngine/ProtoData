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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor

/**
 * Obtains documentation of this [GenericDescriptor].
 */
internal val GenericDescriptor.fileDoc: Documentation
    get() = Documentation(file)

/**
 * Obtains the name of this message type as a [TypeName].
 */
public fun Descriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Converts the receiver `Descriptor` into a [MessageType].
 */
public fun Descriptor.toMessageType(): MessageType =
    messageType {
        name = name()
        file = getFile().file()
        doc = fileDoc.forMessage(this@toMessageType)
        option.addAll(options.toList())
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        oneofGroup.addAll(realOneofs.map { it.toOneOfGroup() })
        field.addAll(fields.mapped())
        nestedMessages.addAll(nestedTypes.map { it.name() })
        nestedEnums.addAll(enumTypes.map { it.name() })
    }

/**
 * Obtains a field with the given [name].
 *
 * @throws IllegalStateException
 *          if there is no such a field in this message type.
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
 * @param simpleName
 *         a simple name of the type.
 * @param file
 *         the file in which the type is declared.
 * @param containingDeclaration
 *         if specified, a descriptor of a message type under which the type is declared.
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
 * @param type
 *         the message definition which may contain nested message definition to walk through.
 * @param extractorFun
 *         a function that, given a message definition, extracts the items of interest.
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
            queue.addAll(msg.nestedTypes)
        }
    }
}
