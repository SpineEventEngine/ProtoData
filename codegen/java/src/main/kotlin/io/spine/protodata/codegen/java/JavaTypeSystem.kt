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

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Type
import io.spine.protodata.Type.KindCase.ENUMERATION
import io.spine.protodata.Type.KindCase.MESSAGE
import io.spine.protodata.Type.KindCase.PRIMITIVE
import io.spine.protodata.TypeName
import io.spine.protodata.Value
import io.spine.protodata.codegen.BaseTypeSystem
import io.spine.protodata.name
import io.spine.type.KnownTypes

/**
 * A type system of an application.
 *
 * Includes all the types known to the app at runtime.
 */
public class JavaTypeSystem
private constructor(
    knownTypes: Map<TypeName, ClassName>
) : BaseTypeSystem<ClassName>(knownTypes) {

    override fun convertPrimitiveType(type: PrimitiveType): ClassName = type.toJavaClass()

    public companion object {

        /**
         * Creates a new `JavaTypeSystem` builder.
         */
        @JvmStatic
        public fun newBuilder(): Builder = Builder()
    }

    /**
     * The builder of a new `JavaTypeSystem` of an application.
     */
    public class Builder internal constructor() {

        private val knownTypes = mutableMapOf<TypeName, ClassName>()

        init {
            KnownTypes.instance()
                .asTypeSet()
                .messagesAndEnums()
                .forEach {
                    knownTypes[it.typeName()] = ClassName(it.javaClass())
                }
        }

        private fun io.spine.type.Type<*, *>.typeName(): TypeName {
            return when (val descriptor = descriptor()) {
                is Descriptor -> descriptor.name()
                is EnumDescriptor -> descriptor.name()
                else -> error("Unexpected type: `$descriptor`.")
            }
        }

        @CanIgnoreReturnValue
        public fun put(file: File, messageType: MessageType): Builder {
            val javaClassName = messageType.javaClassName(declaredIn = file)
            knownTypes[messageType.name] = javaClassName
            return this
        }

        @CanIgnoreReturnValue
        public fun put(file: File, enumType: EnumType): Builder {
            val javaClassName = enumType.javaClassName(declaredIn = file)
            knownTypes[enumType.name] = javaClassName
            return this
        }

        /**
         * Adds all the definitions from the given [file] to the type system.
         */
        @CanIgnoreReturnValue
        public fun addFrom(file: ProtobufSourceFile): Builder {
            file.typeMap.values.forEach {
                put(file.file, it)
            }
            file.enumTypeMap.values.forEach {
                put(file.file, it)
            }
            return this
        }

        /**
         * Builds an instance of `JavaTypeSystem`.
         */
        public fun build(): JavaTypeSystem = JavaTypeSystem(knownTypes)
    }
}

/**
 * Obtains the canonical name of the class representing the given [type] in Java.
 *
 * For Java primitive types, obtains wrapper classes.
 *
 * @throws IllegalStateException if the type is unknown
 */
internal fun JavaTypeSystem.toClass(type: Type): ClassName = when (type.kindCase) {
    PRIMITIVE -> type.primitive.toJavaClass()
    MESSAGE, ENUMERATION -> convertTypeName(type.message)
    else -> throw IllegalArgumentException("Type is empty.")
}
