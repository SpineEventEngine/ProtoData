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

package io.spine.protodata.backend

import io.spine.core.External
import io.spine.core.Subscribe
import io.spine.protodata.ast.nameWithoutExtension
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.protodata.settings.Settings
import io.spine.protodata.settings.event.SettingsFileDiscovered
import io.spine.server.entity.alter
import io.spine.server.route.EventRouting

/**
 * A view on the ProtoData user configuration.
 *
 * Can contain either a configuration file path or a string value of the configuration.
 *
 * @see io.spine.protodata.settings.WithSettings for fetching the value of the user configuration.
 */
internal class SettingsView : View<String, Settings, Settings.Builder>() {

    @Subscribe
    internal fun on(@External event: SettingsFileDiscovered) = alter {
        file = event.file
    }

    /**
     * A repository for the `ConfigView`.
     */
    internal class Repo : ViewRepository<String, SettingsView, Settings>() {

        override fun setupEventRouting(routing: EventRouting<String>) {
            super.setupEventRouting(routing)
            routing.unicast<SettingsFileDiscovered> { e, _ ->
                e.file.nameWithoutExtension
            }
        }
    }
}
