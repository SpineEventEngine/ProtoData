/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.protodata.codegen.java.file

import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import io.spine.server.Closeable
import io.spine.tools.psi.IdeaStandaloneExecution
import io.spine.tools.psi.java.Parser
import io.spine.tools.psi.java.PsiJavaAppEnvironment

/**
 * Parsing environment for Java files based on IntelliJ PSI.
 */
public object PsiJavaParser: Closeable {

    private var project: Project? = null
    private var rootDisposable: Disposable? = null

    /**
     * Obtains the instance of the [Parser].
     */
    public val instance: Parser by lazy {
        createEnvironment()
        Parser(project!!)
    }

    private fun createEnvironment() {
        IdeaStandaloneExecution.setup()
        rootDisposable = Disposer.newDisposable()
        val appEnvironment = PsiJavaAppEnvironment.create(rootDisposable!!)
        val environment = JavaCoreProjectEnvironment(rootDisposable!!, appEnvironment)
        project = environment.project
    }

    override fun close() {
        if (isOpen) {
            rootDisposable!!.dispose()
            rootDisposable = null
            project = null
        }
    }

    override fun isOpen(): Boolean {
        return rootDisposable != null
    }
}
