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

@file:JvmName("TypeNames")

package io.spine.protodata.java

import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.ast.TypeName
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Obtains the path to the `.java` file, generated for the type with this name.
 *
 * The class which represents this message might not be the top-level class of the Java file,
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
 * Obtains a fully qualified Java class name, generated for the Protobuf type with this name.
 *
 * @param accordingTo The header of the proto file in which the type is declared.
 */
public fun TypeName.javaClassName(accordingTo: ProtoFileHeader): ClassName =
    composeJavaTypeName(accordingTo, {
        addAll(nestingTypeNameList)
        add(simpleName)
    }, { packageName, list ->
        ClassName(packageName, list)
    })

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
    create: (String, List<String>) -> ClassName
): ClassName {
    val packageName = accordingTo.javaPackage()
    val javaMultipleFiles = accordingTo.javaMultipleFiles()
    val nameElements = mutableListOf<String>()
    if (!javaMultipleFiles) {
        nameElements.add(accordingTo.javaOuterClassName())
    }
    setup(nameElements)
    return create(packageName, nameElements)
}
