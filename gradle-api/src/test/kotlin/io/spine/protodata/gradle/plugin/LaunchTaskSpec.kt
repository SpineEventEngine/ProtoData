/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import groovy.lang.Closure
import io.kotest.matchers.shouldBe
import io.spine.protodata.gradle.LaunchTask
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`LaunchTask` should")
class LaunchTaskSpec {

    @Test
    fun `provide a task name for a source set`() {
        LaunchTask.nameFor(mainSourceSet) shouldBe "launchProtoData"
        LaunchTask.nameFor(testSourceSet) shouldBe "launchTestProtoData"
        LaunchTask.nameFor(functionalTestsSourceSet) shouldBe "launchFunctionalTestsProtoData"
    }
}

val mainSourceSet = StubSourceSet(SourceSet.MAIN_SOURCE_SET_NAME)
val testSourceSet = StubSourceSet(SourceSet.TEST_SOURCE_SET_NAME)
val functionalTestsSourceSet = StubSourceSet("functionalTests")

class StubSourceSet(private val name: String) : SourceSet {

    override fun getName(): String = name

    private fun stubMethod(): Nothing = throw NotImplementedError(
        "Stub method called. If you need this method for your tests, please create" +
                " a class derived from ${this::class.qualifiedName} and override the method."
    )

    override fun getExtensions(): ExtensionContainer = stubMethod()

    override fun getCompileClasspath(): FileCollection = stubMethod()

    override fun setCompileClasspath(classpath: FileCollection): Unit = stubMethod()

    override fun getAnnotationProcessorPath(): FileCollection = stubMethod()

    override fun setAnnotationProcessorPath(annotationProcessorPath: FileCollection): Unit =
        stubMethod()

    override fun getRuntimeClasspath(): FileCollection = stubMethod()

    override fun setRuntimeClasspath(classpath: FileCollection): Unit = stubMethod()

    override fun getOutput(): SourceSetOutput = stubMethod()

    override fun compiledBy(vararg taskPaths: Any?): SourceSet = stubMethod()

    override fun getResources(): SourceDirectorySet = stubMethod()

    override fun resources(configureClosure: Closure<*>?): SourceSet = stubMethod()

    override fun resources(configureAction: Action<in SourceDirectorySet>): SourceSet =
        stubMethod()

    override fun getJava(): SourceDirectorySet = stubMethod()

    override fun java(configureClosure: Closure<*>?): SourceSet = stubMethod()

    override fun java(configureAction: Action<in SourceDirectorySet>): SourceSet = stubMethod()

    override fun getAllJava(): SourceDirectorySet = stubMethod()

    override fun getAllSource(): SourceDirectorySet = stubMethod()

    override fun getClassesTaskName(): String = stubMethod()

    override fun getProcessResourcesTaskName(): String = stubMethod()

    override fun getCompileJavaTaskName(): String = stubMethod()

    override fun getCompileTaskName(language: String): String = stubMethod()

    override fun getJavadocTaskName(): String = stubMethod()

    override fun getJarTaskName(): String = stubMethod()

    override fun getJavadocJarTaskName(): String = stubMethod()

    override fun getSourcesJarTaskName(): String = stubMethod()

    override fun getTaskName(verb: String?, target: String?): String = stubMethod()

    override fun getCompileOnlyConfigurationName(): String = stubMethod()

    override fun getCompileOnlyApiConfigurationName(): String = stubMethod()

    override fun getCompileClasspathConfigurationName(): String = stubMethod()

    override fun getAnnotationProcessorConfigurationName(): String = stubMethod()

    override fun getApiConfigurationName(): String = stubMethod()

    override fun getImplementationConfigurationName(): String = stubMethod()

    override fun getApiElementsConfigurationName(): String = stubMethod()

    override fun getRuntimeOnlyConfigurationName(): String = stubMethod()

    override fun getRuntimeClasspathConfigurationName(): String = stubMethod()

    override fun getRuntimeElementsConfigurationName(): String = stubMethod()

    override fun getJavadocElementsConfigurationName(): String = stubMethod()

    override fun getSourcesElementsConfigurationName(): String = stubMethod()
}
