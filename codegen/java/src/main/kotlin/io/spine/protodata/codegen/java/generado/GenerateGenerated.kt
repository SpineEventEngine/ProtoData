/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.codegen.java.generado

import com.google.common.annotations.VisibleForTesting
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.file.BeforePrimaryDeclaration
import io.spine.protodata.renderer.SourceFileSet
import javax.annotation.Generated

/**
 * Adds the `javax.annotation.Generated` annotation to the top-level declaration of each Java file
 * in the source set.
 *
 * In order to work, this renderer needs the [BeforePrimaryDeclaration] insertion point. Add
 * the [io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration] before this renderer
 * to make sure the insertion point are present in the source files.
 *
 * *Tradeoff.* The negative side of using this renderer is in that the contents of all the files
 * are altered. Typically, ProtoData performs file loading and insertion point lookup lazily, as
 * those might be costly operations. This renderer undoes this effort by "touching" each file
 * in the source set.
 *
 * @see io.spine.protodata.codegen.java.suppress.SuppressRenderer
 */
public class GenerateGenerated : JavaRenderer() {

    internal companion object {

        @VisibleForTesting
        internal const val GENERATORS = "by the Protobuf Compiler and modified by ProtoData"
    }

    override fun render(sources: SourceFileSet) {
        sources.forEach {
            it.at(BeforePrimaryDeclaration).add(
                "@${Generated::class.qualifiedName}(\"$GENERATORS\")"
            )
        }
    }
}
