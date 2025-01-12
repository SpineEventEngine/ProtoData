/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.protodata.test

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.update
import io.spine.server.route.Route

/**
 * A view of a message type that should not be represented by a Java class.
 */
public class DeletedTypeView : View<TypeName, DeletedType, DeletedType.Builder>() {

    @Subscribe
    internal fun to(@External event: TypeDiscovered) {
        update {
            name = event.type.name
            type = event.type
        }
    }

    internal companion object {

        @Route
        @JvmStatic
        fun on(e: TypeDiscovered): Set<TypeName> {
            val name = e.type.name
            return if (name.simpleName.endsWith("_")) {
                setOf(name)
            } else {
                setOf()
            }
        }
    }
}

/**
 * This class of the repository is used for tests checking the case when both
 * view class and a repository class are passed to the plugin constructor.
 */
public class DeletedTypeRepository
    : ViewRepository<TypeName, DeletedTypeView, DeletedType>()
