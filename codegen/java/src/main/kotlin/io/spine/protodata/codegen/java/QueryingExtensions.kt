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

@file:JvmName("JavaNaming")

package io.spine.protodata.codegen.java

import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Querying
import io.spine.protodata.TypeName
import io.spine.protodata.java.TypeDeclaration
import io.spine.protodata.select
import io.spine.protodata.typeUrl

/**
 * Obtains the name of the class for the given Protobuf type name.
 *
 * The type name could represent a message or an enum type.
 *
 * This method performs a lookup over prepared [TypeDeclaration] views. In order for the method to
 * work, [TypeDeclarationView] must be supplied by one of
 * the [Plugins][io.spine.protodata.plugin.Plugin]. Otherwise, an error occurs.
 *
 * Include [JavaPlugin] into the ProtoData execution to make the necessary view available.
 */
public fun Querying.classNameOf(type: TypeName): ClassName {
    val decl = select<TypeDeclaration>()
        .withId(type)
        .orElseThrow { IllegalArgumentException("Cannot find type with name `${type.typeUrl()}`.") }
    val filePath = decl.whereDeclared
    val source = select<ProtobufSourceFile>()
        .withId(filePath)
        .orElseThrow { IllegalStateException("Cannot find file `${filePath.value}`.") }
    return type.javaClassName(declaredIn = source.file)
}
