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

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.gradle.ProtobufPlugin
import io.spine.tools.gradle.project.sourceSets
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Plugin configuration should")
class ProjectConfigSpec {

    companion object {

        private lateinit var project: Project

        @Suppress("unused") // False positive: JUnit calls this method.
        @BeforeAll
        @JvmStatic
        fun prepareProject(@TempDir projectDir: File) {
            project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
            with(project) {
                apply(plugin = "java")
                apply<ProtobufPlugin>()
                apply<Plugin>()
                repositories.mavenLocal()
            }
            with(project.sourceSets) {
                maybeCreate(MAIN_SOURCE_SET_NAME)
                maybeCreate(TEST_SOURCE_SET_NAME)
            }
        }
    }

    @Test
    fun `add 'install' and 'launch' tasks to the project`() {
        println(project.tasks.map { it.name })
        assertThat(project.tasks)
            .comparingElementsUsing(taskNames)
            .containsAtLeast("launchProtoData", "launchTestProtoData")
    }

    @Test
    fun `add extension`() {
        val assertExtension = assertThat(project.extensions.getByName("protoData"))
        assertExtension
            .isNotNull()
        assertExtension
            .isInstanceOf(Extension::class.java)
    }

    @Test
    fun `bind 'launchProtoData' to Java compilation`() {
        val task = project.tasks.getByName("compileJava")
        assertThat(task.dependsOn)
            .comparingElementsUsing(taskNames)
            .contains("launchProtoData")
    }
}

private val taskNames: Correspondence<Any, String> =
    correspondence("is task named") { something, name ->
        something is Task && something.name == name!!
    }

private fun <I, O> correspondence(
    description: String,
    predicate: Correspondence.BinaryPredicate<I, O>
): Correspondence<I, O> =
    Correspondence.from(predicate, description)
