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

import io.spine.protodata.File
import io.spine.protodata.ServiceName
import io.spine.protodata.TypeName
import io.spine.protodata.type.Declaration
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.Java

/**
 * This convention defines a declarations of message or enum types declared in Protobuf.
 *
 * @throws IllegalStateException if the type name is unknown.
 */
public class MessageOrEnumConvention(ts: TypeSystem) : BaseJavaConvention<TypeName>(ts) {

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
public class MessageOrBuilderConvention(ts: TypeSystem) : BaseJavaConvention<TypeName>(ts) {

    override fun declarationFor(name: TypeName): Declaration<Java, ClassName> {
        val decl = MessageOrEnumConvention(typeSystem).declarationFor(name)
        val messageOrBuilder = decl.name.withSuffix("OrBuilder")
        return Declaration(messageOrBuilder, messageOrBuilder.javaFile)
    }
}

/**
 * Abstract base for conventions which govern generated code for protobuf services.
 *
 * @see <a href="https://protobuf.dev/reference/java/java-generated/#service">Protobuf Services</a>
 */
public abstract class AbstractServiceConvention(ts: TypeSystem) :
    BaseJavaConvention<ServiceName>(ts) {

    override fun declarationFor(name: ServiceName): Declaration<Java, ClassName> {
        val pair = typeSystem.findService(name)
        val file = pair?.second
        check(file != null) { "Unknown service `${name.typeUrl}`." }
        val service = pair.first
        val cls = javaClassName(service.name, declaredIn = file)
        return Declaration(cls, cls.javaFile)
    }

    /**
     * Calculates a class name for the given service declared in the given file.
     */
    protected abstract fun javaClassName(name: ServiceName, declaredIn: File): ClassName
}

/**
 * This convention governs declarations of generated gRPC stubs.
 *
 * In the context of API-level annotations, putting an annotation on a root gRPC-generated
 * class effectively puts it on all the nested classes.
 */
public class GrpcServiceConvention(ts: TypeSystem) : AbstractServiceConvention(ts) {

    protected override fun javaClassName(name: ServiceName, declaredIn: File): ClassName =
        composeJavaClassName(declaredIn) {
            add(name.simpleName + "Grpc")
        }
}

/**
 * This convention governs declarations of
 * [generic](https://protobuf.dev/reference/java/java-generated/#service)
 * Protobuf service stubs.
 *
 * @see <a href="https://protobuf.dev/reference/java/java-generated/#service">Protobuf Services</a>
 */
public class GenericServiceConvention(ts: TypeSystem): AbstractServiceConvention(ts) {

    override fun javaClassName(name: ServiceName, declaredIn: File): ClassName =
        composeJavaClassName(declaredIn) {
            add(name.simpleName)
        }
}
