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

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.Plugin as GradlePlugin

/**
 * The ProtoData Gradle plugin.
 *
 * Adds the `launchProtoData` task which runs the executable with the arguments assembled from
 * the configuration of this plugin.
 *
 * The users can submit configuration parameters, such as renderer and plugin class names, etc. via
 * the `protoData { }` extension.
 *
 * The users can submit the user classpath to the ProtoData by declaring dependencies using
 * the `protoData` configuration.
 *
 * Example:
 * ```
 * protoData {
 *     renderers("com.acme.MyRenderer")
 *     plugins("com.acme.MyPlugin")
 * }
 *
 * dependencies {
 *     protoData(project(":my-plugin"))
 * }
 * ```
 *
 * The plugin also adds the `installProtoData` task which installs the ProtoData executable onto
 * the current machine. If the user does not have the ProtoData installed on their local machine,
 * they can do that by simply running `installProtoData` task.
 */
public class Plugin : GradlePlugin<Project> {

    override fun apply(target: Project) {
        val extension = Extension(target)
        target.extensions.add("protoData", extension)
        val protoDataConfiguration = target.configurations.create("protoData")
        target.sourceSets.addRule("ProtoData task per source set") { name ->
            val launch = createLaunchTask(target, extension, protoDataConfiguration, name)
            target.javaCompileForSourceSet(name)?.dependsOn(launch)
        }

        val resource = Plugin::class.java.classLoader.getResource("version.txt")!!
        val version = resource.readText()
        target.configureProtobufPlugin(extension, version)

        val protoDataRawConfiguration = target.configurations.create("protoDataRawArtifact")
        createInstallTask(target, protoDataRawConfiguration, version)
    }
}

private const val PROTOC_PLUGIN = "protodata"

private fun createLaunchTask(
    target: Project,
    ext: Extension,
    config: Configuration,
    sourceSetName: String
): Task {
    val taskName = launchTaskName(sourceSetName)
    return target.tasks.create(taskName, LaunchProtoData::class.java) { task ->
        with(task) {
            dependsOn(config.buildDependencies)

            protoDataExecutable = project.protoDataExecutable()
            renderers = ext.renderers.get()
            plugins = ext.plugins.get()
            optionProviders = ext.optionProviders.get()
            requestFile = ext.requestFile(sourceSetName)
            source = with(ext) {
                srcBaseDirProperty.get().dir(sourceSetName).dir(srcSubdirProperty).get()
            }
            config.resolve()
            userClasspath = config.asPath

            compileCommandLine()
        }
    }
}

private fun createInstallTask(target: Project, config: Configuration, version: String) {
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
        val protoDataLocation = target.rootProject.protoDataLocation
        if (protoDataLocation != null) {
            val absoluteLocation = target.rootProject.file(protoDataLocation).absolutePath
            command.add(absoluteLocation)
        }
        it.commandLine(command)
        it.dependsOn(stageTask)
    }
}

private fun Project.configureProtobufPlugin(extension: Extension, version: String) =
    protobuf {
        plugins {
            id(PROTOC_PLUGIN) {
                artifact = "io.spine.protodata:protoc:$version:exe@jar"
            }
        }
        generateProtoTasks {
            all().forEach {
                val sourceSet = it.sourceSet.name
                it.plugins {
                    id(PROTOC_PLUGIN) {
                        val requestFile = extension.requestFile(sourceSet)
                        option(requestFile.asFile.absolutePath)
                    }
                }
                project.tasks.getByName(launchTaskName(sourceSet)).dependsOn(it)
            }
        }
    }

private fun launchTaskName(sourceSetName: String): String =
    "launchProtoData${sourceSetName.capitalize()}"
