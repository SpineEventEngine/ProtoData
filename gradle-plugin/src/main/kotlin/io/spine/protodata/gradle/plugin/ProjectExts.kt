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

package io.spine.protodata.gradle.plugin

import io.spine.protodata.gradle.protoDataWorkingDir
import io.spine.tools.code.Language
import io.spine.tools.gradle.project.findJavaCompileFor
import io.spine.tools.gradle.project.findKotlinCompileFor
import io.spine.tools.gradle.project.hasCompileTask
import io.spine.tools.gradle.project.hasJava
import io.spine.tools.gradle.project.hasJavaOrKotlin
import io.spine.tools.gradle.project.hasKotlin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Obtains the directory where ProtoData stores its temporary files.
 */
@Deprecated(
    "Please use `io.spine.protodata.gradle.protoDataWorkingDir` instead.",
    ReplaceWith(
        "protoDataWorkingDir",
        imports = arrayOf("io.spine.protodata.gradle.protoDataWorkingDir")
    )
)
public val Project.protoDataWorkingDir: Directory
    get() = protoDataWorkingDir

/**
 * Tells if this project can deal with Java code.
 *
 * @return `true` if `java` plugin is installed, `false` otherwise.
 */
@Deprecated(
    "Please use `io.spine.tools.gradle.project.hasJava()` instead.",
    ReplaceWith("hasJava()", imports = arrayOf("io.spine.tools.gradle.project.hasJava"))
)
public fun Project.hasJava(): Boolean = hasJava()

/**
 * Tells if this project can deal with Kotlin code.
 *
 * @return `true` if any of the tasks starts with `"compile"` and ends with `"Kotlin"`.
 */
@Deprecated(
    "Please use `io.spine.tools.gradle.project.hasKotlin()` instead.",
    ReplaceWith("hasJava()", imports = arrayOf("io.spine.tools.gradle.project.hasKotlin"))
)
public fun Project.hasKotlin(): Boolean = hasKotlin()

/**
 * Tells if this project has a compile task for the given language.
 */
@Deprecated(
    "Please use `io.spine.tools.gradle.project.hasCompileTask(Language)` instead.",
    ReplaceWith(
        "hasCompileTask(language)",
        imports = arrayOf("io.spine.tools.gradle.project.hasCompileTask")
    )
)
public fun Project.hasCompileTask(language: Language): Boolean = hasCompileTask(language)

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
@Deprecated(
    "Please use `io.spine.tools.gradle.project.hasJavaOrKotlin()` instead.",
    ReplaceWith(
        "hasJavaOrKotlin()",
        imports = arrayOf("io.spine.tools.gradle.project.hasJavaOrKotlin")
    )
)
public fun Project.hasJavaOrKotlin(): Boolean = hasJavaOrKotlin()

/**
 * Attempts to obtain the Java compilation Gradle task for the given source set.
 *
 * Typically, the task is named by a pattern: `compile<SourceSet name>Java`, or just `compileJava`
 * if the source set name is `"main"`. If the task does not fit this described pattern, this method
 * will not find it.
 */
@Deprecated(
    "Please use `io.spine.tools.gradle.project.javaCompileFor()` instead.",
    ReplaceWith(
        "javaCompileFor(sourceSet)",
        imports = arrayOf("io.spine.tools.gradle.project.findJavaCompileFor")
    )
)
public fun Project.javaCompileFor(sourceSet: SourceSet): JavaCompile? =
    findJavaCompileFor(sourceSet)

/**
 * Attempts to obtain the Kotlin compilation Gradle task for the given source set.
 *
 * Typically, the task is named by a pattern: `compile<SourceSet name>Kotlin`, or just
 * `compileKotlin` if the source set name is `"main"`. If the task does not fit this described
 * pattern, this method will not find it.
 */
@Deprecated(
    "Please use `io.spine.tools.gradle.project.kotlinCompileFor()` instead.",
    ReplaceWith(
        "kotlinCompileFor(sourceSet)",
        imports = arrayOf("io.spine.tools.gradle.project.findKotlinCompileFor")
    )
)
public fun Project.kotlinCompileFor(sourceSet: SourceSet): KotlinCompilationTask<*>? =
    findKotlinCompileFor(sourceSet)
