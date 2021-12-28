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

package io.spine.protodata.codegen.java.suppress

import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.file.BeforePrimaryDeclaration
import io.spine.protodata.config.configAs
import io.spine.protodata.renderer.SourceFileSet

/**
 * Suppresses warnings in the generated code.
 *
 * If no configuration is provided to ProtoData, suppresses all the warnings with `"ALL"`.
 * Otherwise, parses the config as a [SuppressConfig] and suppresses only the specified warnings.
 *
 * Warnings in the generated code do no good for the user, as they cannot be fixed without changing
 * the code generation logic. We recommend suppressing them.
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
 * @see io.spine.protodata.codegen.java.generado.GenerateGenerated
 */
public class SuppressRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        val warnings = if (configIsPresent()) {
            configAs<SuppressConfig>().warnings.valueList
        } else {
            listOf("ALL")
        }
        val warningsList = warnings.joinToString { '"' + it + '"' }
        val suppression = "@${SuppressWarnings::class.java.simpleName}({$warningsList})"
        sources.forEach {
            it.at(BeforePrimaryDeclaration).add(suppression)
        }
    }
}
