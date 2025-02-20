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

import com.google.protobuf.Message
import io.spine.annotation.GeneratedMixin
import io.spine.annotation.Internal
import io.spine.protobuf.defaultInstance
import io.spine.string.simply
import io.spine.type.typeName

/**
 * A high-level Protobuf declaration, such as a message, an enum, or a service.
 */
@Internal
@GeneratedMixin
public interface ProtoDeclaration : Message {

    /**
     * Obtains the name of this Protobuf declaration.
     */
    public val name: ProtoDeclarationName

    /**
     * Obtains the qualified name of this declaration, primarily for diagnostic messages.
     */
    public val qualifiedName: String
        get() = typeUrl.substringAfterLast('/')

    /**
     * The type URL of the type.
     *
     * A type URL contains the type URL prefix and the qualified name of the type separated by
     * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
     */
    public val typeUrl: String
        get() = name.typeUrl

    /**
     * Obtains a file in which the declaration is made.
     */
    public val file: File

    /**
     * The list of options of this declaration.
     */
    public val optionList: List<Option>
}

/**
 * A Protobuf declaration of a data type, such as a message or an enum.
 */
@Internal
@GeneratedMixin
public interface TypeDeclaration : ProtoDeclaration

/**
 * Finds the option with the given type [T] applied to this Protobuf declaration.
 *
 * @param T The type of the option.
 * @return the option or `null` if there is no option with such a type applied to this declaration.
 * @see ProtoDeclaration.option
 */
public inline fun <reified T : Message> ProtoDeclaration.findOption(): Option? {
    val typeUrl = T::class.java.defaultInstance.typeName.toUrl().value()
    return optionList.find { opt ->
        opt.value.typeUrl == typeUrl
    }
}

/**
 * Obtains the option with the given type [T] applied to this Protobuf declaration.
 *
 * Invoke this function if you are sure the option with the type [T] is applied
 * to the receiver declaration. Otherwise, please use [findOption].
 *
 * @param T The type of the option.
 * @return the option.
 * @throws IllegalStateException if the option is not found.
 * @see ProtoDeclaration.findOption
 */
public inline fun <reified T : Message> ProtoDeclaration.option(): Option {
    findOption<T>()?.let { return it }
        ?: error("The declaration `${qualifiedName}` must have the `${simply<T>()}` option.")
}

/**
 * Checks that the given path is absolute and is the absolute version of
 * the relative file currently set in this declaration.
 *
 * @throws IllegalArgumentException if the above conditions are not met.
 */
internal fun ProtoDeclaration.checkReplacingAbsoluteFile(path: File) {
    require(path.toJava().isAbsolute) {
        "The path `${path.path} must be absolute."
    }
    require(path.path.endsWith(file.path)) {
        "The path `${path.path}` is not the absolute version of `${file.path}`."
    }
}
