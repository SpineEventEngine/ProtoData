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

package io.spine.protodata.codegen.java

import io.spine.protodata.type.TypeNameElement
import io.spine.tools.code.Java
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass

/**
 * A fully qualified Java class name.
 */
public class ClassName
internal constructor(
    private val packageName: String,
    private val simpleNames: List<String>
) : JavaElement, TypeNameElement<Java> {

    /**
     * Creates a new class name from the given package name a class name.
     *
     * If a class is nested inside another class, the [simpleName] parameter must contain all
     * the names from the outermost class to the innermost one.
     */
    public constructor(packageName: String, vararg simpleName: String) :
            this(packageName, simpleName.toList())

    /**
     * Obtains the class name of the given Java class.
     */
    public constructor(cls: Class<*>) : this(cls.`package`.name, cls.nestedName())

    /**
     * Obtains the Java class name of the given Kotlin class.
     */
    public constructor(klass: KClass<*>) : this(klass.java)

    /**
     * The canonical name of the class.
     *
     * This is the name by which the class is referred to in Java code.
     *
     * For regular Java classes, This is similar to [binary], except that in a binary name nested
     * classes are separated by the dollar (`$`) sign, and in canonical — by the dot (`.`) sign.
     */
    @get:JvmName("canonical")
    public val canonical: String = "$packageName.${simpleNames.joinToString(".")}"

    /**
     * The binary name of the class.
     *
     * This is the name by which the class is referred to in Bytecode.
     *
     * For regular Java classes, This is similar to [canonical], except that in a binary name nested
     * classes are separated by the dollar (`$`) sign, and in canonical — by the dot (`.`) sign.
     */
    @get:JvmName("binary")
    public val binary: String
        get() = "$packageName.${simpleNames.joinToString("$")}"

    @get:JvmName("javaFile")
    public val javaFile: Path by lazy {
        val dir = packageName.replace('.', '/')
        val topLevelClass = simpleNames.first()
        Path("$dir/$topLevelClass.java")
    }

    /**
     * The simple name of this class.
     *
     * If the class is nested inside another class, the outer class name is NOT included.
     */
    @get:JvmName("simpleName")
    public val simpleName: String
        get() = simpleNames.last()

    /**
     * Obtains a new `ClassName` with the given suffix appended to the last simple name.
     */
    public fun withSuffix(suffix: String): ClassName {
        val newLast = simpleNames.last() + suffix
        val newSimpleNames = simpleNames.dropLast(1) + newLast
        return ClassName(packageName, newSimpleNames)
    }

    override fun toCode(): String = canonical

    override fun toString(): String = canonical

    override fun hashCode(): Int = canonical.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassName) return false
        return canonical == other.canonical
    }
}

/**
 * Obtains a name of a class taking into account nesting hierarchy.
 *
 * The receiver class may be nested inside another class, which may be nested inside
 * another class, and so on.
 *
 * The returned list contains simple names of the classes, starting from the outermost to
 * the innermost, which is the receiver of this extension function.
 *
 * If the class is not nested, the returned list contains only a simple name of the class.
 */
private fun Class<*>.nestedName(): List<String> {
    if (declaringClass == null) {
        return listOf(this.simpleName)
    }
    val names = mutableListOf<String>()
    var cls: Class<*>? = this
    do {
        names.add(cls!!.simpleName)
        cls = cls.declaringClass
    } while (cls != null)
    return names.reversed()
}
