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

import com.google.protobuf.gradle.ProtobufConvention
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

/**
 * The `protoData { }` Gradle extension.
 */
@Suppress("UnstableApiUsage") // Gradle Property API.
public class Extension(private val project: Project) {

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.plugin.Plugin` classes.
     */
    public fun plugins(vararg classNames: String) {
        plugins.addAll(classNames.toList())
    }

    internal val plugins: ListProperty<String> =
        project.objects.listProperty(String::class.java).convention(listOf())

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.renderer.Renderer` classes.
     */
    public fun renderers(vararg classNames: String) {
        renderers.addAll(classNames.toList())
    }

    internal val renderers: ListProperty<String> =
        project.objects.listProperty<String>().convention(listOf())

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.option.OptionsProvider` classes.
     */
    public fun optionProviders(vararg classNames: String) {
        optionProviders.addAll(classNames.toList())
    }

    internal val optionProviders: ListProperty<String> =
        project.objects.listProperty<String>().convention(listOf())

    /**
     * Passes the given names of Protobuf files which declare custom options to ProtoData.
     */
    public fun options(vararg protoFiles: String) {
        options.addAll(protoFiles.toList())
    }

    internal val options: ListProperty<String> =
        project.objects.listProperty<String>().convention(listOf())

    /**
     * A directory where the serialized `CodeGeneratorRequest`s are stored.
     *
     * For each source set, we generate a separate request file. Files are named after
     * the associated source set with the `.bin` extension.
     */
    public var requestFilesDir: Any
        get() = requestFilesDirProperty.get()
        set(value) = requestFilesDirProperty.set(project.file(value))

    internal val requestFilesDirProperty: DirectoryProperty =
        project.objects.directoryProperty().convention(
            project.layout.buildDirectory.dir("protodata/requests")
        )

    internal fun requestFile(forSourceSet: SourceSet): Provider<RegularFile> =
        requestFilesDirProperty.file("${forSourceSet.name}.bin")

    /**
     * The base directory where the files generated from Protobuf resides.
     *
     * Files are placed under this dir and divided under sub-directories by source sets, and then by
     * the [subDir]s. For example, a Java class generated from a main-scope `.proto` definition
     * would be placed under `$srcBaseDir/main/java`, where `main` is the name of the source set
     * and `java` is the [subDir].
     *
     * By default, `srcBaseDir` points to the directory which is specified in
     * `protobuf.generatedFilesBaseDir` to the Protobuf Gradle plugin. If `srcBaseDir` is changed,
     * Protobuf compiler settings are NOT affected.
     *
     * To change the location where `protoc` stores files it generates, refer to
     * the Protobuf Gradle plugin DSL.
     *
     * @see subDir
     */
    public var srcBaseDir: Any
        get() = srcBaseDirProperty.get()
        set(value) = srcBaseDirProperty.set(project.file(value))

    private val srcBaseDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(provider {
            val protobuf = convention.getPlugin<ProtobufConvention>().protobuf
            layout.projectDirectory.dir(protobuf.generatedFilesBaseDir)
        })
    }

    /**
     * The sub-directory to which the files generated from Protobuf are placed.
     *
     * The default value is `"java"`.
     *
     * @see srcBaseDir
     */
    public var subDir: String
        get() = subDirProperty.get()
        set(value) {
            if (value.isNotEmpty()) {
                subDirProperty.set(value)
            }
        }

    private val subDirProperty: Property<String> =
        project.objects.property<String>().convention("java")

    /**
     * The base directory where the files generated by ProtoData are placed.
     *
     * By default, points at the `$projectDir/generated/` directory.
     *
     * @see srcBaseDir
     */
    public var targetBaseDir: Any
        get() = targetBaseDirProperty.get()
        set(value) = targetBaseDirProperty.set(project.file(value))

    private val targetBaseDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(
            layout.projectDirectory.dir("generated")
        )
    }

    /**
     * Obtains the source directory for the given source set.
     *
     * @see srcBaseDir for the rules for the source dir construction
     */
    internal fun sourceDir(sourceSet: SourceSet): Provider<Directory> =
        srcBaseDirProperty.get().dir(sourceSet.name).dir(subDirProperty)

    /**
     * Obtains the target directory for code generation.
     *
     * @see targetBaseDir for the rules for the target dir construction
     */
    internal fun targetDir(sourceSet: SourceSet): Provider<Directory> =
        targetBaseDirProperty.get().dir(sourceSet.name).dir(subDirProperty)
}
