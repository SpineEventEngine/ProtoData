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

package io.spine.protodata.test.annotation;

import io.spine.protodata.codegen.java.JavaRenderer;
import io.spine.protodata.renderer.SourceFileSet;
import io.spine.protodata.test.Annotated;
import io.spine.protodata.test.FieldId;

import java.nio.file.Path;
import java.util.Set;

/**
 * Renders Java annotations on field getters for fields marked with
 * the {@code (java_annotation)} option.
 */
@SuppressWarnings("unused") // Accessed reflectively by ProtoData.
public final class AnnotationRenderer extends JavaRenderer {

    private static final int INDENT_LEVEL = 1;

    @Override
    protected void render(SourceFileSet sources) {
        Set<Annotated> annotatedFields = select(Annotated.class).all();
        annotatedFields.forEach(
                field -> renderFor(field, sources)
        );
    }

    private void renderFor(Annotated field, SourceFileSet sourceSet) {
        FieldId id = field.getId();
        FieldGetter getter = new FieldGetter(id);
        Path path = javaFileOf(id.getType(), id.getFile());
        sourceSet.file(path)
                 .at(getter)
                 .withExtraIndentation(INDENT_LEVEL)
                 .add('@' + field.getJavaAnnotation());
    }
}
