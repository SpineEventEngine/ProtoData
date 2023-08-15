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

package io.spine.protodata.test

import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_OUTER_CLASSNAME_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_PACKAGE_FIELD_NUMBER
import com.google.protobuf.Empty
import com.google.protobuf.StringValue
import io.spine.protobuf.pack
import io.spine.protodata.EnumConstant
import io.spine.protodata.EnumType
import io.spine.protodata.Field
import io.spine.protodata.File
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.Option
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.TypeName
import io.spine.protodata.type.TypeSystem
import io.spine.protodata.constantName
import io.spine.protodata.enumConstant
import io.spine.protodata.fieldName
import io.spine.protodata.file
import io.spine.protodata.filePath
import io.spine.protodata.messageType
import io.spine.protodata.option
import io.spine.protodata.protobufSourceFile
import io.spine.protodata.type
import io.spine.protodata.typeName
import io.spine.protodata.enumType as newEnumType
import io.spine.protodata.field as newField

public object TypesTestEnv {

    public val filePath: FilePath = filePath { value = "acme/example/foo.proto" }
    public val rejectionsFilePath: FilePath = filePath { value = "acme/example/cartoon_rejections.proto" }
    public val multipleFilesOption: Option = option {
        name = "java_multiple_files"
        number = JAVA_MULTIPLE_FILES_FIELD_NUMBER
        type = type { primitive = TYPE_BOOL }
        value = BoolValue.of(true).pack()
    }
    public val javaPackageOption: Option = option {
        name = "java_package"
        number = JAVA_PACKAGE_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = StringValue.of("ua.acme.example").pack()
    }
    public val outerClassnameOption: Option = option {
        name = "java_outer_classname"
        number = JAVA_OUTER_CLASSNAME_FIELD_NUMBER
        type = type { primitive = TYPE_STRING }
        value = StringValue.of("CartoonRejections").pack()
    }
    public val protoFile: File = file {
        path = filePath
        packageName = "acme.example"
        option.add(multipleFilesOption)
        option.add(javaPackageOption)
    }
    public val rejectionsProtoFile: File = file {
        path = rejectionsFilePath
        packageName = "acme.example"
        option.add(javaPackageOption)
        option.add(outerClassnameOption)
    }
    public val messageTypeName: TypeName = typeName {
        packageName = protoFile.packageName
        simpleName = "Foo"
        typeUrlPrefix = "type.spine.io"
    }
    public val rejectionTypeName: TypeName = typeName {
        packageName = rejectionsProtoFile.packageName
        simpleName = "CannotDrawCartoon"
        typeUrlPrefix = "type.spine.io"
    }
    public val stringField: Field = newField {
        type = type { primitive = TYPE_STRING }
        name = fieldName { value = "bar" }
        single = Empty.getDefaultInstance()
    }
    public val idField: Field = newField {
        type = type { primitive = TYPE_STRING }
        name = fieldName { value = "uuid" }
        single = Empty.getDefaultInstance()
    }
    public val messageType: MessageType = messageType {
        file = filePath
        name = messageTypeName
        field.add(stringField)
    }
    public val rejectionType: MessageType = messageType {
        file = rejectionsFilePath
        name = rejectionTypeName
        field.add(idField)
    }
    public val enumTypeName: TypeName = typeName {
        packageName = protoFile.packageName
        typeUrlPrefix = messageTypeName.typeUrlPrefix
        simpleName = "Kind"
    }
    public val undefinedConstant: EnumConstant = enumConstant {
        name = constantName { value = "UNDEFINED" }
        number = 0
    }
    public val enumConstant: EnumConstant = enumConstant {
        name = constantName { value = "INSTANCE" }
        number = 1
    }
    public val enumType: EnumType = newEnumType {
        file = filePath
        name = enumTypeName
        constant.add(undefinedConstant)
        constant.add(enumConstant)
    }
    public val typeSystem: TypeSystem = run {
        val definitions = protobufSourceFile {
            filePath = TypesTestEnv.filePath
            file = protoFile
            type.put(messageTypeName.typeUrl, messageType)
            enumType.put(enumTypeName.typeUrl, TypesTestEnv.enumType)
        }
        val rejections = protobufSourceFile {
            filePath = rejectionsFilePath
            file = rejectionsProtoFile
            type.put(rejectionTypeName.typeUrl, rejectionType)
        }
        TypeSystem(setOf(definitions, rejections))
    }
}
