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

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.sksamuel.aedile.core.cacheBuilder
import io.spine.protodata.InsertedPoints
import io.spine.protodata.file
import io.spine.server.query.select
import io.spine.text.Text
import io.spine.text.TextFactory.text
import io.spine.tools.code.Language
import io.spine.tools.psi.convertLineSeparators
import io.spine.tools.psi.java.Environment
import java.lang.System.lineSeparator
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking

/**
 * A file with the source code.
 *
 * This file is a part of a [source set][SourceFileSet]. It should be treated as
 * a part of a software module rather than a file system object.
 * One `SourceFile` may reflect multiple actual files on a file system. For example,
 * a `SourceFile` may be read from one location on the file system and written
 * into another location.
 *
 * @param L the language of this source code file.
 * @property language
 *            the programming language of this source file or [AnyLanguage], if the file is in
 *            the language not currently supported.
 * @property relativePath
 *            the file system path to the file relative to the source root.
 * @property code
 *            the source code.
 * @property changed
 *            tells if the [code] was modified after it was loaded, or if the file was
 *            created [from the code][fromCode].
 * @see SourceFileSet
 */
@Suppress(
    "TooManyFunctions"
    /* Those functions constitute the primary API and should not be represented as extensions. */
)
public class SourceFile<L: Language>
private constructor(
    public val language: Language,
    public val relativePath: Path,
    private var code: String,
    private var changed: Boolean = false
) {
    private lateinit var sources: SourceFileSet
    private val preReadActions = mutableListOf<(SourceFile<L>) -> Unit>()
    private var alreadyRead = false

    /**
     * The full path to the file created in the [input root][SourceFileSet.inputRoot] of
     * the source file set to which this file belongs.
     */
    public val inputPath: Path by lazy {
        sources.inputRoot / relativePath
    }

    /**
     * The full path to the file created in the [output root][SourceFileSet.outputRoot] of
     * the source file set to which this file belongs.
     */
    public val outputPath: Path by lazy {
        sources.outputRoot / relativePath
    }

    /**
     * The file factory for parsing the code.
     */
    private val fileFactory by lazy {
        PsiFileFactory.getInstance(sources.project)
    }

    /**
     * The instance of [PsiFile] obtained by parsing the current [code].
     *
     * Is `null` before the [psi] method is called, or after the [overwrite] method is called.
     */
    private var psiFile: PsiFile? = null

    /**
     * The type of the file to be used by [fileFactory] when parsing.
     */
    private val fileType: FileType by lazy {
        psiFileType()
    }

    /**
     * Obtains an instance of [PsiFile] which corresponds to this source file.
     *
     * The content of the source file is parsed using the language type
     * obtained from the input file name.
     *
     * The returned value is cached until [overwrite] is called.
     *
     * Modifications made to the returned instance are <em>NOT</em>
     * automatically reflected in the [code].
     * If you intend to modify this source file via PSI, get the updated text
     * via [PsiFile.getText] after modifications are applied, and then call [overwrite].
     */
    public fun psi(): PsiFile {
        if (psiFile != null) {
            return psiFile!!
        }
        val fileName = outputPath.toFile().canonicalPath
        val timeStamp = Instant.now().toEpochMilli()
        return fileFactory.createFileFromText(
            fileName,
            fileType,
            code.convertLineSeparators(),
            timeStamp,
            true /* `eventSystemEnabled` */
        ).also {
            psiFile = it
        }
    }

    public companion object {

        /**
         * The cache of created [SourceFile] instances by their full paths.
         *
         * The cache is needed to make sure that two or more [SourceFileSet]s hold
         * the same instance of [SourceFile] for the same file on the file system.
         */
        private val cache = cacheBuilder<Path, SourceFile<*>> {
            initialCapacity = 100
        }.build()

        /**
         * Clears the internal cache which maps full file paths to [SourceFile] instances.
         *
         * Clearing the cache may be useful in between tests to avoid stale instances obtained
         * in cases of using the same full paths.
         */
        @VisibleForTesting
        public fun clearCache() {
            synchronized(this) {
                cache.invalidateAll()
            }
        }

        /**
         * Reads the file from the given file system location.
         */
        internal fun read(
            sourceRoot: Path,
            relativePath: Path,
            charset: Charset = Charsets.UTF_8
        ): SourceFile<*> {
            val absolute = sourceRoot / relativePath
            return synchronized(this) {
                runBlocking {
                    cache.get(absolute) {
                        val lang = Language.of(absolute)
                        val code = absolute.readText(charset)
                        create(lang, relativePath, code)
                    }
                }
            }
        }

        /**
         * Creates an instance of [SourceFile] with for the given language, path, and the code.
         *
         * This function is needed for passing the generic parameter to the constructor.
         */
        private fun <L: Language> create(
            lang: L,
            relativePath: Path,
            code: String,
            changed: Boolean = false
        ): SourceFile<L> = SourceFile(lang, relativePath, code, changed)

        /**
         * Constructs a file from source code.
         *
         * @param relativePath
         *         the file system path for the file relative to the source root;
         *         the file might not exist on the file system.
         * @param code
         *         the source code.
         */
        @VisibleForTesting
        public fun fromCode(
            relativePath: Path,
            code: String
        ): SourceFile<*> =
            create(Language.of(relativePath), relativePath, code, changed = true)
    }

    /**
     * Creates a new fluent builder for adding code at the given [insertionPoint].
     *
     * If the [insertionPoint] is not found in the code, no action will be
     * performed as the result. If there is more than one instance of
     * the same insertion point, the code will be added to all of them.
     *
     * Insertion points should be marked with comments of special format.
     * The added code is always inserted after the line with the comment, and
     * the line with the comment is preserved.
     *
     * @see atInline
     */
    public fun at(insertionPoint: InsertionPoint): SourceAtLine =
        SourceAtLine(this, insertionPoint)

    /**
     * Creates a new fluent builder for adding code at the given inline [insertionPoint].
     *
     * If the [insertionPoint] is not found in the code, no action will be performed as the result.
     * If there is more than one instance of the same insertion point, the code will be added to
     * all of them.
     *
     * Code inserted via the resulting fluent builder, unlike code inserted via [SourceFile.at],
     * will be placed right into the line that contains the insertion point.
     * Authors of [Renderer]s may choose to either use insertion points in the whole-line
     * mode or in the inline mode.
     * Also, the same insertion point may be used in both modes.
     *
     * @see at
     */
    public fun atInline(insertionPoint: InsertionPoint): SourceAtPoint {
        val points = sources.querying.select<InsertedPoints>()
            .findById(file { path = relativePath.toString() })
        val point = points?.pointList?.firstOrNull { it.label == insertionPoint.label }
        return if (point != null) {
            SpecificPoint(this@SourceFile, point)
        } else {
            NoOp
        }
    }

    /**
     * Deletes this file from the source set.
     *
     * As a result of this method, the associated source file will be
     * eventually removed from the file system.
     *
     * If the file was created earlier (by the same or a different [Renderer]),
     * the file will not be written to the file system.
     *
     * After this method, the file will no longer be accessible via
     * the associated `SourceSet`.
     */
    public fun delete() {
        sources.delete(relativePath)
    }

    /**
     * Changes the contents of this file to the provided [newCode].
     *
     * **Note** This method may overwrite the work of other [Renderer]s, as well
     * as remove the insertion points from the file. Use with caution.
     * Prefer using [at(InsertionPoint)][at] when possible.
     */
    public fun overwrite(newCode: String) {
        this.code = newCode
        this.changed = true
        this.psiFile = null
    }

    /**
     * Overwrites the code in this file line by line.
     */
    internal fun updateLines(newCode: List<String>) {
        overwrite(newCode.joinToString(lineSeparator()))
    }

    /**
     * Injects the given [sources].
     */
    internal fun attachTo(sources: SourceFileSet) {
        this.sources = sources
    }

    /**
     * Writes the source code into the file on the file system.
     *
     * It may be the case that the file is read from one directory (source) and
     * written into another directory (target). Thus, the initial path from where
     * the file is read may not coincide with the path from where the file is written.
     *
     * @param baseDir
     *         the directory into which the file should be written;
     *         this file's [relativePath] is resolved upon this directory.
     * @param charset
     *         the charset to use to write the file; UTF-8 is the default.
     * @param forceWrite
     *         if `true`, this file must be written to the FS even if no changes have been
     *         done upon it; otherwise, the file may not be written to avoid unnecessary
     *         file system operations.
     */
    internal fun write(
        baseDir: Path,
        charset: Charset = Charsets.UTF_8,
        forceWrite: Boolean = false
    ) {
        if (changed || forceWrite) {
            val targetPath = baseDir / relativePath
            targetPath.toFile()
                .parentFile
                .mkdirs()
            targetPath.writeText(code, charset, WRITE, TRUNCATE_EXISTING, CREATE)
        }
    }

    /**
     * Deletes this source file from the file system.
     *
     * It may be the case that the file is read from one directory (source) and
     * changed in another directory (target). Thus, the initial path from where
     * the file is read may not coincide with the path from where the file is deleted.
     *
     * @param rootDir
     *         the root directory where the file lies; the [relativePath] is resolved
     *         upon this directory.
     * @see write
     */
    internal fun rm(rootDir: Path) {
        val targetPath = rootDir / relativePath
        targetPath.toFile().deleteRecursively()
    }

    /**
     * Gets the entire content of this file.
     */
    public fun code(): String {
        initializeCode()
        return code
    }

    /**
     * Gets the content of this file as a list of lines.
     */
    public fun lines(): List<String> {
        return code.lines()
    }

    /**
     * Gets the text content of this source file.
     */
    @Deprecated("Use `code()` instead.", ReplaceWith("code()"))
    public fun text(): Text {
        return text(code)
    }

    /**
     * Adds an action to be executed before obtaining the [code] of this source file.
     */
    internal fun beforeRead(action: (SourceFile<*>) -> Unit) {
        preReadActions.add(action)
        if (alreadyRead) {
            action(this)
        }
    }

    private fun initializeCode() {
        if (!alreadyRead) {
            alreadyRead = true
            preReadActions.forEach { it(this) }
        }
    }

    /**
     * Returns the [relativePath] string of this source file.
     */
    override fun toString(): String = relativePath.toString()
}

/**
 * Gets the type of this source file by using its name.
 */
private fun SourceFile<*>.psiFileType(): FileType {
    // Ensure all the services are registered. This is fast to call repeatedly.
    Environment.setUp()
    val registry = FileTypeRegistry.getInstance()
    check(registry != null) {
        "Unable to get `FileTypeRegistry` instance."
    }
    return registry.getFileTypeByFileName(relativePath.toString())
}
