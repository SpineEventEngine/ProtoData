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

@file:Suppress("TooManyFunctions")

package io.spine.protodata

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type.BOOL
import com.google.protobuf.Descriptors.FieldDescriptor.Type.BYTES
import com.google.protobuf.Descriptors.FieldDescriptor.Type.DOUBLE
import com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FIXED32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FIXED64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FLOAT
import com.google.protobuf.Descriptors.FieldDescriptor.Type.GROUP
import com.google.protobuf.Descriptors.FieldDescriptor.Type.INT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.INT64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SFIXED32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SFIXED64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SINT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SINT64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING
import com.google.protobuf.Descriptors.FieldDescriptor.Type.UINT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.UINT64
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.Empty
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import io.spine.base.EventMessage
import io.spine.protobuf.TypeConverter
import io.spine.protobuf.pack
import io.spine.protodata.FieldKt.ofMap
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_INT64
import io.spine.protodata.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.PrimitiveType.TYPE_SINT32
import io.spine.protodata.PrimitiveType.TYPE_SINT64
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.PrimitiveType.TYPE_UINT32
import io.spine.protodata.PrimitiveType.TYPE_UINT64
import io.spine.protodata.ProtoFileHeader.SyntaxVersion
import io.spine.protodata.ProtoFileHeader.SyntaxVersion.PROTO2
import io.spine.protodata.ProtoFileHeader.SyntaxVersion.PROTO3
import kotlin.Any
import com.google.protobuf.Any as ProtoAny

/**
 * Converts this field descriptor into a [Field] with options.
 *
 * @see buildField
 */
public fun FieldDescriptor.toField(): Field {
    val field = buildField(this)
    return field.copy {
        // There are several similar expressions in this file like
        // the `option.addAll()` call below. Sadly, these duplicates
        // could not be refactored into a common function because
        // they have no common compile-time type.
        option.addAll(options.toList())
    }
}

/**
 * Converts this field descriptor into a [Field].
 *
 * The resulting [Field] will not reflect the field options.
 *
 * @see toField
 */
public fun buildField(desc: FieldDescriptor): Field =
    field {
        val declaredIn = desc.containingType.name()
        name = desc.name()
        orderOfDeclaration = desc.index
        doc = desc.fileDoc.forField(desc)
        number = desc.number
        declaringType = declaredIn
        copyTypeAndCardinality(desc)
    }

/**
 * Converts the field type and cardinality (`map`/`list`/`oneof_name`/`single`) from
 * the given descriptor to the receiver DSL-style builder.
 */
private fun FieldKt.Dsl.copyTypeAndCardinality(
    desc: FieldDescriptor
) {
    if (desc.isMapField) {
        val (keyField, valueField) = desc.messageType.fields
        map = ofMap { keyType = keyField.primitiveType() }
        type = valueField.type()
    } else {
        type = desc.type()
        when {
            desc.isRepeated -> list = Empty.getDefaultInstance()
            desc.realContainingOneof != null -> oneofName = desc.realContainingOneof.name()
            else -> single = Empty.getDefaultInstance()
        }
    }
}

/**
 * Converts this enum value descriptor into an [EnumConstant] with options.
 *
 * @see buildConstant
 */
public fun EnumValueDescriptor.toEnumConstant(declaringType: TypeName): EnumConstant {
    val constant = buildConstant(this, declaringType)
    return constant.copy {
        option.addAll(options.toList())
    }
}

/**
 * Converts this enum value descriptor into an [EnumConstant].
 *
 * The resulting [EnumConstant] will not reflect the options on the enum constant.
 *
 * @see toEnumConstant
 */
public fun buildConstant(desc: EnumValueDescriptor, declaringType: TypeName): EnumConstant =
    enumConstant {
        name = constantName { value = desc.name }
        declaredIn = declaringType
        number = desc.number
        orderOfDeclaration = desc.index
        doc = desc.fileDoc.forEnumConstant(desc)
    }

/**
 * Converts this method descriptor into an [Rpc] with options.
 *
 * @see buildRpc
 */
public fun MethodDescriptor.toRpc(declaringService: ServiceName): Rpc {
    val rpc = buildRpc(this, declaringService)
    return rpc.copy {
        option.addAll(options.toList())
    }
}

/**
 * Converts this method descriptor into an [Rpc].
 *
 * The resulting [Rpc] will not reflect the method options.
 *
 * @see toRpc
 */
public fun buildRpc(
    desc: MethodDescriptor,
    declaringService: ServiceName
): Rpc = rpc {
    name = desc.name()
    cardinality = desc.cardinality
    requestType = desc.inputType.name()
    responseType = desc.outputType.name()
    doc = desc.fileDoc.forRpc(desc)
    service = declaringService
}

/**
 * Extracts metadata from this file descriptor, including file options.
 */
public fun FileDescriptor.toHeader(): ProtoFileHeader = protoFileHeader {
    file = file()
    packageName = `package`
    syntax = syntaxVersion()
    option.addAll(options.toList())
}

/**
 * Obtains the file path from this file descriptor.
 */
public fun FileDescriptorProto.toFile(): File = file {
    path = name
}

/**
 * Constructs a [Type] of the receiver field.
 */
public fun FieldDescriptor.type(): Type {
    return when (type) {
        ENUM -> enum(this)
        MESSAGE -> message(this)
        GROUP -> error("Cannot process the field `$fullName` of type `$type`.")
        else -> primitiveType().asType()
    }
}

/**
 * Converts this field type into an instance of [PrimitiveType] or throws an exception
 * if this type is not a primitive one.
 */
@Suppress("ComplexMethod") // ... not really, performing plain conversion.
public fun FieldDescriptor.Type.toPrimitiveType(): PrimitiveType =
    when (this) {
        BOOL -> TYPE_BOOL
        BYTES -> TYPE_BYTES
        DOUBLE -> TYPE_DOUBLE
        FIXED32 -> TYPE_FIXED32
        FIXED64 -> TYPE_FIXED64
        FLOAT -> TYPE_FLOAT
        INT32 -> TYPE_INT32
        INT64 -> TYPE_INT64
        SFIXED32 -> TYPE_SFIXED32
        SFIXED64 -> TYPE_SFIXED64
        SINT32 -> TYPE_SINT32
        SINT64 -> TYPE_SINT64
        STRING -> TYPE_STRING
        UINT32 -> TYPE_UINT32
        UINT64 -> TYPE_UINT64
        else -> error("`$this` is not a primitive type.")
    }

/**
 * Obtains the type of this field as a [PrimitiveType] or throws an exception
 * if the type is not a primitive one.
 */
public fun FieldDescriptor.primitiveType(): PrimitiveType = type.toPrimitiveType()

/**
 * Obtains the type of the given [field] as an enum type.
 */
private fun enum(field: FieldDescriptor): Type = type {
    enumeration = field.enumType.name()
}

/**
 * Obtains the type of the given [field] as a message type.
 */
private fun message(field: FieldDescriptor): Type = type {
    message = field.messageType.name()
}

/**
 * Obtains the syntax version of the given [FileDescriptor].
 */
public fun FileDescriptor.syntaxVersion(): SyntaxVersion =
    when (toProto().syntax) {
        "proto2" -> PROTO2
        "proto3" -> PROTO3
        else -> PROTO2
    }

/**
 * Yields events regarding a set of options.
 *
 * @param options
 *         the set of options, such as `FileOptions`, `FieldOptions`, etc.
 * @param factory
 *         a function which given an option, constructs a fitting event.
 */
public suspend fun SequenceScope<EventMessage>.produceOptionEvents(
    options: ExtendableMessage<*>,
    factory: (Option) -> EventMessage
) {
    parseOptions(options).forEach {
        yield(factory(it))
    }
}

/**
 * Parses this `options` message into a list of [Option]s.
 */
public fun ExtendableMessage<*>.toList(): List<Option> =
    parseOptions(this).toList()

private fun parseOptions(options: ExtendableMessage<*>): Sequence<Option> =
    sequence {
        options.allFields.forEach { (optionDescriptor, value) ->
            if (value is Collection<*>) {
                value.forEach {
                    val option = toOption(optionDescriptor, it!!)
                    yield(option)
                }
            } else {
                val option = toOption(optionDescriptor, value)
                yield(option)
            }
        }
    }

private fun toOption(
    optionDescriptor: FieldDescriptor,
    value: Any
): Option {
    val optionValue = fieldToAny(optionDescriptor, value)
    val option = option {
        name = optionDescriptor.name
        number = optionDescriptor.number
        type = optionDescriptor.type()
        this.value = optionValue
    }
    return option
}

private fun fieldToAny(field: FieldDescriptor, value: Any): ProtoAny =
    if (field.type == ENUM) {
        val descr = value as EnumValueDescriptor
        val enumValue = com.google.protobuf.enumValue {
            name = descr.name
            number = descr.number
        }
        enumValue.pack()
    } else {
        TypeConverter.toAny(value)
    }

/**
 * Converts this file descriptor to the instance of [ProtobufSourceFile].
 */
public fun FileDescriptor.toPbSourceFile(): ProtobufSourceFile {
    val path = file()
    val definitions = DefinitionFactory(this)
    return protobufSourceFile {
        file = path
        header = toHeader()
        with(definitions) {
            type.putAll(messageTypes().associateByUrl())
            enumType.putAll(enumTypes().associateByUrl())
            service.putAll(services().associateByUrl())
        }
    }
}

private fun <T : ProtoDeclaration> Sequence<T>.associateByUrl() =
    associateBy { it.name.typeUrl }

private val GenericDescriptor.fileDoc: Documentation
    get() = Documentation(file)

/**
 * Converts the receiver `Descriptor` into a [MessageType].
 */
public fun Descriptor.toMessageType(): MessageType =
    messageType {
        val docs = fileDoc
        val typeName = name()
        name = typeName
        file = getFile().file()
        doc = docs.forMessage(this@toMessageType)
        option.addAll(options.toList())
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        oneofGroup.addAll(realOneofs.map { it.toOneOfGroup() })
        field.addAll(fields.mapped())
        nestedMessages.addAll(nestedTypes.map { it.name() })
        nestedEnums.addAll(enumTypes.map { it.name() })
    }

/**
 * Converts this oneof descriptor to [OneofGroup].
 */
public fun OneofDescriptor.toOneOfGroup(): OneofGroup =
    oneofGroup {
        val docs = fileDoc
        val groupName = name()
        name = groupName
        field.addAll(fields.mapped())
        option.addAll(options.toList())
        doc = docs.forOneof(this@toOneOfGroup)
    }

private fun Iterable<FieldDescriptor>.mapped(): Iterable<Field> = map { it.toField() }

/**
 * Converts this enum descriptor into [EnumType] instance.
 */
public fun EnumDescriptor.toEnumType(): EnumType =
    enumType {
        val docs = fileDoc
        val typeName = name()
        name = typeName
        option.addAll(options.toList())
        file = getFile().file()
        constant.addAll(values.map { it.toEnumConstant(typeName) })
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        doc = docs.forEnum(this@toEnumType)
    }

/**
 * Converts this service descriptor into [Service] instance.
 */
public fun ServiceDescriptor.toService(): Service =
    service {
        val docs = fileDoc
        val serviceName = name()
        name = serviceName
        file = getFile().file()
        rpc.addAll(methods.map { it.toRpc(serviceName) })
        option.addAll(options.toList())
        doc = docs.forService(this@toService)
    }

/**
 * A factory of Protobuf definitions of a single `.proto` file.
 *
 * @property file
 *            the descriptor of the Protobuf file.
 */
private class DefinitionFactory(private val file: FileDescriptor) {

    /**
     * Builds the message type definitions from the [file].
     *
     * @return all the message types declared in the file, including nested types.
     */
    fun messageTypes(): Sequence<MessageType> {
        var messages = file.messageTypes.asSequence()
        for (msg in file.messageTypes) {
            messages += walkMessage(msg) { it.nestedTypes }
        }
        return messages.map { it.toMessageType() }
    }

    /**
     * Builds the enum type definitions from the [file].
     *
     * @return all the enums declared in the file, including nested enums.
     */
    fun enumTypes(): Sequence<EnumType> {
        var enums = file.enumTypes.asSequence()
        for (msg in file.messageTypes) {
            enums += walkMessage(msg) { it.enumTypes }
        }
        return enums.map { it.toEnumType() }
    }

    /**
     * Builds the service definitions from the [file].
     *
     * @return all the services declared in the file, including the nested ones.
     */
    fun services(): Sequence<Service> =
        file.services.asSequence().map { it.toService() }
}

/**
 * Produces a sequence by walking through all the nested message definitions staring with [type].
 *
 * @param type
 *         the message definition which may contain nested message definition to walk through.
 * @param extractorFun
 *         a function that, given a message definition, extracts the items of interest.
 * @return results of the calls to [extractorFun] flattened into one sequence.
 */
private fun <T> walkMessage(
    type: Descriptor,
    extractorFun: (Descriptor) -> Iterable<T>,
): Sequence<T> {
    val queue = ArrayDeque<Descriptor>()
    queue.add(type)
    return sequence {
        while (queue.isNotEmpty()) {
            val msg = queue.removeFirst()
            yieldAll(extractorFun(msg))
            queue.addAll(msg.nestedTypes)
        }
    }
}
