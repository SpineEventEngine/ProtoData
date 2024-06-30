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

package io.spine.protodata.cli.app

import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * A factory for creating instances of classes by their fully qualified names.
 *
 * The class is loaded via a `ClassLoader` and an instance is created.
 * It is expected that the class has a `public` constructor with no parameters.
 *
 * @param T the type of the objects created by this factory.
 */
public open class ReflectiveFactory<T: Any> {

    /**
     * Creates an instance of `T`.
     *
     * It is necessary that the class defined by the [className] parameter is a subtype of `T`.
     * Otherwise, a casting error occurs.
     *
     * @param className
     *         name of the concrete class to instantiate
     * @param classLoader
     *         the [ClassLoader] to load the class by its name
     */
    public fun createByName(className: @FqBinaryName String, classLoader: ClassLoader): T {
        val cls = classLoader.loadClass(className).kotlin
        @Suppress("UNCHECKED_CAST")
        val tClass = cls as KClass<T>
        return create(tClass)
    }

    private fun create(cls: KClass<T>): T {
        val ctor = cls.constructors.find { it.visibility.isPublic() && it.parameters.isEmpty() }
        check(ctor != null) {
            "The class `${cls.qualifiedName}` should have a public zero-parameter constructor."
        }
        return ctor.call()
    }
}

/**
 * Checks if this [KVisibility] is [public][KVisibility.PUBLIC].
 */
private fun KVisibility?.isPublic() = this == KVisibility.PUBLIC
