/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.tools.gradle.protobuf.generatedDir
import io.spine.tools.gradle.protobuf.generatedSourceProtoDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaModule

/**
 * Ensures that the sources generated by Protobuf Gradle plugin are
 * not included in the IDEA project.
 *
 * IDEA should only see the sources generated by ProtoData as
 * we define in [GenerateProtoTask.configureSourceSetDirs].
 */
internal fun Project.configureIdea() {
    val thisProject = this
    gradle.afterProject {
        if (it == thisProject) {
            pluginManager.withPlugin("idea") {
                val idea = extensions.getByType<IdeaModel>()
                idea.module.setupDirectories(thisProject)
            }
        }
    }
}

private fun IdeaModule.setupDirectories(project: Project,) {

    fun filterSources(sources: Set<File>, excludeDir: File): Set<File> =
        sources.filter { !it.residesIn(excludeDir) }.toSet()

    val protocOutput = project.file(project.generatedSourceProtoDir)
    val protocTargets = project.protocTargets()
    excludeWithNested(protocOutput.toPath(), protocTargets)
    sourceDirs = filterSources(sourceDirs, protocOutput)
    testSources.filter { !it.residesIn(protocOutput) }
    generatedSourceDirs = project.generatedDir.resolve(protocTargets)
        .map { it.toFile() }
        .toSet()
}

/**
 * Obtains the path of the `generated` directory under the project root directory.
 */
private val Project.generatedDir: Path
    get() = projectDir.resolve(targetBaseDir).toPath()

/**
 * Lists target directories for Protobuf code generation.
 *
 * The directory names are in the following format:
 *
 * `<source-set-name>/<builtIn-or-plugin-name>`
 */
private fun Project.protocTargets(): List<Path> {
    val protobufTasks = tasks.withType(GenerateProtoTask::class.java)
    val codegenTargets = sequence {
        protobufTasks.forEach { task ->
            val sourceSet = task.sourceSet.name
            val builtins = task.builtins.map { builtin -> builtin.name }
            val plugins = task.plugins.map { plugin -> plugin.name }
            val combined = builtins + plugins
            combined.forEach { subdir ->
                yield(Paths.get(sourceSet, subdir))
            }
        }
    }
    return codegenTargets.toList()
}

/**
 * Excludes the given directory and its subdirectories from
 * being seen as ones with the source code.
 *
 * The primary use of this extension is to exclude `build/generated/source/proto` and its
 * subdirectories to avoid duplication of types in the generated code with those in
 * produced by ProtoData under the `$projectDir/generated/` directory.
 */
private fun IdeaModule.excludeWithNested(directory: Path, subdirs: Iterable<Path>) {
    excludeDirs.add(directory.toFile())
    directory.resolve(subdirs).forEach {
        excludeDirs.add(it.toFile())
    }
}

private fun Path.resolve(subdirs: Iterable<Path>): List<Path> =
    subdirs.map {
        resolve(it)
    }

/**
 * Tells if this file resides in the given [directory].
 */
internal fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.absolutePath)
