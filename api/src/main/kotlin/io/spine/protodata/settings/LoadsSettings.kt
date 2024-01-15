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

package io.spine.protodata.settings

import com.google.common.io.CharSource
import com.google.common.io.Files.asByteSource
import io.spine.annotation.Internal
import io.spine.protodata.ConfigurationError
import io.spine.protodata.File
import io.spine.protodata.settings.Config.KindCase.EMPTY
import io.spine.protodata.settings.Config.KindCase.FILE
import io.spine.protodata.settings.Config.KindCase.KIND_NOT_SET
import io.spine.protodata.settings.Config.KindCase.TEXT
import io.spine.protodata.toPath
import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.util.theOnly
import java.nio.charset.Charset.defaultCharset

/**
 * A ProtoData component which accesses its settings via the [Config] view.
 */
@Internal
public interface LoadsSettings : Querying, WithSettings {

    override fun <T: Any> loadSettings(cls: Class<T>): T {
        val allSettings = select<Config>().all()
        if (allSettings.isEmpty()) {
            missingSettings(cls)
        }
        //TODO:2024-01-15:alexander.yevsyukov: Obtain settings for the given class.
        val settings = allSettings.theOnly()
        return when (settings.kindCase!!) {
            FILE -> parseFile(settings.file, cls)
            TEXT -> parseRaw(settings.text, cls)
            EMPTY, KIND_NOT_SET -> missingSettings(cls)
        }
    }

    override fun settingsAvailable(): Boolean {
        //TODO:2024-01-15:alexander.yevsyukov: Query for the settings corresponding to this
        // ProtoData component.
        val settings = select<Config>().all()
        return settings.isNotEmpty()
    }
}

private fun missingSettings(expectedType: Class<*>): Nothing {
    throw ConfigurationError("No configuration provided. Expected `${expectedType.canonicalName}`.")
}

private fun <T> parseFile(file: File, cls: Class<T>): T {
    val path = file.toPath()
    val format = formatOf(path)
    val bytes = asByteSource(path.toFile())
    return format.parser.parse(bytes, cls)
}

private fun <T> parseRaw(config: Text, cls: Class<T>): T {
    val bytes = CharSource
        .wrap(config.value)
        .asByteSource(defaultCharset())
    return config.format.parser.parse(bytes, cls)
}
