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

package io.spine.protodata.renderer

import io.spine.tools.code.Language
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * Thrown when [ActionFactory] cannot instantiate
 * a [RenderAction][io.spine.protodata.renderer.RenderAction].
 */
public class ActionFactoryException private constructor(message: String, cause: Throwable?)
    : ReflectiveOperationException(message, cause) {

    private constructor(message: String) : this(message, null)

    internal companion object {

        @Suppress("ConstPropertyName") // To conform Java convention.
        private const val serialVersionUID: Long = -3922823622064715639L

        /**
         * Analyzes the type of the given [Throwable] and rethrows it wrapped into
         * [ActionFactoryException] with the supplied diagnostic message.
         *
         * @throws ActionFactoryException always.
         */
        internal fun propagate(actionClassName: @FqBinaryName String, e: Throwable): Nothing {
            val msg = when (e) {
                is ClassNotFoundException -> {
                    "Unable to create an instance of the class: `$actionClassName`. " +
                            "Please make sure that the class is available in the classpath."
                }
                is ClassCastException -> {
                    val actionClass = RenderAction::class.java.canonicalName
                    "The class `$actionClassName` cannot be cast to `$actionClass`."
                }
                else -> {
                    "Unable to create an instance of the class: `$actionClassName`."
                }
            }
            throw ActionFactoryException(msg, e)
        }

        internal fun incompatibleLanguage(
            actionClassName: @FqBinaryName String,
            factoryLanguage: Language,
            actionLanguage: Language
        ): Nothing {
            throw ActionFactoryException(
                "The action class `$actionClassName` serves the language `$actionLanguage`" +
                        " is not compatible with the language for which the factory was created " +
                        "(`$factoryLanguage`)."
            )
        }
    }
}
