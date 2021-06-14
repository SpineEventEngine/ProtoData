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

package io.spine.protodata.test;

import com.google.common.collect.ImmutableSet;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.language.CommonLanguages;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.Renderer;
import io.spine.protodata.renderer.SourceSet;

import java.util.Set;
import java.util.UUID;

import static io.spine.protodata.codegen.java.JavaNaming.classNameOf;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;

public final class UuidJavaRenderer extends Renderer {

    private static final String METHOD_FORMAT =
            "public static %s randomId() {" + lineSeparator() +
            "    return newBuilder().setUuid(" + lineSeparator() +
                    "%s.randomUUID().toString()" + lineSeparator() +
            "     ).build(); " + lineSeparator() +
            '}' + lineSeparator();

    public UuidJavaRenderer() {
        super(ImmutableSet.of(CommonLanguages.java()));
    }

    @Override
    protected void doRender(SourceSet sources) {
        Set<UuidType> uuidTypes = select(UuidType.class).all();
        for (UuidType type : uuidTypes) {
            TypeName typeName = type.getName();
            ClassName className = classNameOf(this, typeName);
            InsertionPoint classScope = new ClassScope(typeName);
            String code = format(METHOD_FORMAT, className, UUID.class.getName());
            sources.atEvery(classScope, s -> s.add(code));
        }
    }
}
