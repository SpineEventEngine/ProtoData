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

@file:JvmName("Ast")

package io.spine.protodata

import com.google.protobuf.BoolValue
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import io.spine.option.OptionsProto
import java.io.File.separatorChar
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Obtains the package and the name of the type.
 */
public fun MessageType.qualifiedName(): String = name.qualifiedName()

/**
 * Obtains the type URl of the type.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 *
 * @see MessageType.qualifiedName
 * @see TypeName.typeUrl
 */
public fun MessageType.typeUrl(): String = name.typeUrl()

/**
 * Obtains the type URl of the type.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 *
 * @see MessageType.qualifiedName
 * @see TypeName.typeUrl
 */
public fun EnumType.typeUrl(): String = name.typeUrl()

/**
 * Obtains the package and the name from this `TypeName`.
 */
public fun TypeName.qualifiedName(): String = "${packageName}.${simpleName}"

/**
 * Obtains the type URl from this `TypeName`.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 *
 * @see TypeName.qualifiedName
 * @see MessageType.typeUrl
 */
public fun TypeName.typeUrl(): String = "${typeUrlPrefix}/${qualifiedName()}"

/**
 * Shows if this field is a `map`.
 *
 * If the field is a `map`, the `Field.type` contains the type of the value, and
 * the `Field.map.key_type` contains the type the the map key.
 */
public fun Field.isMap(): Boolean = hasMap()

/**
 * Shows if this field is a list.
 *
 * In Protobuf `repeated` keyword denotes a sequence of values for a field. However, a map is also
 * treated as a repeated field for serialization reasons. We use the term "list" for repeated fields
 * which are not maps.
 */
public fun Field.isList(): Boolean = hasList()

/**
 * Shows if this field repeated.
 *
 * Can be declared in Protobuf either as a `map` or a `repeated` field.
 */
public fun Field.isRepeated(): Boolean = isMap() || isList()

/**
 * Shows if this field is a part of a `oneof` group.
 *
 * If the field is a part of a `oneof`, the `Field.oneof_name` contains the name of that `oneof`.
 */
public fun Field.isPartOfOneof(): Boolean = hasOneofName()

/**
 * Looks up an option value by the [optionName].
 *
 * @return the value of the option or a `null` if the option is not found
 */
public fun <T : Message> Iterable<Option>.find(optionName: String, cls: Class<T>): T? {
    val value = firstOrNull { it.name == optionName }?.value
    return value?.unpack(cls)
}

/**
 * Obtains the path to the `.java` file, generated from this message.
 *
 * The class which represents this message might not be the top level class of the Java file.
 */
public fun MessageType.javaFile(declaredIn: File): Path {
    val packageName = declaredIn.javaPackage()
    val javaMultipleFiles = declaredIn.javaMultipleFiles()
    val topLevelClassName = when {
        !javaMultipleFiles -> declaredIn.javaOuterClassName()
        name.nestingTypeNameList.isNotEmpty() -> name.nestingTypeNameList.first()
        else -> name.simpleName
    }
    return Paths.get(packageName.replace('.', separatorChar), "$topLevelClassName.java")
}

/**
 * Obtains the full name of the Java class, generated from this message.
 *
 * @return binary name of the class generated from this message
 */
public fun MessageType.javaClassName(declaredIn: File): String {
    val packageName = declaredIn.javaPackage()
    val javaMultipleFiles = declaredIn.javaMultipleFiles()
    val nameElements = mutableListOf<String>()
    if (!javaMultipleFiles) {
        nameElements.add(declaredIn.javaOuterClassName())
    }
    nameElements.addAll(name.nestingTypeNameList)
    nameElements.add(name.simpleName)
    val className = nameElements.joinToString(separator = "$")
    return "${packageName}.${className}"
}

private fun File.javaPackage() =
    optionList.find("java_package", StringValue::class.java)
        ?.value
        ?: packageName

private fun File.javaMultipleFiles() =
    optionList.find("java_multiple_files", BoolValue::class.java)
        ?.value
        ?: false

private fun File.javaOuterClassName() =
    optionList.find("java_outer_classname", StringValue::class.java)
        ?.value
        ?: "${nameWithoutExtension().CamelCase()}OuterClass"

@Suppress("FunctionName") // Demonstrates the CamelCase example.
private fun String.CamelCase(): String =
    split("_")
        .filter { it.isNotBlank() }
        .joinToString { it.capitalize() }


private fun File.nameWithoutExtension(): String {
    val name = path.value.split("/").last()
    val index = name.indexOf(".")
    return if (index > 0) {
        name.substring(0, index)
    } else {
        name
    }
}

/**
 * Obtains the name of this message type as a [TypeName].
 */
internal fun Descriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Obtains the name of this enum type as a [TypeName].
 */
internal fun EnumDescriptor.name(): TypeName = buildTypeName(name, file, containingType)

private fun buildTypeName(simpleName: String,
                          file: FileDescriptor,
                          containingDeclaration: Descriptor?): TypeName {
    val nestingNames = mutableListOf<String>()
    var parent = containingDeclaration
    while (parent != null) {
        nestingNames.add(0, parent.name)
        parent = parent.containingType
    }
    val typeName = TypeName
        .newBuilder()
        .setSimpleName(simpleName)
        .setPackageName(file.`package`)
        .setTypeUrlPrefix(file.options.getExtension(OptionsProto.typeUrlPrefix))
    if (nestingNames.isNotEmpty()) {
        typeName.addAllNestingTypeName(nestingNames)
    }
    return typeName.build()
}

/**
 * Obtains the name of this `oneof` as a [OneofName].
 */
internal fun OneofDescriptor.name(): OneofName =
    OneofName.newBuilder()
             .setValue(name)
             .build()

/**
 * Obtains the name of this field as a [FieldName].
 */
internal fun FieldDescriptor.name(): FieldName =
    FieldName.newBuilder()
             .setValue(name)
             .build()

/**
 * Obtains the relative path to this file as a [FilePath].
 */
internal fun FileDescriptor.path(): FilePath =
    FilePath.newBuilder()
            .setValue(name)
            .build()

/**
 * Obtains a [Type] wrapping this `PrimitiveType`.
 */
internal fun PrimitiveType.asType(): Type =
    Type.newBuilder()
        .setPrimitive(this)
        .build()
