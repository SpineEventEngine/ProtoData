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

package io.spine.protodata.context

import io.spine.annotation.Internal
import io.spine.base.EntityState
import io.spine.protodata.ast.EnumInFile
import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageInFile
import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.ast.ProtobufSourceFile
import io.spine.protodata.ast.ServiceInFile
import io.spine.protodata.ast.enums
import io.spine.protodata.ast.messages
import io.spine.protodata.ast.services
import io.spine.protodata.settings.LoadsSettings
import io.spine.protodata.type.TypeSystem
import io.spine.server.query.QueryingClient
import io.spine.tools.code.Language

/**
 * A part of [CodegenContext] which participates in the code generation process and
 * may have settings it can load.
 *
 * @param L The type of the programming language served by this member.
 */
public abstract class Member<L : Language>
protected constructor(
    /**
     * The programming language served by this member.
     *
     * As most implementations of [Language] are Kotlin `object`s,
     * like [io.spine.tools.code.Java] or [io.spine.tools.code.Kotlin], it is likely that
     * the value passed to this parameter would repeat the argument specified
     * for the generic parameter [L].
     */
    public val language: L
) : LoadsSettings, ContextAware {

    /**
     * The backing field for the [context] property.
     */
    private lateinit var _context: CodegenContext

    /**
     * A code generation context associated with this instance.
     *
     * Is `null` before the call to [registerWith].
     *
     * @see registerWith
     */
    protected open val context: CodegenContext?
        get() = if (this::_context.isInitialized) {
            _context
        } else {
            null
        }

    /**
     * A type system with the Protobuf types defined in the current code generation pipeline.
     *
     * Is `null` if the type system is not yet available to this renderer.
     *
     * This property is guaranteed to be non-`null` after [registerWith].
     */
    protected open val typeSystem: TypeSystem?
        get() = context?.typeSystem

    /**
     * Creates a [QueryingClient] for obtaining entity states of the given type.
     *
     * @param S the type of the entity state.
     */
    public inline fun <reified S : EntityState<*>> select(): QueryingClient<S> =
        select(S::class.java)

    /**
     * Creates a [QueryingClient] for obtaining entity states of the given type.
     *
     * @param S the type of the entity state.
     * @param type the class of the entity state.
     */
    public final override fun <S : EntityState<*>> select(type: Class<S>): QueryingClient<S> =
        _context.select(type)

    final override fun <T : Any> loadSettings(cls: Class<T>): T = super.loadSettings(cls)

    final override fun settingsAvailable(): Boolean = super.settingsAvailable()

    /**
     * Injects the `Code Generation` context into this instance.
     *
     * The reference to the context is necessary to query the state of entities.
     *
     * This method is `public` because it is inherited from the [ContextAware] interface.
     * But it is essentially `internal` to ProtoData SDK, and is not supposed to be called
     * by authors of plugins directly.
     *
     * @see [select]
     * @see [io.spine.protodata.backend.Pipeline]
     *
     * @suppress This function is not supposed to be used by plugin authors code.
     */
    @Internal
    public final override fun registerWith(context: CodegenContext) {
        if (isRegistered()) {
            check(_context == context) {
                "Unable to register `$this` with `${context}` because" +
                        " it is already registered with `${this._context}`."
            }
            return
        }
        _context = context
    }

    /**
     * Checks if this member is registered with the context.
     *
     * @suppress Similarly to [registerWith], this function is not supposed to be called by
     *  plugin authors users.
     */
    @Internal
    override fun isRegistered(): Boolean {
        return this::_context.isInitialized
    }
}

/**
 * Obtains the header of the proto file with the given [path].
 */
public fun Member<*>.findHeader(path: File): ProtoFileHeader? =
    select<ProtobufSourceFile>().findById(path)?.header

/**
 * Obtains all Protobuf source code files passed to the current compilation process.
 */
public fun Member<*>.findAllFiles(): Collection<ProtobufSourceFile> =
    select<ProtobufSourceFile>().all()

/**
 * Obtains all the message types that are parsed by the current compilation process
 * along with the corresponding file headers.
 *
 * Message types that are dependencies of the compilation process are not included.
 *
 * @see ProtobufSourceFile
 * @see io.spine.protodata.ast.ProtobufDependency
 */
public fun Member<*>.findMessageTypes(): Set<MessageInFile> =
    findAllFiles()
        .flatMap { it.messages() }
        .toSet()

/**
 * Obtains all the enum types that are parsed by the current compilation process
 * along with the corresponding file headers.
 *
 * Enum types that are dependencies of the compilation process are not included.
 *
 * @see ProtobufSourceFile
 * @see io.spine.protodata.ast.ProtobufDependency
 */
public fun Member<*>.findEnumTypes(): Set<EnumInFile> =
    findAllFiles()
        .flatMap { it.enums() }
        .toSet()

/**
 * Obtains all service declarations that are parsed by the current compilation process
 * along with the corresponding file headers.
 *
 * Services that are dependencies of the compilation process are not included.
 *
 * @see ProtobufSourceFile
 * @see ProtobufDependency
 */
public fun Member<*>.findServices(): Set<ServiceInFile> =
    findAllFiles()
        .flatMap { it.services() }
        .toSet()
