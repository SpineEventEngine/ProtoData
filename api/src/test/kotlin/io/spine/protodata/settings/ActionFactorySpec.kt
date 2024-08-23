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

package io.spine.protodata.settings

import io.kotest.matchers.string.shouldContain
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.renderer.RenderAction
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.settings.given.JustMatchingConstructor
import io.spine.protodata.settings.given.RendererInKotlin
import io.spine.protodata.settings.given.StubContext
import io.spine.protodata.toMessageType
import io.spine.tools.code.Java
import io.spine.tools.code.Kotlin
import io.spine.tools.kotlin.reference
import kotlin.io.path.Path
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.google.protobuf.Any as ProtoAny

@DisplayName("`ActionFactory` should")
internal class ActionFactorySpec {

    private val classLoader = javaClass.classLoader

    /**
     * A stub instance of [MessageType] for passing to [ActionFactory.create].
     */
    private val messageType = ProtoAny.getDescriptor().toMessageType()

    /**
     * A stub instance of [SourceFile] to be passed to [ActionFactory.create].
     */
    private val sourceFile = SourceFile.byCode<Java>(
        Path("MyClass.java"),
        """
        public class MyClass {
        }
        """.trimIndent()
    )

    /**
     * A stub instance of [CodegenContext] to be passed to [ActionFactory.create].
     */
    private val stubContext = StubContext()

    @Test
    fun `prohibit empty 'Actions' instance`() {
        assertThrows<IllegalArgumentException> {
            ActionFactory<Java, MessageType>(Java, Actions.getDefaultInstance(), classLoader)
        }
    }

    @Nested inner class
    `Provide diagnostics for` {

        @Test
        fun `not found class`() {
            val missingClass = "absent.Class"
            val actions = actions {
                action.put(missingClass, ProtoAny.getDefaultInstance())
            }

            val e = assertThrows<ActionFactoryException> {
                createActions(actions)
            }

            e.message.let {
                it shouldContain missingClass
                it shouldContain "available in the classpath"
            }
        }

        @Test
        fun `not a 'RenderAction' class given`() {
            val notRenderingAction = JustMatchingConstructor::class.reference
            val actions = actions {
                action.put(notRenderingAction, ProtoAny.getDefaultInstance())
            }

            val e = assertThrows<ActionFactoryException> {
                createActions(actions)
            }

            e.message.let {
                it shouldContain notRenderingAction
                it shouldContain "cannot be cast to"
                it shouldContain RenderAction::class.java.canonicalName
            }
        }

        @Test
        fun `an action serving incompatible language`() {
            val actions = actions {
                add(RendererInKotlin::class)
            }

            val e = assertThrows<ActionFactoryException> {
                createActions(actions)
            }

            e.message.let {
                it shouldContain RendererInKotlin::class.reference
                it shouldContain "is not compatible with the language for which the factory"
                it shouldContain Kotlin.toString()
            }
        }

        /**
         * Creates an instance of [ActionFactory] and attempts to create actions
         * for the given settings using stubs defined above.
         */
        private fun createActions(actions: Actions) {
            val factory = ActionFactory<Java, MessageType>(Java, actions, classLoader)
            factory.create(messageType, sourceFile, stubContext)
        }
    }
}
