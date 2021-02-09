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

import io.spine.core.Subscribe
import io.spine.server.projection.Projection

/**
 * A projection which accumulates information about a Protobuf source file.
 */
public class ProtoSourceFileProjection
    : Projection<FilePath, ProtobufSourceFile, ProtobufSourceFile.Builder>() {

    @Subscribe
    internal fun on(e: EnteredFile) {
        builder()
            .setFilePath(e.file.path)
            .setFile(e.file)
    }

    @Subscribe
    internal fun on(e: FileOptionDiscovered) {
        builder()
            .fileBuilder
            .addOption(e.option)
    }

    @Subscribe
    internal fun on(e: EnteredType) {
        builder().putType(e.type.typeUrl(), e.type)
    }

    @Subscribe
    internal fun on(e: TypeOptionDiscovered) {
        modifyType(e.type) {
            addOption(e.option)
        }
    }

    @Subscribe
    internal fun on(e: EnteredOneofGroup) {
        modifyType(e.type) {
            addOneofGroup(e.group)
        }
    }

    @Subscribe
    internal fun on(e: OneofOptionDiscovered) {
        modifyType(e.type) {
            val oneof = findOneof(e.group)
            oneof.addOption(e.option)
        }
    }

    @Subscribe
    internal fun on(e: EnteredField) {
        modifyType(e.type) {
            if (e.field.hasOneofName()) {
                val oneof = findOneof(e.field.oneofName)
                oneof.addField(e.field)
            } else {
                addField(e.field)
            }
        }
    }

    @Subscribe
    internal fun on(e: FieldOptionDiscovered) {
        modifyType(e.type) {
            val field = findField(e.field)
            field.addOption(e.option)
        }
    }

    private fun modifyType(name: TypeName, changes: MessageType.Builder.() -> Unit) {
        val typeUrl = name.typeUrl()
        val typeBuilder = builder().getTypeOrThrow(typeUrl)
            .toBuilder()
        changes(typeBuilder)
        builder().putType(typeUrl, typeBuilder.build())
    }
}

private fun MessageType.Builder.findOneof(name: OneofName): OneofGroup.Builder =
    oneofGroupBuilderList.find { it.name == name }!!

/**
 * Looks up a field by its name in this message type.
 *
 * The field may be found either in the message directly or in one of the `oneof` groups.
 */
private fun MessageType.Builder.findField(name: FieldName): Field.Builder {
    var fieldBuilder = fieldBuilderList.find { it.name == name }
    if (fieldBuilder == null) {
        fieldBuilder = oneofGroupBuilderList
            .flatMap { group -> group.fieldBuilderList }
            .find { it.name == name }
    }
    return fieldBuilder!!
}
