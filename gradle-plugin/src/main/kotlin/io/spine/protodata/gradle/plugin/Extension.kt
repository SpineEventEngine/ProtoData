/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.protodata.gradle.CodeGeneratorRequestFile
import io.spine.protodata.gradle.CodeGeneratorRequestFile.DEFAULT_DIRECTORY
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.renderer.Custom
import io.spine.protodata.renderer.Default
import io.spine.tools.code.Java
import io.spine.tools.fs.DirectoryName.generated
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.protobuf.generatedSourceProtoDir
import kotlin.io.path.name
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.listProperty

/**
 * The `protoData { }` Gradle extension.
 */
public class Extension(internal val project: Project): CodegenSettings {

    public override fun plugins(vararg classNames: String): Unit =
        plugins.addAll(classNames.toList())

    private val factory = project.objects

    internal val plugins: ListProperty<String> =
        factory.listProperty(String::class.java).convention(listOf())

    @Deprecated("Supply Renderers via Plugins instead.")
    internal val renderers: ListProperty<String> =
        factory.listProperty<String>().convention(listOf())

    public override fun optionProviders(vararg classNames: String): Unit =
        optionProviders.addAll(classNames.toList())

    internal val optionProviders: ListProperty<String> =
        factory.listProperty<String>().convention(listOf())

    public override var requestFilesDir: Any
        get() = requestFilesDirProperty.get()
        set(value) = requestFilesDirProperty.set(project.file(value))

    internal val requestFilesDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(
            layout.buildDirectory.dir(DEFAULT_DIRECTORY)
        )
    }

    internal fun requestFile(forSourceSet: SourceSet): Provider<RegularFile> =
        requestFilesDirProperty.file(CodeGeneratorRequestFile.name(forSourceSet))

    public val paths: NamedDomainObjectContainer<SourcePaths> =
        project.objects.domainObjectContainer(SourcePaths::class) {
            name -> SourcePaths(generatorName = name)
        }

    internal fun pathsOrCompat(): Set<SourcePaths> {
        if (paths.isNotEmpty()) {
            return paths
        }
        return project.sourceSets.asSequence().flatMap {
            val srcRoots = sourceDirs(it).get()
            val targetRoots = targetDirs(it).get()
            srcRoots.zip(targetRoots)
        }.map { (src, target) ->
            val pathSuffix = src.asFile.toPath().name
            val generatorName = if (pathSuffix == "java") Default else Custom(pathSuffix)
            SourcePaths(
                src.asFile,
                target.asFile,
                Java,
                generatorName
            )
        }.toSet()
    }

    public companion object {

        /**
         * Default subdirectories expected by ProtoData under a generated source set.
         *
         * @see subDirs
         */
        public val defaultSubdirectories: List<String> = listOf(
            "java",
            "kotlin",
            "grpc",
            "js",
            "dart",
            "spine",
            "protodata"
        )
    }

    /**
     * Synthetic property for providing the source directories for the given
     * source set under [Project.generatedSourceProtoDir].
     *
     * @see sourceDirs
     */
    @Deprecated("Use `paths` instead.")
    private val srcBaseDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(provider {
            layout.projectDirectory.dir(generatedSourceProtoDir.toString())
        })
    }

    /**
     * Allows to configure the subdirectories under the generated source set.
     *
     * Defaults to [defaultSubdirectories].
     */
    @Deprecated("Use `paths` instead.")
    public override var subDirs: List<String>
        get() = subDirProperty.get()
        set(value) {
            if (value.isNotEmpty()) {
                subDirProperty.set(value)
            }

        }

    @Deprecated("Use `paths` instead.")
    private val subDirProperty: ListProperty<String> =
        factory.listProperty<String>().convention(defaultSubdirectories)

    @Deprecated("Use `paths` instead.")
    public override var targetBaseDir: Any
        get() = targetBaseDirProperty.get()
        set(value) = targetBaseDirProperty.set(project.file(value))

    @Deprecated("Use `paths` instead.")
    private val targetBaseDirProperty: DirectoryProperty = with(project) {
        objects.directoryProperty().convention(
            layout.projectDirectory.dir(generated.name)
        )
    }

    /**
     * Obtains the source directories for the given source set.
     */
    @Deprecated("Use `paths` instead.")
    internal fun sourceDirs(sourceSet: SourceSet): Provider<List<Directory>> =
        compileDir(sourceSet, srcBaseDirProperty)

    /**
     * Obtains the target directories for code generation.
     *
     * @see targetBaseDir for the rules for the target dir construction
     */
    @Deprecated("Use `paths` instead.")
    internal fun targetDirs(sourceSet: SourceSet): Provider<List<Directory>> =
        compileDir(sourceSet, targetBaseDirProperty)

    @Deprecated("Use `paths` instead.")
    private fun compileDir(sourceSet: SourceSet, base: DirectoryProperty): Provider<List<Directory>> {
        val sourceSetDir = base.dir(sourceSet.name)
        return sourceSetDir.map { root: Directory ->
            subDirs.map { root.dir(it) }
        }
    }
}
