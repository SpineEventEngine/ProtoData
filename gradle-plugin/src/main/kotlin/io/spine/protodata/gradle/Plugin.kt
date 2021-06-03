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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.gradle.api.Plugin as GradlePlugin

/**
 * The ProtoData Gradle plugin.
 *
 * Adds the `installProtoData` task which installs the ProtoData executable onto
 * the current machine.
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
 */
public class Plugin : GradlePlugin<Project> {

    override fun apply(target: Project) {
        val extension = Extension(target)
        target.extensions.add("protoData", extension)
        val protoDataConfiguration = target.configurations.create("protoData")
        createEvalTask(target, extension, protoDataConfiguration)

        val resource = Plugin::class.java.classLoader.getResource("version.txt")!!
        val version = resource.readText()
        configureProtobufPlugin(target, extension, version)

        val protoDataRawConfiguration = target.configurations.create("protoDataRawArtifact")
        createInstallTask(target, protoDataRawConfiguration, version)
    }
}

/**
 * The `protoData { }` Gradle extension.
 */
@Suppress("UnstableApiUsage") // Gradle Property API.
public class Extension(private val project: Project) {

    internal  val plugins: ListProperty<String> =
        project.objects.listProperty(String::class.java)

    /**
     * Passes given names of Java classes to ProtoData as the `io.spine.protodata.plugin.Plugin`
     * classes.
     */
    public fun plugins(vararg classNames: String) {
        plugins.addAll(classNames.toList())
    }

    internal val renderers: ListProperty<String> =
        project.objects.listProperty(String::class.java)

    /**
     * Passes given names of Java classes to ProtoData as the `io.spine.protodata.renderer.Renderer`
     * classes.
     */
    public fun renderers(vararg classNames: String) {
        renderers.addAll(classNames.toList())
    }

    internal  val optionProviders: ListProperty<String> =
        project.objects.listProperty(String::class.java)

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.option.OptionsProvider` classes.
     */
    public fun optionProviders(vararg classNames: String) {
        optionProviders.addAll(classNames.toList())
    }

    internal val requestFileProperty: RegularFileProperty =
        project.objects.fileProperty().convention(
            project.layout.buildDirectory.file("protodata/request.bin")
        )

    /**
     * The location where to write and read the serialized `CodeGeneratorRequest`.
     *
     * The value accepted by this property it turned into a file via the Gradle's `Project.file(..)`
     * method.
     *
     * By default, the request file is stored in a subdir inside the `build` directory.
     */
    public var requestFile: Any
        get() = requestFileProperty.get()
        set(value) = requestFileProperty.set(project.file(value))

    internal val sourceProperty: DirectoryProperty =
        project.objects.directoryProperty().convention(
            project.layout.projectDirectory.dir("generated/main/java")
        )

    /**
     * The location where the sources processed by ProtoData can be found.
     *
     * By default, `generated/main/java`.
     */
    public var source: Any
        get() = sourceProperty.get()
        set(value) = sourceProperty.set(project.file(value))

    internal val generateProtoTasks: ListProperty<String> =
        project.objects.listProperty(String::class.java).convention(listOf("generateProto"))

    /**
     * Adds the names of Gradle tasks which generate the sources processed by ProtoData.
     *
     * By default, only `generateProto` task is considered.
     */
    public fun generateProtoTasks(vararg taskNames: String) {
        optionProviders.addAll(taskNames.toList())
    }
}

private const val EXECUTABLE = "protodata"
private const val PROTO_DATA_LOCATION = "protoDataLocation"
private const val PROTOC_PLUGIN = "protodata"

private fun createEvalTask(
    target: Project,
    extension: Extension,
    config: Configuration
): TaskProvider<*> =
    target.tasks.register("launchProtoData", Exec::class.java) { task ->
        task.dependsOn(config.buildDependencies)
        task.dependsOn(extension.generateProtoTasks)
        task.buildCommand(config, extension)

        target.tasks.withType<JavaCompile> { dependsOn(task) }
    }

private fun Project.protoDataExecutable(): String {
    val location = rootProject.property(PROTO_DATA_LOCATION) as String?
    return if (location == null) {
        EXECUTABLE
    } else {
        "$location/protodata/bin/$EXECUTABLE"
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
    command.add(extension.requestFileProperty.get().asFile.absolutePath)
    command.add("--src")
    command.add(extension.sourceProperty.asFile.get().absolutePath)
    if (userClassPath.isNotEmpty()) {
        command.add("--user-classpath")
        command.add(userClassPath)
    }
    commandLine(command)
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
        val location = target.rootProject.property(PROTO_DATA_LOCATION) as String?
        if (location != null) {
            val absoluteLocation = target.rootProject.file(location).absolutePath
            command.add(absoluteLocation)
        }
        it.commandLine(command)
        it.dependsOn(stageTask)
    }
}

private fun configureProtobufPlugin(target: Project, extension: Extension, version: String) {
    target.protobuf {
        plugins {
            id(PROTOC_PLUGIN) {
                artifact = "io.spine.protodata:protoc:$version:exe@jar"
            }
        }
        generateProtoTasks {
            all().forEach {
                it.plugins {
                    id(PROTOC_PLUGIN) {
                        val requestFile = extension.requestFileProperty.asFile.get().absolutePath
                        option(requestFile)
                    }
                }
            }
        }
    }
}
