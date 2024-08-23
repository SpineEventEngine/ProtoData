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

package io.spine.protodata.settings

import com.google.protobuf.Any
import com.google.protobuf.Empty
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import com.google.protobuf.stringValue
import io.spine.protobuf.pack
import io.spine.protodata.ProtoDeclaration
import io.spine.protodata.renderer.RenderAction
import io.spine.tools.code.Language
import io.spine.tools.kotlin.reference
import kotlin.reflect.KClass

/**
 * Adds an action which accepts the given [parameter].
 *
 * @param L The language served by the action.
 * @param D The Protobuf declaration handled by the action.
 * @param P The type of the parameters passed to the action.
 *
 * @param cls The class of the render action.
 * @param parameter The parameter passed to the action.
 */
public fun <A : RenderAction<L, D, P>, L : Language, D : ProtoDeclaration, P : Message>
        ActionsKt.Dsl.add(cls: KClass<out A>, parameter: P) {
    action.put(cls.reference, parameter.pack())
}

/**
 * Adds an action which accepts a [StringValue] parameter.
 *
 * @param L The language served by the action.
 * @param D The Protobuf declaration handled by the action.
 *
 * @param cls The class of the render action.
 * @param parameter The [value][StringValue.getValue] of the parameter to be passed to the action.
 */
public fun <A : RenderAction<L, D, StringValue>, L : Language, D : ProtoDeclaration>
        ActionsKt.Dsl.add(cls: KClass<out A>, parameter: String) {
    val value = stringValue { value = parameter }
    add(cls, value)
}

/**
 * Adds an action which has no parameter.
 *
 * @param L The language served by the action.
 * @param D The Protobuf declaration handled by the action.
 *
 * @param cls The class of the render action.
 */
public fun <A : RenderAction<L, D, Empty>, L : Language, D : ProtoDeclaration>
        ActionsKt.Dsl.add(cls: KClass<out A>) {
    action.put(cls.reference, Any.getDefaultInstance())
}

/**
 * Adds an action which accepts the given [parameter].
 *
 * @param L The language served by the action.
 * @param D The Protobuf declaration handled by the action.
 * @param P The type of the parameters passed to the action.
 *
 * @param cls The class of the render action.
 * @param parameter The parameter passed to the action.
 */
public fun <A : RenderAction<L, D, P>, L : Language, D : ProtoDeclaration, P : Message>
        ActionsKt.Dsl.add(cls: Class<out A>, parameter: P) {
    add(cls.kotlin, parameter)
}

/**
 * Adds an action which accepts a [StringValue] parameter.
 *
 * @param L The language served by the action.
 * @param D The Protobuf declaration handled by the action.
 *
 * @param cls The class of the render action.
 * @param parameter The [value][StringValue.getValue] of the parameter to be passed to the action.
 */
public fun <A : RenderAction<L, D, StringValue>, L : Language, D : ProtoDeclaration>
        ActionsKt.Dsl.add(cls: Class<out A>, parameter: String) {
    add(cls.kotlin, parameter)
}

/**
 * Adds an action which has no parameter.
 *
 * @param L The language served by the action.
 * @param D The Protobuf declaration handled by the action.
 *
 * @param cls The class of the render action.
 */
public fun <A : RenderAction<L, D, Empty>, L : Language, D : ProtoDeclaration>
        ActionsKt.Dsl.add(cls: Class<out A>) {
    add(cls.kotlin)
}
