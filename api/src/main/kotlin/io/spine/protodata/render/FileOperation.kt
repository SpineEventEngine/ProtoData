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

import io.spine.protodata.ast.ProtoDeclarationName
import io.spine.protodata.type.Convention
import io.spine.protodata.type.NameElement
import io.spine.tools.code.Language
import java.nio.file.Path

/**
 * A marker interface for fluent API operation classes for creating or searching for a file.
 */
public sealed interface FileOperation

/**
 * Part of the fluent API for finding source files.
 *
 * @param N the type of the Protobuf declaration name such as message, enum, or a service.
 */
public class FileLookup<N: ProtoDeclarationName>(
    private val sources: SourceFileSet,
    private val name: N
) : FileOperation {

    /**
     * Searches for a source file with for the given Proto type generated according to
     * the given [convention].
     */
    public fun <L : Language, T : NameElement<L>> namedUsing(
        convention: Convention<L, N, T>
    ): SourceFile<*>? {
        val declaration = convention.declarationFor(name)
        val path = declaration?.path
        return path?.let { sources.find(it) }
    }
}

/**
 * Part of the fluent API for creating new source files.
 */
public class FileCreation<N: ProtoDeclarationName>(
    private val sources: SourceFileSet,
    private val name: N
) : FileOperation {

    /**
     * Attempts to create a file path for the given type name using the given [convention].
     *
     * If the convention does not define a declaration for the given type, returns `null`.
     *
     * @param T the type of the Protobuf declaration name such as message, enum, or a service.
     */
    public fun <L : Language, T : NameElement<L>> namedUsing(
        convention: Convention<L, N, T>
    ): FileCreationWithPath? {
        val declaration = convention.declarationFor(name)
        val path = declaration?.path
        return path?.let { FileCreationWithPath(sources, path) }
    }
}

/**
 * Part of the fluent API for creating new source files.
 */
public class FileCreationWithPath(
    private val sources: SourceFileSet,
    private val file: Path
) : FileOperation {

    /**
     * Writes the given [code] into the provided file.
     *
     * @return the new source file
     */
    public fun withCode(code: String): SourceFile<*> = sources.createFile(file, code)
}
