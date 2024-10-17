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

package io.spine.protodata.render

import io.kotest.matchers.shouldBe
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.type.TypeSystem
import io.spine.tools.code.AnyLanguage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

@DisplayName("`Renderer` should")
internal class RendererSpec {

    /**
     * A stub instance of `"Code Generation"` context to be used when needed in parameters.
     *
     * We do not pass any custom entity classes or repositories to the context builder,
     * as we do not need them in tests.
     */
    private lateinit var context: CodegenContext

    private lateinit var renderer: StubRenderer
    private lateinit var typeSystem: TypeSystem

    @BeforeEach
    fun initEnvironment() {
        context = CodeGenerationContext.newInstance()
        typeSystem = context.typeSystem
        renderer = StubRenderer()
        renderer.registerWith(context)
    }

    @AfterEach
    fun closeContext() {
        context.close()
    }

    @Test
    fun `inject type system`() {
        renderer.typeSystem() shouldBe typeSystem
    }

    @Test
    fun `allow repeated injection of the same type system`() {
        assertDoesNotThrow {
            renderer.registerWith(context)
        }
    }

    @Test
    fun `prevent injecting another type system`() {
        val anotherContext = CodeGenerationContext.newInstance()
        anotherContext.use {
            assertThrows<IllegalStateException> {
                renderer.registerWith(it)
            }
        }
    }
}

private class StubRenderer : Renderer<AnyLanguage>(AnyLanguage) {

    /**
     * Opens the access to [typeSystem] property for being checked in tests.
     */
    fun typeSystem(): TypeSystem? = typeSystem

    override fun render(sources: SourceFileSet) = Unit
}
