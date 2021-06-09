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
internal const val PROTO_DATA_LOCATION = "protoDataLocation"

internal val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin<JavaPluginConvention>().sourceSets

internal fun Project.javaCompileForSourceSet(name: String): JavaCompile? {
    val javaCompileInfix = if (name == SourceSet.MAIN_SOURCE_SET_NAME) "" else name.capitalize()
    val javaCompileName = "compile${javaCompileInfix}Java"
    return tasks.findByName(javaCompileName) as JavaCompile?
}

internal fun Project.protoDataExecutable(): String {
    val location = protoDataLocation
    return if (location == null) {
        EXECUTABLE
    } else {
        "$location/protodata/bin/$EXECUTABLE"
    }
}

internal val Project.protoDataLocation: String?
    get() {
        if (!rootProject.hasProperty(PROTO_DATA_LOCATION)) {
            return null
        }
        return rootProject.property(PROTO_DATA_LOCATION) as String?
    }
