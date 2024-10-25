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

package io.spine.protodata.plugin;

import io.spine.core.External;
import io.spine.protodata.ast.event.TypeEntered;
import io.spine.server.event.NoReaction;
import io.spine.server.event.React;
import io.spine.server.tuple.EitherOf2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`Policy` Java API should")
class PolicyJavaApiSpec {

    /**
     * This test merely makes the {@link Policy#ignore} method used without making any
     * meaningful assertions.
     *
     * <p>It creates a {@link Policy} which calls the `protected` method of the companion object
     * showing the usage scenario.
     *
     * @see PolicySpec#allowIgnoring() the test for Kotlin API
     */
    @Test
    @DisplayName("have static factory method for ignoring incoming events")
    void allowIgnoring() {
        var policy = new Policy<TypeEntered>() {
            @React
            @Override
            protected EitherOf2<TypeEntered, NoReaction> whenever(
                    @External TypeEntered entered) {
                return ignore();
            }
        };
        assertThat(policy).isNotNull();
    }
}
