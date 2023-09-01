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

package io.spine.protodata.test.uuid;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import io.spine.protodata.plugin.Plugin;
import io.spine.protodata.plugin.ViewRepository;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.test.UuidType;
import io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration;

import java.util.Set;
import java.util.List;

/**
 * The plugin which supplies the {@link UuidType} view.
 */
@SuppressWarnings("unused") // Accessed reflectively.
public final class UuidPlugin implements Plugin {

    @Override
    public List<Renderer<?>> renderers() {
        return ImmutableList.of(
                new ClassScopePrinter(),
                new UuidJavaRenderer(),
                new PrintBeforePrimaryDeclaration()
        );
    }

    @Override
    public Set<ViewRepository<?, ?, ?>> viewRepositories() {
        return ImmutableSet.of(new UuidTypeRepository());
    }
}
