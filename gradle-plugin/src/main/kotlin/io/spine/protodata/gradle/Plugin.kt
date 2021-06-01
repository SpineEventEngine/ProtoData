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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.Plugin as GradlePlugin

public class Plugin : GradlePlugin<Project> {

    override fun apply(target: Project) {
        val extension = Extension(target)
        target.extensions.add("protoData", extension)
        val protoDataConfiguration = target.configurations.create("protoData")
        createEvalTask(target, extension, protoDataConfiguration)

        val protoDataRawConfiguration = target.configurations.create("protoDataRawArtifact")
        createInstallTask(target, protoDataRawConfiguration)
    }
}

@Suppress("UnstableApiUsage") // Gradle Property API.
public class Extension(project: Project) {

    public val plugins: ListProperty<String> =
        project.objects.listProperty(String::class.java)
    public val renderers: ListProperty<String> =
        project.objects.listProperty(String::class.java)
    public val optionProviders: ListProperty<String> =
        project.objects.listProperty(String::class.java)
    public val requestFile: RegularFileProperty =
        project.objects.fileProperty().convention(
            project.layout.buildDirectory.file("protodata/request.bin")
        )
    public val source: DirectoryProperty =
        project.objects.directoryProperty().convention(
            project.layout.projectDirectory.dir("generated/main/java")
        )
    public val generateProtoTasks: ListProperty<String> =
        project.objects.listProperty(String::class.java).convention(listOf("generateProto"))
}

private const val EXECUTABLE = "protodata"
private const val PROTO_DATA_LOCATION = "protoDataLocation"

private fun createEvalTask(
    target: Project,
    extension: Extension,
    config: Configuration
): TaskProvider<*> =
    target.tasks.register("launchProtoData", Exec::class.java) { task ->
        task.dependsOn(config.buildDependencies)
        task.dependsOn(extension.generateProtoTasks)
        task.buildCommand(config, extension)
    }

private fun Project.protoDataExecutable(): String {
    val location = rootProject.property(PROTO_DATA_LOCATION) as String?
    return if (location == null) {
        EXECUTABLE
    } else {
        "$location/protodata/bin/protodata"
    }
}

private fun Exec.buildCommand(config: Configuration,
                              extension: Extension) {
    config.resolve()
    val userClassPath = config.asPath
    val command = mutableListOf(project.protoDataExecutable())
    if (extension.plugins.isPresent) {
        extension.plugins.get().forEach {
            command.add("--plugin")
            command.add(it)
        }
    }
    if (extension.renderers.isPresent) {
        extension.renderers.get().forEach {
            command.add("--renderer")
            command.add(it)
        }
    }
    if (extension.optionProviders.isPresent) {
        extension.optionProviders.get().forEach {
            command.add("--options")
            command.add(it)
        }
    }
    command.add("--request")
    command.add(extension.requestFile.get().asFile.absolutePath)
    command.add("--src")
    command.add(extension.source.asFile.get().absolutePath)
    if (userClassPath.isNotEmpty()) {
        command.add("--user-classpath")
        command.add(userClassPath)
    }
    commandLine(command)
}

private fun createInstallTask(target: Project, config: Configuration) {
    val resource = Plugin::class.java.classLoader.getResource("version.txt")!!
    val version = resource.readText()
    val dependency = target.dependencies.add(config.name,
                                             "io.spine.protodata:executable:${version}")
    val stagingDir = "${target.buildDir}/protodata/staging"
    val stageTask = target.tasks.register("stageProtoData", Copy::class.java) {
        val artifact = config.files(dependency).first().absoluteFile
        it.from(target.zipTree(artifact))
        it.into(stagingDir)
    }

    target.tasks.register("installProtoData", Exec::class.java) {
        val command = mutableListOf("$stagingDir/install.sh")
        val location = target.rootProject.property(PROTO_DATA_LOCATION) as String?
        if (location != null) {
            val absoluteLocation = target.rootProject.file(location).absolutePath
            command.add(absoluteLocation)
        }
        it.commandLine(command)
        it.dependsOn(stageTask)
    }
}
