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

import io.spine.protodata.type.NameElement
import io.spine.tools.code.Java
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * A fully qualified name of a Java type.
 */
public abstract class TypeName internal constructor(
    protected val packageName: String,
    protected val simpleNames: List<String>
) : NameElement<Java>, JavaElement {

    init {
        require(simpleNames.isNotEmpty()) {
            "A type name must have a least one simple name."
        }
    }

    /**
     * The simple name of this type.
     *
     * If the type is nested inside a class, the outer class name is NOT included.
     */
    @get:JvmName("simpleName")
    public val simpleName: String
        get() = simpleNames.last()
    
    /**
     * A prefix to be used to refer this type as a fully qualified name.
     *
     * If [packageName] is empty, the prefix is also empty.
     * Otherwise, the prefix contains the package name followed by a dot (`.`).
     */
    protected val packagePrefix: String
        get() = if (packageName.isEmpty()) "" else "$packageName."

    public companion object {

        /**
         * A regular expression for a simple Java type name.
         */
        public val simpleNameRegex: Regex = Regex("^[a-zA-Z_$][a-zA-Z\\d_$]*$")
    }
}

/**
 * A fully qualified Java class or enum name.
 */
public abstract class ClassOrEnumName internal constructor(
    packageName: String,
    simpleNames: List<String>
) : TypeName(packageName, simpleNames) {

    /**
     * The canonical name of the type.
     *
     * This is the name by which the class is referred to in Java code.
     *
     * For regular Java classes, this is similar to [ClassName.binary],
     * except that in a binary name nested classes are separated by
     * the dollar (`$`) sign, and in canonical — by the dot (`.`) sign.
     */
    @get:JvmName("canonical")
    public val canonical: String = "$packagePrefix${simpleNames.joinToString(".")}"

    /**
     * The path to the Java source file of this type.
     */
    @get:JvmName("javaFile")
    public val javaFile: Path by lazy {
        val dir = packageName.replace('.', '/')
        val topLevelClass = simpleNames.first()
        Path("$dir/$topLevelClass.java")
    }

    /**
     * Tells if this type is nested inside another type.
     */
    public val isNested: Boolean = simpleNames.size > 1

    /**
     * Obtains the [canonical] name of the type.
     */
    override fun toCode(): String = canonical

    /**
     * Obtains the [canonical] name of the type.
     */
    override fun toString(): String = canonical

    override fun hashCode(): Int = canonical.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassOrEnumName) return false
        return canonical == other.canonical
    }
}

/**
 * Obtains a name of a class taking into account nesting hierarchy.
 *
 * The receiver class may be nested inside another class, which may be
 * nested inside another class, and so on.
 *
 * The returned list contains simple names of the classes, starting
 * from the outermost to the innermost, which is the receiver of
 * this extension function.
 *
 * If the class is not nested, the returned list contains only
 * a simple name of the class.
 */
internal fun Class<*>.nestedNames(): List<String> {
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
