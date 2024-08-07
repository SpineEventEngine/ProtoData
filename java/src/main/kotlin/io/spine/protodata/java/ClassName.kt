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

package io.spine.protodata.java

import kotlin.reflect.KClass
import org.checkerframework.checker.signature.qual.FullyQualifiedName

/**
 * A fully qualified Java class name.
 */
public class ClassName(
    packageName: String,
    simpleNames: List<String>
) : ClassOrEnumName(packageName, simpleNames) {

    init {
        simpleNames.forEach {
            require(!it.contains(SEPARATOR)) {
                "A simple name must not contain a package separator (`$it`)."
            }
        }
    }

    /**
     * Creates a new class name from the given package name a class name.
     *
     * If a class is nested inside another class, the [simpleName] parameter must
     * contain all the names from the outermost class to the innermost one.
     */
    public constructor(packageName: String, vararg simpleName: String) :
            this(packageName, simpleName.toList())

    /**
     * Obtains the class name of the given Java class.
     */
    public constructor(cls: Class<*>) : this(cls.`package`?.name ?: "", cls.nestedNames())

    /**
     * Obtains the Java class name of the given Kotlin class.
     */
    public constructor(klass: KClass<*>) : this(klass.java)

    /**
     * The binary name of the class.
     *
     * This is the name by which the class is referred to in Bytecode.
     *
     * For regular Java classes, This is similar to [canonical], except that
     * in a binary name nested classes are separated by the dollar (`$`) sign,
     * and in canonical — by the dot (`.`) sign.
     */
    @get:JvmName("binary")
    public val binary: String
        get() = "$packagePrefix${simpleNames.joinToString("$")}"

    /**
     * Obtains a new `ClassName` with the given suffix appended to the last simple name.
     *
     * The method is useful for obtaining names for `MessageOrBuilder` interfaces.
     */
    public fun withSuffix(suffix: String): ClassName {
        val newLast = simpleNames.last() + suffix
        val newSimpleNames = simpleNames.dropLast(1) + newLast
        return ClassName(packageName, newSimpleNames)
    }

    /**
     * Obtains a new `ClassName` with the given simple name appended to the list of simple names.
     *
     * The method is useful for obtaining names for nested classes such as `Message.Builder`.
     */
    public fun nested(simpleClassName: String): ClassName =
        ClassName(packageName, simpleNames + simpleClassName)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassName) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun hashCode(): Int = binary.hashCode()

    public companion object {

        internal const val SEPARATOR = '.'

        /**
         * Returns a new [ClassName] instance for the given fully-qualified class name string.
         *
         * The function assumes that given name follows Java conventions for naming classes and
         * packages with `lowercase` package names and `UpperCamelCase` class names.
         *
         * This method of obtaining a class name should be
         */
        public fun guess(name: @FullyQualifiedName String): ClassName {
            require(name.isNotEmpty())
            require(name.isNotBlank())
            val packageSeparator = "."
            val items = name.split(SEPARATOR)
            val packageName = items.filter { it[0].isLowerCase() }.joinToString(packageSeparator)
            val simpleNames = items.filter { it[0].isUpperCase() }
            return ClassName(packageName, simpleNames)
        }
    }
}
