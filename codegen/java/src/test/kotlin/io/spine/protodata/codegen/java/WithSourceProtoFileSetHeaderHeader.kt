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

package io.spine.protodata.codegen.java

import io.spine.protodata.renderer.SourceFileSet
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

internal const val JAVA_FILE = "java/org/example/Test.java"

/**
 * A base for test cases that require a source file set with a Java file to run.
 */
internal open class WithSourceProtoFileSetHeaderHeader protected constructor() {

    protected lateinit var sources: List<SourceFileSet>
        private set

    @BeforeEach
    fun createSourceSet(@TempDir path: Path) {
        val sourceRoot = path.resolve("source")
        val targetRoot = path.resolve("target")
        val sourceFile = sourceRoot.resolve(JAVA_FILE)
        val contents = javaClass.classLoader.getResource(JAVA_FILE)!!.readText()
        sourceFile.parent.toFile().mkdirs()
        sourceFile.writeText(contents, options = arrayOf(StandardOpenOption.CREATE_NEW))
        sources = listOf(SourceFileSet.create(sourceRoot, targetRoot))
    }
}
