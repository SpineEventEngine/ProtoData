/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.codegen.java.file

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import io.spine.protobuf.pack
import io.spine.protodata.Option
import io.spine.protodata.option

/**
 * The option to instruct `protoc` to generate multiple Java files.
 */
public val javaMultipleFiles: Option
    get() = option {
        name = "java_multiple_files"
        value = BoolValue.of(true).pack()
    }

/**
 * Obtains an option to set the Java package with the given [name]
 * for the generated code.
 */
public fun javaPackage(name: String): Option = option {
    this.name = "java_package"
    value = StringValue.of(name).pack()
}

/**
 * Obtains the option to set the [name] of the outer Java class.
 */
public fun javaOuterClassName(name: String): Option = option {
    this.name = "java_outer_classname"
    value = StringValue.of(name).pack()
}