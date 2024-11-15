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

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import org.checkerframework.checker.signature.qual.FullyQualifiedName

/**
 * A fully qualified Java class name.
 */
public open class ClassName(
    public val packageName: String,
    public val simpleNames: List<String>
) : ObjectName() {

    init {
        require(simpleNames.isNotEmpty()) {
            "A class name must have a least one simple name."
        }
        simpleNames.forEach {
            require(!it.contains(PACKAGE_SEPARATOR)) {
                "A simple name must not contain a package separator" +
                        " (`$PACKAGE_SEPARATOR`). Encountered: `$it`."
            }
            require(!it.contains(BINARY_SEPARATOR)) {
                "A simple name must not contain a binary class name separator" +
                        " (`$BINARY_SEPARATOR`). Encountered: `$it`."
            }
        }
    }

    /**
     * Creates a new class name from the given package name and one or more class names.
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
     * Tells if this class is nested inside another class.
     */
    public val isNested: Boolean = simpleNames.size > 1

    /**
     * Returns an expression that obtains `Class` instance of this class.
     */
    public val clazz: Expression<Class<*>> by lazy {
        Expression("$canonical.class")
    }

    /**
     * The simple name of this class.
     *
     * If it is nested inside another class, the outer class name is NOT included.
     */
    @get:JvmName("simpleName")
    public val simpleName: String
        get() = simpleNames.last()

    /**
     * A prefix to be used to refer to this class as a fully qualified name.
     *
     * If [packageName] is empty, the prefix is also empty.
     * Otherwise, the prefix contains the package name followed by a dot (`.`).
     */
    private val packagePrefix: String =
        if (packageName.isEmpty()) "" else "$packageName."

    /**
     * The canonical name of the class.
     *
     * For regular Java classes, this is similar to [ClassName.binary],
     * except that in a binary name nested classes are separated by
     * the dollar (`$`) sign, and in canonical — by the dot (`.`) sign.
     *
     * @see Class.getCanonicalName
     */
    public override val canonical: String =
        "$packagePrefix${simpleNames.joinToString(CANONICAL_SEPARATOR)}"

    /**
     * The path to the Java source file of this class.
     *
     * The returned path uses the Unix path [separator][PATH_SEPARATOR] (`/`).
     */
    @get:JvmName("javaFile")
    public val javaFile: Path by lazy {
        val dir = packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR)
        val topLevelClass = simpleNames.first()
        Path("$dir$PATH_SEPARATOR$topLevelClass.java")
    }

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

    /**
     * Obtains a new `ClassName` with the last element removed from the list of simple names.
     *
     * The method is useful for obtaining names for outer classes,
     * e.g. `Message` from `Message.Builder`.
     *
     * @return  A new `ClassName` if the current class name is not the top-level class.
     *          Otherwise, `null` is returned.
     */
    public fun outer(): ClassName? {
        return if (simpleNames.size > 1)
            ClassName(packageName, simpleNames.dropLast(1))
        else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassName) return false
        if (!super.equals(other)) return false
        return true
    }

    public companion object {

        /**
         * Returns a new [ClassName] instance for the given fully qualified class name string.
         *
         * The function assumes that the given name follows Java conventions for naming classes and
         * packages with `lowercase` package names and `UpperCamelCase` class names.
         *
         * @throws IllegalArgumentException when binary class name separator (`"$"`) is used
         *  in the given name, or the given value is empty or blank.
         */
        public fun guess(name: @FullyQualifiedName String): ClassName {
            require(name.isNotEmpty())
            require(name.isNotBlank())
            require(!name.contains(BINARY_SEPARATOR)) {
                "The class name (`$name`) must not contain" +
                        " a binary class name separator (`$BINARY_SEPARATOR`)."
            }
            val items = name.split(PACKAGE_SEPARATOR)
            val packageName = items.filter { it[0].isLowerCase() }.joinToString(PACKAGE_SEPARATOR)
            val simpleNames = items.filter { it[0].isUpperCase() }
            return ClassName(packageName, simpleNames)
        }

        /**
         * The separator in a binary class name.
         */
        public const val BINARY_SEPARATOR: String = "$"

        /**
         * The separator used between nested class names in a canonical name.
         */
        public const val CANONICAL_SEPARATOR: String = "."

        /**
         * A regular expression for a simple Java class name.
         */
        public val simpleNameRegex: Regex = Regex("^[a-zA-Z_$][a-zA-Z\\d_$]*$")

        /**
         * The separator in a package name.
         */
        public const val PACKAGE_SEPARATOR: String = "."

        /**
         * The Unix style separator used to delimit directory names in a Java file name.
         *
         * This separator is compatible with IntelliJ PSI.
         */
        public const val PATH_SEPARATOR: String = "/"
    }
}

/**
 * Returns the [Class] instance for this [ClassName], if any.
 *
 * The function returns a non-`null` result if a Java class denoted by this
 * [ClassName] is present on the classpath.
 */
public fun ClassName.javaClass(): Class<*>? =
    try {
        Class.forName(canonical)
    } catch (ignored: ClassNotFoundException) {
        null
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
