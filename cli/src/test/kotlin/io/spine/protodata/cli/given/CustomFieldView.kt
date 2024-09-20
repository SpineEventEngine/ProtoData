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

package io.spine.protodata.cli.given

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.cli.test.CustomField
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.alter
import io.spine.server.route.EventRoute.withId
import io.spine.server.route.EventRouting

class CustomFieldView : View<FieldName, CustomField, CustomField.Builder>() {

    @Subscribe
    internal fun on(@External @Where(field = "option.name", equals = "custom")
                        event: FieldOptionDiscovered
    ) = alter {
        field = event.field
    }

    class Repository : ViewRepository<FieldName, CustomFieldView, CustomField>() {

        override fun setupEventRouting(routing: EventRouting<FieldName>) {
            super.setupEventRouting(routing)
            routing.route(FieldOptionDiscovered::class.java) { event, _ ->
                withId(event.field)
            }
        }
    }
}
