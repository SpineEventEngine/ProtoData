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

package io.spine.protodata.codegen.java.given

import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_PACKAGE_FIELD_NUMBER
import com.google.protobuf.Empty
import com.google.protobuf.StringValue
import io.spine.protobuf.pack
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.codegen.java.JavaTypeSystem
import io.spine.protodata.constantName
import io.spine.protodata.enumConstant
import io.spine.protodata.fieldName
import io.spine.protodata.file
import io.spine.protodata.filePath
import io.spine.protodata.messageType
import io.spine.protodata.option
import io.spine.protodata.path
import io.spine.protodata.type
import io.spine.protodata.typeName
import io.spine.protodata.enumType as newEnumType
import io.spine.protodata.field as newField

object TypesTestEnv {

    val filePath = filePath { value = "acme/example/foo.proto" }
    val multipleFilesOption = option {
        name = "java_multiple_files"
        number = JAVA_MULTIPLE_FILES_FIELD_NUMBER
        type = type { primitive = TYPE_BOOL }
        value = BoolValue.of(true).pack()
    }
    val javaPackageOption = option {
        name = "java_package"
        number = JAVA_PACKAGE_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = StringValue.of("ua.acme.example").pack()
    }
    val protoFile = file {
        path = filePath
        packageName = "acme.example"
        option.add(multipleFilesOption)
        option.add(javaPackageOption)
    }
    val messageTypeName = typeName {
        packageName = protoFile.packageName
        simpleName = "Foo"
        typeUrlPrefix = "type.spine.io"
    }
    val stringField = newField {
        type = type { primitive = TYPE_STRING }
        name = fieldName { value = "bar" }
        single = Empty.getDefaultInstance()
    }
    val messageType = messageType {
        file = filePath
        name = messageTypeName
        field.add(stringField)
    }
    val enumTypeName = typeName {
        packageName = protoFile.packageName
        typeUrlPrefix = messageTypeName.typeUrlPrefix
        simpleName = "Kind"
    }
    val undefinedConstant = enumConstant {
        name = constantName { value = "UNDEFINED" }
        number = 0
    }
    val enumConstant = enumConstant {
        name = constantName { value = "INSTANCE" }
        number = 1
    }
    val enumType = newEnumType {
        file = filePath
        name = enumTypeName
        constant.add(undefinedConstant)
        constant.add(enumConstant)
    }
    val typeSystem: JavaTypeSystem = JavaTypeSystem.newBuilder()
        .put(protoFile, messageType)
        .put(protoFile, enumType)
        .build()
}
