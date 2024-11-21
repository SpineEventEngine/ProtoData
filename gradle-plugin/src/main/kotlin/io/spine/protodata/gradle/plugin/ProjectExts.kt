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

import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

/**
 * Obtains the name of the directory where ProtoData places generated files.
 */
internal val Project.targetBaseDir: String
    get() = extension.targetBaseDir.toString()

/**
 * Obtains the `generated` directory for the given [sourceSet] and a language.
 *
 * If the language is not given, the returned directory is the root directory for the source set.
 */
internal fun Project.generatedDir(sourceSet: SourceSet, language: String = ""): File {
    val path = "$targetBaseDir/${sourceSet.name}/$language"
    return File(path)
}

/**
 * Tells if this project can deal with Java code.
 *
 * @return `true` if `java` plugin is installed, `false` otherwise.
 */
internal fun Project.hasJava(): Boolean =
    pluginManager.hasPlugin("java")

/**
 * Tells if this project can deal with Kotlin code.
 *
 * @return `true` if `compileKotlin` or `compileTestKotlin` tasks are present, `false` otherwise.
 */
internal fun Project.hasKotlin(): Boolean {
    val compileKotlin = tasks.findByName("compileKotlin")
    val compileTestKotlin = tasks.findByName("compileTestKotlin")
    return compileKotlin != null || compileTestKotlin != null
}

/**
 * Verifies if the project can deal with Java or Kotlin code.
 *
 * The current Protobuf support of Kotlin is based on Java codegen.
 * Therefore, it is likely that Java would be enabled in the project for
 * Kotlin proto code to be generated.
 * Though, it may change someday, and Kotlin support for Protobuf would be
 * self-sufficient. This method assumes such a case when it checks the presence of
 * Kotlin compilation tasks.
 *
 * @see [hasJava]
 * @see [hasKotlin]
 */
internal  fun Project.hasJavaOrKotlin(): Boolean {
    if (hasJava()) {
        return true
    }
    return hasKotlin()
}

/**
 * Attempts to obtain the Java compilation Gradle task for the given source set.
 *
 * Typically, the task is named by a pattern: `compile<SourceSet name>Java`, or just `compileJava`
 * if the source set name is `"main"`. If the task does not fit this described pattern, this method
 * will not find it.
 */
internal fun Project.javaCompileFor(sourceSet: SourceSet): JavaCompile? {
    val taskName = sourceSet.compileJavaTaskName
    return tasks.findByName(taskName) as JavaCompile?
}

/**
 * Attempts to obtain the Kotlin compilation Gradle task for the given source set.
 *
 * Typically, the task is named by a pattern: `compile<SourceSet name>Kotlin`, or just
 * `compileKotlin` if the source set name is `"main"`. If the task does not fit this described
 * pattern, this method will not find it.
 */
internal fun Project.kotlinCompileFor(sourceSet: SourceSet): KotlinCompile<*>? {
    val taskName = sourceSet.getCompileTaskName("Kotlin")
    return tasks.findByName(taskName) as KotlinCompile<*>?
}
