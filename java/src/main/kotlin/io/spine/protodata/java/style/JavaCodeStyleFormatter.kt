/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.java.style

import io.spine.protodata.java.JavaRenderer
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.settings.defaultConsumerId
import io.spine.protodata.settings.loadSettings
import io.spine.tools.psi.codeStyleManager
import io.spine.tools.psi.codeStyleSettings
import io.spine.tools.psi.force
import io.spine.tools.psi.java.Environment
import io.spine.tools.psi.java.FileSystem
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.javaCodeStyleSettings

/**
 * Reformats Java source code files using settings passed as [JavaCodeStyle] instance.
 *
 * If no settings are passed, default Java code style settings used in Spine SDK are applied.
 * 
 * @see javaCodeStyleDefaults
 */
public class JavaCodeStyleFormatter : JavaRenderer() {

    override val consumerId: String = defaultConsumerId

    private val project = Environment.project

    private val codeStyle: JavaCodeStyle by lazy {
        if (settingsAvailable()) {
            loadSettings<JavaCodeStyle>()
        } else {
            javaCodeStyleDefaults()
        }
    }

    private var appliedToIntelliJ: Boolean = false

    private fun applyStyleToIntelliJ() {
        if (!appliedToIntelliJ) {
            codeStyle.run {
                applyTo(project.codeStyleSettings)
                applyTo(project.javaCodeStyleSettings)
            }
            project.force(project.codeStyleSettings)
            appliedToIntelliJ = true
        }
    }

    override fun render(sources: SourceFileSet) {
        applyStyleToIntelliJ()
        sources.forEach {
            reformat(it)
        }
    }

    private fun reformat(file: SourceFile) {
        val psiFile = FileSystem.load(file.outputPath.toFile())
        execute {
            project.codeStyleManager.reformat(psiFile)
        }
        val updatedCode = psiFile.text
        file.overwrite(updatedCode)
    }

    public companion object {

        /**
         * The ID to be used when passing settings to [JavaCodeStyleFormatter].
         */
        public val defaultConsumerId: String = JavaCodeStyle::class.java.defaultConsumerId
    }

}
