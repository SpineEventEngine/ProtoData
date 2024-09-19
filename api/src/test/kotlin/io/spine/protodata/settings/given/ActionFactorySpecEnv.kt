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

package io.spine.protodata.settings.given

import com.google.protobuf.Empty
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import io.spine.base.EntityState
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.renderer.RenderAction
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.ActionFactory
import io.spine.protodata.type.TypeSystem
import io.spine.server.entity.Entity
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.query.QueryingClient
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin

/**
 * The class constructor of which matches the expectation of [RenderAction]
 */
@Suppress("unused") // To be called reflectively.
class JustMatchingConstructor(
    private val subject: MessageType,
    private val file: SourceFile<Java>,
    private val context: CodegenContext
)

/**
 * A stub render action implemented for Kotlin.
 */
class RenderInKotlin(
    type: MessageType,
    file: SourceFile<Kotlin>,
    context: CodegenContext
) : RenderAction<Kotlin, MessageType, Empty>(
    Kotlin,
    type,
    file,
    Empty.getDefaultInstance(),
    context
) {
    override fun render() {
        // Do nothing
    }
}

/**
 * Abstract base for render actions which exposes the [parameter] property for tests.
 *
 * It also implements the [render] method doing nothing for the brevity of descendants.
 */
abstract class ExposeParam<P: Message>(
    type: MessageType,
    file: SourceFile<Java>,
    param: P,
    context: CodegenContext
) : RenderAction<Java, MessageType, P>(Java, type, file, param, context) {

    /** Exposes the `parameter` property for tests. */
    fun param(): P {
        return parameter
    }

    override fun render() {
        // Do nothing
    }
}

/**
 * A stab render action implemented for Java which has no parameter.
 */
class ActionNoParam(
    type: MessageType,
    file: SourceFile<Java>,
    context: CodegenContext
) : ExposeParam<Empty>(type, file, Empty.getDefaultInstance(), context)

/**
 * A stab render action implemented for Java which has [StringValue] parameter.
 */
class ActionStringParams(
    type: MessageType,
    file: SourceFile<Java>,
    param: StringValue,
    context: CodegenContext
) : ExposeParam<StringValue>(type, file, param, context)

/**
 * A stub implementation of [CodegenContext] to be passed to [ActionFactory.create].
 */
class StubContext : CodegenContext {

    override val typeSystem: TypeSystem
        get() = TODO("Not yet implemented")

    override val insertionPointsContext: ThirdPartyContext
        get() = TODO("Not yet implemented")

    override fun <E : Entity<*, *>> hasEntitiesOfType(cls: Class<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun <P : EntityState<*>> select(type: Class<P>): QueryingClient<P> {
        TODO("Not yet implemented")
    }

    override fun close() = Unit

    override fun isOpen(): Boolean = false
}
