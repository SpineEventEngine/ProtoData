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

package io.spine.protodata.protobuf

import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.protodata.ast.Service
import io.spine.protodata.ast.ServiceName
import io.spine.protodata.ast.coordinates
import io.spine.protodata.ast.documentation
import io.spine.protodata.ast.service
import io.spine.protodata.ast.serviceName
import io.spine.protodata.ast.toList

/**
 * Obtains the name of this service as a [ServiceName].
 */
public fun ServiceDescriptor.name(): ServiceName = serviceName {
    typeUrlPrefix = file.typeUrlPrefix
    packageName = file.`package`
    simpleName = name
}

/**
 * Converts this service descriptor into [Service] instance.
 */
public fun ServiceDescriptor.toService(): Service =
    service {
        val serviceName = name()
        name = serviceName
        file = getFile().file()
        rpc.addAll(methods.map { it.toRpc(serviceName) })
        option.addAll(options.toList())
        doc = documentation().forService(this@toService)
        span = coordinates().forService(this@toService)
    }
