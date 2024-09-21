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

package io.spine.protodata.test.annotation;

import com.google.protobuf.StringValue;
import io.spine.core.External;
import io.spine.core.Subscribe;
import io.spine.core.Where;
import io.spine.protodata.ast.event.FieldOptionDiscovered;
import io.spine.protodata.plugin.View;
import io.spine.protodata.plugin.ViewRepository;
import io.spine.protodata.test.Annotated;
import io.spine.protodata.test.FieldId;
import io.spine.server.route.EventRouting;
import org.checkerframework.checker.nullness.qual.NonNull;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.route.EventRoute.withId;

/**
 * A view on a field which is marked with the {@code (java_annotation)} option.
 */
final class AnnotatedView extends View<FieldId, Annotated, Annotated.Builder> {

    private static final String OPTION_NAME = "java_annotation";

    @Subscribe
    void on(@External @Where(field = "option.name", equals = OPTION_NAME)
                    FieldOptionDiscovered event) {
        var value = unpack(event.getOption().getValue(), StringValue.class);
        builder().setJavaAnnotation(value.getValue());
    }

    static final class Repo extends ViewRepository<FieldId, AnnotatedView, Annotated> {

        @Override
        protected void setupEventRouting(@NonNull EventRouting<FieldId> routing) {
            super.setupEventRouting(routing);
            routing.route(FieldOptionDiscovered.class, (message, context) ->
                    withId(FieldId.newBuilder()
                                  .setFile(message.getFile())
                                  .setType(message.getType())
                                  .setField(message.getField())
                                  .build())
            );
        }
    }
}
