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

package io.spine.protodata.java

import io.spine.option.IsOption
import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.java.JavaTypeName.Companion.PACKAGE_SEPARATOR
import org.checkerframework.checker.signature.qual.FullyQualifiedName

/**
 * Obtains a fully qualified name of a Java type corresponding the value
 * of the `java_type` property of `is` or `every_is` options.
 *
 * @param header the header of the proto file to resolve the Java package if
 *   a simple type name is specified.
 * @throws IllegalStateException if the `java_type` property is empty or blank.
 * @see IsOption
 */
public fun IsOption.qualifiedJavaType(header: ProtoFileHeader): @FullyQualifiedName String {
    check(javaType.isNotEmpty() && javaType.isNotBlank()) {
        "The value of `java_type` must not be empty or blank. Got: `\"$javaType\"`."
    }
    return if (javaType.isQualified) {
        javaType
    } else {
        "${header.javaPackage()}.$javaType"
    }
}

private val String.isQualified: Boolean
    get() = contains(PACKAGE_SEPARATOR)
