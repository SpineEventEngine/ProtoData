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

package io.spine.protodata.language

import io.spine.protodata.file.Glob
import io.spine.protodata.renderer.SourceSet

/**
 * A programming language or a specific syntax.
 *
 * For example, `"Java"`, `"Python 3.x"`, etc.
 */
public abstract class Language(

    /**
     * Label to distinguish the language.
     */
    public val name: String,

    /**
     * Pattern which all the language sources must match.
     *
     * For example, all Java source files must have `.java` extension.
     */
    private val filePattern: Glob
) {

    /**
     * Creates a syntactically valid comment in this language.
     *
     * @param line
     *     the contents of the comment
     * @return a line which can be safely inserted into a code file.
     */
    public abstract fun comment(line: String): String

    /**
     * Filters a given source set retaining only the files in this language.
     *
     * @return a new source set with all the files from the given [sourceSet] which match.
     *     the [filePattern] of this language
     */
    internal fun filter(sourceSet: SourceSet): SourceSet {
        val files = sourceSet.filter { filePattern.matches(it.path) }.toSet()
        return SourceSet(files, sourceSet.rootDir)
    }

    override fun toString(): String = name
}

/**
 * A C-like language.
 *
 * Supports double-slash comments (`// <comment body>`).
 */
public class LanguageWithSlashComments(
    name: String,
    filePattern: Glob
) : Language(name, filePattern) {

    override fun comment(line: String): String = "// $line"
}

/**
 * A collection of commonly used [Language]s.
 *
 * If this prepared set is not enough, users are encouraged to create custom [Language] types
 * by either extending the class directly, or using one of its existing subtypes, such as
 * [LanguageWithSlashComments].
 */
public object CommonLanguages {

    /**
     * Any language will do.
     *
     * This instance indicates that any programming language can be accepted.
     *
     * Intended to be used for filtering source files by language via file name conventions. If no
     * filtering required, but a [Language] is needed, use `CommonLanguages.any`.
     *
     * Does not support [comments][Language.comment].
     */
    @JvmStatic
    public val any: Language = object : Language("any language", Glob.any) {
        override fun comment(line: String): String {
            throw UnsupportedOperationException("`$name` does not support comments.")
        }
    }

    @JvmStatic
    public val Kotlin: Language = LanguageWithSlashComments("Kotlin", Glob.extension("kt"))
    @JvmStatic
    public val Java: Language = LanguageWithSlashComments("Java", Glob.extension("java"))
    @JvmStatic
    public val JavaScript: Language = LanguageWithSlashComments("JavaScript", Glob.extension("js"))
}
