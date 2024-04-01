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

package io.spine.protodata

import com.google.common.annotations.VisibleForTesting
import io.spine.protodata.type.TypeSystem
import io.spine.server.Closeable
import io.spine.server.entity.Entity
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.query.Querying

/**
 * A context of code generation.
 */
public interface CodegenContext : Querying, Closeable {

    /**
     * The type system containing all the types available for code generation.
     */
    public val typeSystem: TypeSystem

    /**
     * The `Insertion Points` context which generates events when
     * [InsertionPoint][io.spine.protodata.renderer.InsertionPoint]s are added to the code.
     */
    public val insertionPointsContext: ThirdPartyContext

    /**
     * A test-only method which checks if the context has entities of the given type.
     */
    @VisibleForTesting
    public fun <E : Entity<*, *>> hasEntitiesOfType(cls: Class<E>): Boolean
}
