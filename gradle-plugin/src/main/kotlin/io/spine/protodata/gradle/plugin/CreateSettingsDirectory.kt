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

package io.spine.protodata.gradle.plugin

import io.spine.protodata.gradle.Directories.PROTODATA_WORKING_DIR
import io.spine.protodata.gradle.Directories.SETTINGS_SUBDIR
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public abstract class CreateSettingsDirectory: DefaultTask() {

    @get:OutputDirectory
    public abstract val settingsDir: DirectoryProperty

    init {
        @Suppress("LeakingThis") // As advised by Gradle docs.
        settingsDir.convention(
            project.layout.buildDirectory.dir("$PROTODATA_WORKING_DIR/$SETTINGS_SUBDIR")
        )
    }

    @TaskAction
    internal fun createDirectory() {
        project.ensureSettingsDirExists(settingsDir.get().asFile)
    }
}

/**
 * Ensures that the settings directory exists.
 *
 * ProtoData CLI expects that the directory exists.
 * ProtoData may be configured to run without settings, e.g., when running tests.
 *
 * Normally, there will be a task that writes settings for ProtoData, and `LaunchProtoData`
 * task would depend on this task.
 *
 * This function handles the case when the directory is missed.
 * If the directory does not exist, it creates it performing logging operations
 * using the project logger.
 */
private fun Project.ensureSettingsDirExists(settingsDirectory: File) {
    if (!settingsDirectory.exists()) {
        if (settingsDirectory.mkdirs()) {
            logger.warn(
                "The ProtoData settings directory has been created: `{}`.",
                settingsDirectory
            )
        } else {
            logger.error(
                "Unable to create the ProtoData settings directory: `{}`.",
                settingsDirectory)
        }
    }
}
