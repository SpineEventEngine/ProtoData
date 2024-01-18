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

import io.spine.protodata.settings.event.SettingsFileDiscovered
import io.spine.protodata.settings.event.settingsFileDiscovered
import io.spine.protodata.toProto
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries

/**
 * A directory containing settings files.
 *
 * Only the files with the [recognized extensions][Format] are considered settings files.
 * Only the files directly in the directory are considered, no subdirectories are traversed.
 */
public class SettingsDirectory(
    private val directory: Path
) {
    init {
        require(directory.toFile().isDirectory) {
            "The path `$directory` is not a directory."
        }
        require(directory.exists()) {
            "The directory `$directory` does not exist."
        }
    }

    /**
     * Writes settings file for the given consumer.
     *
     * @param consumerId the ID of the consumer to write settings for.
     * @param format the format of the settings file.
     * @param content the content of the settings file.
     */
    public fun write(consumerId: String, format: Format, content: String) {
        val file = file(consumerId, format)
        file.toFile().writeText(content)
    }

    /**
     * Writes settings file for the given consumer.
     *
     * @param T the type of the settings consumer.
     * @param format the format of the settings file.
     * @param content the content of the settings file.
     */
    public inline fun <reified T: LoadsSettings> writeFor(format: Format, content: String) {
        write(T::class.java.defaultConsumerId, format, content)
    }

    /**
     * Writes settings file for the given consumer.
     *
     * @param consumerId the ID of the consumer to write settings for.
     * @param format the format of the settings file.
     * @param content the content of the settings file.
     */
    public fun write(consumerId: String, format: Format, content: ByteArray) {
        val file = file(consumerId, format)
        file.toFile().writeBytes(content)
    }

    /**
     * Writes settings file for the given consumer.
     *
     * @param T the type of the settings consumer.
     * @param format the format of the settings file.
     * @param content the content of the settings file.
     */
    public inline fun <reified T: LoadsSettings> writeFor(format: Format, content: ByteArray) {
        write(T::class.java.defaultConsumerId, format, content)
    }

    private fun file(consumerId: String, format: Format): Path {
        val fileName = "${consumerId}.${format.extensions.first()}"
        return directory.resolve(fileName)
    }

    /**
     * Traverses the setting files in this directory and emits
     * [SettingsFileDiscovered] for each of them.
     */
    public fun emitEvents(): List<SettingsFileDiscovered> =
        files().map {
            settingsFileDiscovered {
                file = it.toProto()
            }
        }

    private fun files() =
        directory.listDirectoryEntries()
            .filter { it.isSettings() }
}
