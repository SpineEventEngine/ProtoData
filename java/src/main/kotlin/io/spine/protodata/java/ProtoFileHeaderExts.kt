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

@file:JvmName("ProtoFileHeaders")

package io.spine.protodata.java

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.ast.find
import io.spine.protodata.ast.nameWithoutExtension
import io.spine.string.camelCase

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
 * @return A value of `java_outer_classname` option, if it is set for this file.
 *         If an option is not set, a `CamelCase` version of the proto file name is
 *         returned, according to Protobuf conventions for Java.
 */
public fun ProtoFileHeader.javaOuterClassName(): String =
    optionList.find("java_outer_classname", StringValue::class.java)
        ?.value
        ?: nameWithoutExtension().camelCase()
