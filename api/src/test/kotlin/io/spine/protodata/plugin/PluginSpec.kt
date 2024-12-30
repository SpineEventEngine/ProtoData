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

package io.spine.protodata.plugin

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.spine.protodata.ast.event.FieldEntered
import io.spine.protodata.ast.event.FieldExited
import io.spine.protodata.backend.CodeGenerationContext
import io.spine.protodata.backend.Pipeline
import io.spine.protodata.protobuf.ProtoFileList
import io.spine.protodata.testing.pipelineParams
import io.spine.protodata.testing.withRoots
import io.spine.protodata.type.TypeSystem
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder
import io.spine.server.event.Just
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import java.nio.file.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("`Plugin` should")
internal class PluginSpec {

    private lateinit var policy1: StubPolicy1
    private lateinit var policy2: StubPolicy2
    private lateinit var plugin: Plugin

    @BeforeEach
    fun createStubs() {
        policy1 = StubPolicy1()
        policy2 = StubPolicy2()
        plugin = StubPlugin(policy1, policy2)
    }

    @Test
    fun `propagate 'TypeSystem' into its policies`() {
        val ctx = BoundedContext.singleTenant("Stubs")
        val typeSystem = TypeSystem(ProtoFileList(emptyList()), emptySet())
        plugin.applyTo(ctx, typeSystem)
        policy1.typeSystem() shouldBe typeSystem
        policy2.typeSystem() shouldBe typeSystem
    }

    @Test
    fun `register policies with a 'CodegenContext'`(
        @TempDir src: Path,
        @TempDir target: Path
    ) {
        runPipeline(src, target)

        policy1.context() shouldNotBe null
        policy1.context().name().value shouldStartWith CodeGenerationContext.NAME_PREFIX
        policy2.context() shouldBe policy1.context()
    }

    @Test
    fun `extend a given context via its builder`(
        @TempDir src: Path,
        @TempDir target: Path
    ) {
        runPipeline(src, target)
        (plugin as StubPlugin).contextBuilder shouldNotBe null
    }

    private fun runPipeline(src: Path, target: Path) {
        val params = pipelineParams {
            withRoots(src, target)
        }
        val pipeline = Pipeline(
            params,
            plugin,
        )
        pipeline()
    }
}

private class StubPlugin(vararg policies: Policy<*>) : Plugin(policies = policies.toSet()) {

    lateinit var contextBuilder: BoundedContextBuilder

    override fun extend(context: BoundedContextBuilder) {
        super.extend(context)
        contextBuilder = context
    }
}

private class StubPolicy1 : TsStubPolicy<FieldEntered>() {
    @React
    override fun whenever(event: FieldEntered): Just<NoReaction> = Just.noReaction
}

private class StubPolicy2 : TsStubPolicy<FieldExited>() {
    @React
    override fun whenever(event: FieldExited): Just<NoReaction> = Just.noReaction
}
