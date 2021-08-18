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

package io.spine.protodata.codegen.java.generado

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.protodata.Pipeline
import io.spine.protodata.codegen.java.JAVA_FILE
import io.spine.protodata.codegen.java.WithSourceSet
import io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration
import kotlin.io.path.Path
import org.junit.jupiter.api.Test

class `'GenerateGenerated' renderer should` : WithSourceSet() {

    @Test
    fun `add the annotation`() {
        Pipeline(
            plugins = listOf(),
            renderers = listOf(PrintBeforePrimaryDeclaration(), GenerateGenerated()),
            sourceSet = sourceSet,
            request = CodeGeneratorRequest.getDefaultInstance()
        )()
        val code = sourceSet
            .file(Path(JAVA_FILE))
            .code()
        assertThat(code)
            .contains("""
                @javax.annotation.Generated("${GenerateGenerated.GENERATORS}")
            """.trimIndent())
    }
}
