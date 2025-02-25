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

package io.spine.protodata.testing

import com.google.protobuf.Descriptors.Descriptor
import io.spine.logging.testing.tapConsole
import io.spine.protodata.Compilation
import io.spine.protodata.params.WorkingDirectory
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.testing.PipelineSetup.Companion.byResources
import io.spine.tools.code.SourceSetName
import java.nio.file.Path
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

/**
 * An abstract base for classes that test compilation errors.
 *
 * Note that there is no `assertCompilationPasses()` counterpart in this class
 * because we avoid creation of [PipelineSetup] for positive assertions.
 * Initialization and running of a pipeline is time-consuming. We do it here
 * for each assertion just because ProtoData is expected to fail anyway.
 *
 * For tests, where compilation is expected to pass, a single run of a pipeline
 * should be used for all sources. Positive tests usually have way more assertions.
 *
 * Another alternative is to create a standalone module just for tests, where all
 * messages-under-test are compiled at once by `launchTestProtoData` task without
 * a pipeline at all. Though, assertion of the generated source code is impossible
 * with this approach.
 */
public abstract class AbstractCompilationErrorTest {

    /**
     * A directory to be used by the compilation process.
     */
    @TempDir
    protected lateinit var workingDir: Path

    /**
     * The name of a source set under which the tested Proto declarations reside.
     *
     * By default, it is `testFixtures`.
     */
    protected open val protoSources: SourceSetName = SourceSetName("testFixtures")

    /**
     * List of ProtoData plugins to use in the compilation.
     */
    protected abstract fun plugins(): List<Plugin>

    /**
     * Writes plugin settings before the pipeline is created.
     *
     * The default implementation of this method does nothing.
     */
    protected open fun writeSettings(settings: SettingsDirectory) {
        // No settings by default.
    }

    /**
     * Asserts that the messages represented by the given [descriptor]
     * fails the compilation process.
     */
    public fun assertCompilationFails(descriptor: Descriptor): Compilation.Error {
        val setup = createSetup(descriptor)
        val pipeline = setup.createPipeline()
        val error = assertThrows<Compilation.Error> {
            // Redirect console output so that we don't print errors during the build.
            tapConsole {
                pipeline()
            }
        }
        return error
    }

    private fun createSetup(descriptor: Descriptor): PipelineSetup {
        val wd = WorkingDirectory(workingDir)
        val outputDir = workingDir.resolve("output")
            .also { it.toFile().mkdirs() }
        val params = pipelineParams {
            withRequestFile(wd.requestDirectory.file(protoSources))
            withSettingsDir(wd.settingsDirectory.path)
        }
        return byResources(
            params,
            plugins(),
            outputDir,
            acceptingOnly(descriptor),
            ::writeSettings
        )
    }
}
