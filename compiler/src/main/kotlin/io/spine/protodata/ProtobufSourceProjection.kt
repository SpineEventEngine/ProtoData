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

public class ProtobufSourceProjection : Projection<Path, ProtobufSource, ProtobufSource.Builder>() {

    @Subscribe
    internal fun on(e: FileDiscovered) {
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
    internal fun on(e: TypeDiscovered) {
        builder().putType(e.type.fqn(), e.type)
    }

    @Subscribe
    internal fun on(e: FieldDiscovered) {
        val typeName = e.type.fqn()
        val type = builder().getTypeOrThrow(typeName)
            .toBuilder()
            .addField(e.field)
            .build()
        builder().putType(typeName, type)
    }

    @Subscribe
    internal fun on(e: FieldOptionDiscovered) {
        val typeName = e.type.fqn()
        val typeBuilder = builder().getTypeOrThrow(typeName)
            .toBuilder()
        val fieldBuilder = typeBuilder.fieldBuilderList.find { e.field.number == it.number }
        fieldBuilder?.addOption(e.option) ?: throw IllegalStateException(
            "Cannot find field `${e.field.name}` (#${e.field.number}) in type $typeName"
        )
        builder().putType(typeName, typeBuilder.build())
    }
}
