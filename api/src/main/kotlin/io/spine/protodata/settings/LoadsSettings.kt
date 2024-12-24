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

import io.spine.protodata.ast.File
import io.spine.protodata.ast.toPath
import io.spine.protodata.settings.Settings.KindCase.EMPTY
import io.spine.protodata.settings.Settings.KindCase.FILE
import io.spine.protodata.settings.Settings.KindCase.KIND_NOT_SET
import io.spine.server.query.Querying
import io.spine.server.query.select

/**
 * A ProtoData plugin component which accesses its settings via the [Settings] view.
 */
public interface LoadsSettings : Querying, WithSettings {

    /**
     * The ID of the settings consumer which is used for [loading settings][loadSettings].
     *
     * The default value is a canonical name of the Java class implementing this interface.
     */
    public val consumerId: String
        get() = this::class.java.defaultConsumerId

    override fun <T: Any> loadSettings(cls: Class<T>): T {
        val settings = findSettings() ?: missingSettings()
        return settings.parse(cls)
    }

    override fun settingsAvailable(): Boolean {
        val settings = findSettings()
        return settings != null
    }

    private fun findSettings(): Settings? {
        val settings = select<Settings>().findById(consumerId)
        return settings
    }
}

private fun LoadsSettings.missingSettings(): Nothing {
    error("Could not find settings for `$consumerId`.")
}

/**
 * Loads settings with the type specified by the generic parameter [T].
 */
public inline fun <reified T: Any> LoadsSettings.loadSettings(): T =
    loadSettings(T::class.java)

/**
 * Obtains the default ID of the settings consumer which is used
 * for [loading settings][LoadsSettings.loadSettings].
 *
 * @receiver the class which implements [LoadsSettings].
 *           The class is not bound to avoid casting at the usage sites.
 * @return the canonical name of the given class.
 */
public val Class<*>.defaultConsumerId: String
    get() = canonicalName

/**
 * Parses this instance of [Settings] into the given class.
 */
private fun <T : Any> Settings.parse(cls: Class<T>): T =
    when (kindCase!!) {
        FILE -> parseFile(file, cls)
        EMPTY, KIND_NOT_SET -> unknownCase(cls)
    }

private fun <T : Any> parseFile(file: File, cls: Class<T>): T =
    io.spine.protodata.util.parseFile(file.toPath().toFile(), cls)

private fun Settings.unknownCase(cls: Class<*>): Nothing {
    error("Unable to parse settings as `${cls.canonicalName}`. `kindCase` is `$kindCase`.")
}
