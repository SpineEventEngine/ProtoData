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

import io.spine.internal.dependency.Dokka
import io.spine.internal.gradle.publish.getOrCreate
import java.io.File
import java.time.LocalDate
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder

/**
 * To generate the documentation as seen from Java perspective, the `kotlin-as-java`
 * plugin was added to the Dokka classpath.
 *
 * @see <a href="https://github.com/Kotlin/dokka#output-formats">
 *     Dokka output formats</a>
 */
fun DependencyHandlerScope.useDokkaForKotlinAsJava() {
    dokkaPlugin(Dokka.KotlinAsJavaPlugin.lib)
}

/**
 * To exclude pieces of code annotated with `@Internal` from the documentation
 * a custom plugin is added to the Dokka classpath.
 *
 * @see <a href="https://github.com/SpineEventEngine/dokka-tools/tree/master/dokka-extensions">
 *     Custom Dokka Plugins</a>
 */
fun DependencyHandlerScope.useDokkaWithSpineExtensions() {
    dokkaPlugin(Dokka.SpineExtensions.lib)
}

private fun DependencyHandler.dokkaPlugin(dependencyNotation: Any): Dependency? =
    add("dokkaPlugin", dependencyNotation)

private fun Project.dokkaOutput(language: String): File =
    buildDir.resolve("docs/dokka${language.capitalized()}")

fun Project.dokkaConfigFile(file: String): File {
    val dokkaConfDir = project.rootDir.resolve("buildSrc/src/main/resources/dokka")
    return dokkaConfDir.resolve(file)
}

/**
 * Configures the presentation style, logo, and footer message.
 *
 * Dokka Base plugin allows setting a few properties to customize the output:
 * - `customStyleSheets` property to which CSS files are passed overriding
 *  styles generated by Dokka;
 * - `customAssets` property to provide resources. The image with the name
 *  "logo-icon.svg" is passed to override the default logo used by Dokka;
 * - `separateInheritedMembers` when set to `true`, creates a separate tab in
 *  type-documentation for inherited members.
 *
 * @see <a href="https://kotlin.github.io/dokka/1.8.10/user_guide/base-specific/frontend/#prerequisites">
 *  Dokka modifying frontend assets</a>
 */
fun AbstractDokkaTask.configureStyle() {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        customStyleSheets = listOf(project.dokkaConfigFile("styles/custom-styles.css"))
        customAssets = listOf(project.dokkaConfigFile("assets/logo-icon.svg"))
        separateInheritedMembers = true
        footerMessage = "Copyright ${LocalDate.now().year}, TeamDev"
    }
}
private fun AbstractDokkaLeafTask.configureFor(language: String) {
    dokkaSourceSets.configureEach {
        /**
         * Configures links to the external Java documentation.
         */
        jdkVersion.set(BuildSettings.javaVersion.asInt())

        skipEmptyPackages.set(true)

        includeNonPublic.set(true)

        documentedVisibilities.set(
            setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
            )
        )
    }

    outputDirectory.set(project.dokkaOutput(language))

    configureStyle()
}

/**
 * Configures this [DokkaTask] to accept only Kotlin files.
 */
fun AbstractDokkaLeafTask.configureForKotlin() {
    configureFor("kotlin")
}

/**
 * Configures this [DokkaTask] to accept only Java files.
 */
fun AbstractDokkaLeafTask.configureForJava() {
    configureFor("java")
}

/**
 * Finds the `dokkaHtml` Gradle task.
 */
fun TaskContainer.dokkaHtmlTask(): DokkaTask? = this.findByName("dokkaHtml") as DokkaTask?

/**
 * Returns only Java source roots out of all present in the source set.
 *
 * It is a helper method for generating documentation by Dokka only for Java code.
 * It is helpful when both Java and Kotlin source files are present in a source set.
 * Dokka can properly generate documentation for either Kotlin or Java depending on
 * the configuration, but not both.
 */
@Suppress("unused")
internal fun GradleDokkaSourceSetBuilder.onlyJavaSources(): FileCollection {
    return sourceRoots.filter(File::isJavaSourceDirectory)
}

private fun File.isJavaSourceDirectory(): Boolean {
    return isDirectory && name == "java"
}

/**
 * Locates or creates `dokkaKotlinJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains the Dokka output, generated upon
 * Kotlin sources from `main` source set. Requires Dokka to be configured in the target project by
 * applying `dokka-for-kotlin` plugin.
 */
fun Project.dokkaKotlinJar(): TaskProvider<Jar> = tasks.getOrCreate("dokkaKotlinJar") {
    archiveClassifier.set("dokka")
    from(files(dokkaOutput("kotlin")))

    tasks.dokkaHtmlTask()?.let{ dokkaTask ->
        this@getOrCreate.dependsOn(dokkaTask)
    }
}

/**
 * Tells if this task belongs to the execution graph which contains publishing tasks.
 *
 * The task `"publishToMavenLocal"` is excluded from the check because it is a part of
 * the local testing workflow.
 */
fun DokkaTask.isInPublishingGraph(): Boolean =
    project.gradle.taskGraph.allTasks.any {
        with(it.name) {
            startsWith("publish") && !startsWith("publishToMavenLocal")
        }
    }

/**
 * Locates or creates `dokkaJavaJar` task in this [Project].
 *
 * The output of this task is a `jar` archive. The archive contains the Dokka output, generated upon
 * Kotlin sources from `main` source set. Requires Dokka to be configured in the target project by
 * applying `dokka-for-java` and/or `dokka-for-kotlin` script plugin.
 */
fun Project.dokkaJavaJar(): TaskProvider<Jar> = tasks.getOrCreate("dokkaJavaJar") {
    archiveClassifier.set("dokka-java")
    from(files(dokkaOutput("java")))

    tasks.dokkaHtmlTask()?.let{ dokkaTask ->
        this@getOrCreate.dependsOn(dokkaTask)
    }
}

/**
 * Disables Dokka and Javadoc tasks in this `Project`.
 *
 * This function could be useful to improve build speed when building subprojects containing
 * test environments or integration test projects.
 */
@Suppress("unused")
fun Project.disableDocumentationTasks() {
    gradle.taskGraph.whenReady {
        tasks.forEach { task ->
            val lowercaseName = task.name.toLowerCase()
            if (lowercaseName.contains("dokka") || lowercaseName.contains("javadoc")) {
                task.enabled = false
            }
        }
    }
}
