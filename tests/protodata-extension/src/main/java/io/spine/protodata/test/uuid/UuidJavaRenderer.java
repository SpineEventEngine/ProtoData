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

import com.google.common.collect.ImmutableList;
import io.spine.protodata.FilePath;
import io.spine.protodata.TypeName;
import io.spine.protodata.codegen.java.ClassName;
import io.spine.protodata.codegen.java.JavaRenderer;
import io.spine.protodata.renderer.InsertionPoint;
import io.spine.protodata.renderer.SourceFileSet;
import io.spine.protodata.test.UuidType;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * A renderer which adds the {@code randomId()} factory methods to the UUID types.
 *
 * <p>A UUID type is a message which only has one field — a {@code string} field
 * called {@code uuid}.
 */
@SuppressWarnings("unused") // Accessed by ProtoData via refection.
public final class UuidJavaRenderer extends JavaRenderer {

    /**
     * The indentation level of one offset (four space characters).
     */
    private static final int INDENT_LEVEL = 1;

    private static final Template METHOD_FORMAT = Template.from(
            "public static %s randomId() {",
            "    return newBuilder().setUuid(",
                    "%s.randomUUID().toString()",
            "     ).build(); ",
            "}");

    /**
     * Renders the random ID factory method for all UUID types.
     *
     * <p>If a class represents a UUID type, places a public static method into the class scope.
     * The method generates a new instance of the class with a random UUID value.
     *
     * <p>A UUID type is a message with a single string field called UUID.
     */
    @Override
    protected void render(SourceFileSet sources) {
        System.err.println("000000000000000");
        System.err.println("UuidJavaRenderer");
        System.err.println("000000000000000");
        Set<UuidType> uuidTypes = select(UuidType.class).all();
        for (UuidType type : uuidTypes) {
            System.err.println(type.getName().getSimpleName());
            System.err.println("--=----=-==-=-=-=-=-=-==-==-=-=-==-");
            TypeName typeName = type.getName();
            FilePath file = type.getDeclaredIn();
            ClassName className = classNameOf(typeName, file);
            InsertionPoint classScope = new ClassScope(typeName);
            ImmutableList<String> lines = METHOD_FORMAT.format(className, UUID.class.getName());
            Path javaFilePath = javaFileOf(typeName, file);
            sources.file(javaFilePath)
                   .at(classScope)
                   .withExtraIndentation(INDENT_LEVEL)
                   .add(lines);
        }
    }
}
