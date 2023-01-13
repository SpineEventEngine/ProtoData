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

package io.spine.protodata.gradle

import io.spine.tools.code.SourceSetName
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

/**
 * Launches the ProtoData command-line utility from a Gradle project.
 */
public object LaunchTask {

    /**
     * Obtains a name of the task for the given source set.
     */
    public fun nameFor(sourceSet: SourceSet): String {
        val sourceSetName = SourceSetName(sourceSet.name)
        return "launch${sourceSetName.toInfix()}ProtoData"
    }

    /**
     * Obtains an instance of the task in the given project for the specified source set.
     */
    public fun get(project: Project, sourceSet: SourceSet): Task {
        val name = nameFor(sourceSet)
        return project.tasks.getByName(name)
    }

    /**
     * Obtains an instance of the task in the given project for the specified source set.
     *
     * @return the task or `null` if there is no task created for this source set
     */
    public fun find(project: Project, sourceSet: SourceSet): Task? {
        val name = nameFor(sourceSet)
        return project.tasks.findByName(name)
    }
}
