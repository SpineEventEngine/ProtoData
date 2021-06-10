/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getPlugin

private const val EXECUTABLE = "protodata"

/**
 * The name of the Gradle property pointing at the custom ProtoData installation location.
 */
internal const val PROTO_DATA_LOCATION = "protoDataLocation"

/**
 * The [sourceSets][SourceSetContainer] of this project.
 */
internal val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin<JavaPluginConvention>().sourceSets

/**
 * Attempts to obtain the Java compilation Gradle task for the given source set.
 *
 * Typically, the task is named by a pattern: `compile<SourceSet name>Java`, or just `compileJava`
 * if the source set name is `"main"`. If the task does not fit this described pattern, this method
 * will not find it.
 */
internal fun Project.javaCompileFor(sourceSet: SourceSet): JavaCompile? {
    val name = sourceSet.name
    val javaCompileInfix = if (name == SourceSet.MAIN_SOURCE_SET_NAME) "" else name.capitalize()
    val javaCompileName = "compile${javaCompileInfix}Java"
    return tasks.findByName(javaCompileName) as JavaCompile?
}

/**
 * Obtains the ProtoData executable for this project.
 *
 * If the [protoDataLocation] property is defined, uses in to locate the executable.
 * Otherwise, obtains the name of the executable with hopes that it is in the `PATH`.
 */
internal fun Project.protoDataExecutable(): String {
    val location = protoDataLocation
    return if (location == null) {
        EXECUTABLE
    } else {
        "$location/protodata/bin/$EXECUTABLE"
    }
}

/**
 * The `protoDataLocation` Gradle property.
 *
 * Points at the custom installation location for ProtoData.
 *
 * The value is `null` if the property is not defined. Explicitly setting the property to `null`
 * is the same as not defining it at all.
 */
internal val Project.protoDataLocation: String?
    get() {
        if (!rootProject.hasProperty(PROTO_DATA_LOCATION)) {
            return null
        }
        return rootProject.property(PROTO_DATA_LOCATION) as String?
    }
