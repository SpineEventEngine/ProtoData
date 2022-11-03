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

import io.spine.protodata.FileCoordinates;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.test.FieldId;
import io.spine.text.Position;
import io.spine.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protodata.Ast.typeUrl;
import static io.spine.string.Strings.camelCase;
import static io.spine.protodata.renderer.LineNumber.notInFile;
import static io.spine.text.TextFactory.positionNotFound;
import static java.lang.String.format;

/**
 * An insertion point at the line right before a getter method of the given field.
 *
 * <p>This implementation should only be used for test purposes. It might not cover all the possible
 * edge cases when fining the line where the getter is.
 */
final class FieldGetter implements InsertionPoint {

    private final FieldId field;

    FieldGetter(FieldId field) {
        this.field = checkNotNull(field);
    }

    @NonNull
    @Override
    public String getLabel() {
        return format("getter-for:%s.%s", typeUrl(field.getType()), field.getField().getValue());
    }

    @NonNull
    @Override
    public FileCoordinates locate(Text text) {
        String fieldName = camelCase(field.getField().getValue());
        String getterName = "get" + fieldName;
        Pattern pattern = Pattern.compile("public .+ " + getterName);
        List<String> lines = text.lines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (pattern.matcher(line).find()) {
                return atLine(i);
            }
        }
        return nowhere();
    }
}
