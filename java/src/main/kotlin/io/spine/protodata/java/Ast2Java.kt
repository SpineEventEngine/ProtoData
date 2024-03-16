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

@file:JvmName("Ast2Java")

@file:Suppress("TooManyFunctions")

package io.spine.protodata.java

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import io.spine.protodata.EnumType
import io.spine.protodata.Field
import io.spine.protodata.FieldName
import io.spine.protodata.MessageType
import io.spine.protodata.PrimitiveType
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.TypeName
import io.spine.protodata.find
import io.spine.protodata.isPrimitive
import io.spine.protodata.nameWithoutExtension
import io.spine.string.camelCase
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Obtains the path to the `.java` file, generated from this message.
 *
 * The class which represents this message might not be the top level class of the Java file,
 * which is determined by the options in the given Protobuf file header.
 */
public fun MessageType.javaFile(accordingTo: ProtoFileHeader): Path =
    name.javaFile(accordingTo)

/**
 * Obtains the path to the `.java` file, generated for the type with this name.
 *
 * The class which represents this message might not be the top level class of the Java file,
 * which is determined by the options in the given Protobuf file header.
 */
public fun TypeName.javaFile(accordingTo: ProtoFileHeader): Path {
    val packageName = accordingTo.javaPackage()
    val javaMultipleFiles = accordingTo.javaMultipleFiles()
    val topLevelClassName = when {
        !javaMultipleFiles -> accordingTo.javaOuterClassName()
        nestingTypeNameList.isNotEmpty() -> nestingTypeNameList.first()
        else -> simpleName
    }
    val packageAsPath = packageName.replace('.', java.io.File.separatorChar)
    return Path(packageAsPath, "$topLevelClassName.java")
}

/**
 * Obtains the full name of the Java class, generated from this message.
 *
 * @return name of the class generated from this message.
 */
public fun MessageType.javaClassName(accordingTo: ProtoFileHeader): ClassName =
    name.javaClassName(accordingTo)

/**
 * Obtains the full name of the Java enum, generated from this Protobuf enum.
 *
 * @return name of the enum class generated from this enum.
 */
public fun EnumType.javaClassName(accordingTo: ProtoFileHeader): ClassName =
    name.javaClassName(accordingTo)

/**
 * Obtains a Java type name for a Protobuf declaration.
 *
 * The function calculates the package name taking into account values of
 * `java_package_name` and `java_multiple_files` options that may present in the file.
 *
 * @param accordingTo
 *        the header of the Protobuf file where the declaration resides in.
 * @param setup
 *        the block of code which adds the name elements to the target name.
 * @param create
 *        the block of code which produces the name from the target Java type.
 * @see ProtoFileHeader.javaPackage
 * @see ProtoFileHeader.javaMultipleFiles
 */
internal fun composeJavaTypeName(
    accordingTo: ProtoFileHeader,
    setup: MutableList<String>.() -> Unit,
    create: (String, List<String>) -> ClassOrEnumName
): ClassOrEnumName {
    val packageName = accordingTo.javaPackage()
    val javaMultipleFiles = accordingTo.javaMultipleFiles()
    val nameElements = mutableListOf<String>()
    if (!javaMultipleFiles) {
        nameElements.add(accordingTo.javaOuterClassName())
    }
    setup(nameElements)
    return create(packageName, nameElements)
}

/**
 * Obtains a fully qualified Java class name, generated for the Protobuf type with this name.
 */
public fun TypeName.javaClassName(accordingTo: ProtoFileHeader): ClassName =
    composeJavaTypeName(accordingTo, {
        addAll(nestingTypeNameList)
        add(simpleName)
    }, { packageName, list ->
        ClassName(packageName, list)
    }) as ClassName

/**
 * Obtains a fully qualified Java enum type name, generated for the Protobuf enum with this name.
 */
public fun TypeName.javaEnumName(accordingTo: ProtoFileHeader): EnumName =
    composeJavaTypeName(accordingTo, {
        addAll(nestingTypeNameList)
        add(simpleName)
    }, { packageName, list ->
        EnumName(packageName, list)
    }) as EnumName

/**
 * Obtains a name of a Java package for the code generated from this Protobuf file.
 *
 * @return A value of the `java_package` option, if it is set.
 *         Otherwise, returns the package name of the file.
 */
public fun ProtoFileHeader.javaPackage(): String =
    optionList.find("java_package", StringValue::class.java)
        ?.value
        ?: packageName

/**
 * Obtains a value of `java_multiple_files` option set for this file.
 */
public fun ProtoFileHeader.javaMultipleFiles(): Boolean =
    optionList.find("java_multiple_files", BoolValue::class.java)
        ?.value
        ?: false

/**
 * Obtains a name of the Java outer class generated for this Protobuf file.
 *
 * @return A value of `java_outer_classname` option, if it set for this file.
 */
public fun ProtoFileHeader.javaOuterClassName(): String =
    optionList.find("java_outer_classname", StringValue::class.java)
        ?.value
        ?: nameWithoutExtension().camelCase()


/**
 * Obtains the name of the field in Java case, as if it were declared as a field
 * in a Java class.
 *
 * @return this field name in `lowerCamelCase` form.
 */
public fun FieldName.javaCase(): String {
    val camelCase = value.camelCase()
    return camelCase.replaceFirstChar { it.lowercaseChar() }
}

/**
 * Obtains the name of the primary setter method for this field.
 */
public val Field.primarySetterName: String
    get() = FieldMethods(this).primarySetter

/**
 * Obtains the name of the accessor method for this field.
 */
public val Field.getterName: String
    get() = FieldMethods(this).getter

/**
 * Tells if the type of this field corresponds to a primitive type in Java.
 */
public val Field.isJavaPrimitive: Boolean
    get() = if (!type.isPrimitive) {
        false
    } else when (type.primitive) {
        PrimitiveType.TYPE_STRING, PrimitiveType.TYPE_BYTES -> false
        else -> true
    }
