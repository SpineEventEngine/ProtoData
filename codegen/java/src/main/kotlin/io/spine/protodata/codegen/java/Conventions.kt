/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.protodata.codegen.java

import io.spine.protodata.TypeName
import io.spine.protodata.type.Declaration
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.Java

/**
 * This convention defines a declarations of message or enum types declared in Protobuf.
 *
 * @throws IllegalStateException if the type name is unknown.
 */
public class MessageOrEnumConvention(ts: TypeSystem) : BaseJavaTypeConvention(ts) {

    override fun declarationFor(name: TypeName): Declaration<Java, ClassName> {
        val file = typeSystem.findMessageOrEnum(name)?.second
        check(file != null) { "Unknown type `${name.typeUrl}`." }
        val cls = name.javaClassName(declaredIn = file)
        return Declaration(cls, cls.javaFile)
    }
}

/**
 * This convention governs declarations of interfaces extending
 * [MessageOrBuilder][com.google.protobuf.MessageOrBuilder] which are generated along
 * with corresponding message classes.
 *
 * @throws IllegalStateException if the type name is unknown.
 */
public class MessageOrBuilderConvention(ts: TypeSystem) : BaseJavaTypeConvention(ts) {

    override fun declarationFor(name: TypeName): Declaration<Java, ClassName> {
        val decl = MessageOrEnumConvention(typeSystem).declarationFor(name)
        val messageOrBuilder = decl.name.withSuffix("OrBuilder")
        return Declaration(messageOrBuilder, messageOrBuilder.javaFile)
    }
}
