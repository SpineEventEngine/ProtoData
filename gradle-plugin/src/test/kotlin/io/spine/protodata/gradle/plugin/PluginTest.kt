/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.truth.Truth.assertThat
import io.spine.protodata.gradle.Names.GRADLE_PLUGIN_ID
import io.spine.testing.SlowTest
import io.spine.tools.gradle.task.TaskName
import io.spine.tools.gradle.testing.GradleProject
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SKIPPED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@SlowTest
class `ProtoData Gradle plugin should` {

    private val taskName: TaskName = TaskName { "launchProtoDataMain" }

    private lateinit var projectDir: File
    private lateinit var project: GradleProject

    @BeforeEach
    fun prepareDir(@TempDir projectDir: File) {
        this.projectDir = projectDir
    }

    @Test
    fun `skip launch task if request file does not exist`() {
        createEmptyProject()
        launchAndExpectResult(SKIPPED)
    }

    @Test
    fun `launch ProtoData`() {
        createProjectWithProto()
        launchAndExpectResult(SUCCESS)
    }

    @Test
    fun `configure incremental compilation for launch task`() {
        createProjectWithProto()
        launchAndExpectResult(SUCCESS)
        launchAndExpectResult(UP_TO_DATE)
    }

    private fun createEmptyProject() {
        createProject("empty-test")
    }

    private fun createProjectWithProto() {
        createProject("launch-test")
    }

    private fun launchAndExpectResult(outcome: TaskOutcome) {
        val result = launch()
        assertThat(result.task(taskName.path())!!.outcome)
            .isEqualTo(outcome)
    }

    private fun launch(): BuildResult =
        project.executeTask(taskName)

    private fun createProject(resourceDir: String) {
        val version = Plugin.readVersion()
        val builder = GradleProject.setupAt(projectDir)
            .fromResources(resourceDir)
            .replace("@PROTODATA_PLUGIN_ID@", GRADLE_PLUGIN_ID)
            .replace("@PROTODATA_VERSION@", version)
            //.enableRunnerDebug()
            .copyBuildSrc()
        project = builder.create()
    }
}
