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

package io.spine.code.java;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TimestampOrBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link SimpleClassName}.
 *
 * <p>Even though the code where {@link SimpleClassName} resides no longer depends on the
 * {@code base} module, this test uses descriptors copied from the {@code base} stored in
 * resources. That's why the {@code ErrorProto} descriptor is available for these tests.
 */
@DisplayName("SimpleClassName should")
class SimpleClassNameTest {

    @Test
    @DisplayName("obtain default builder class name")
    void obtain_default_builder_class_name() {
        assertTrue(SimpleClassName.ofBuilder()
                                  .value()
                                  .contains(Message.Builder.class.getSimpleName()));
    }

    @Test
    @DisplayName("obtain name for message or builder")
    void obtain_name_for_message_or_builder() {
        assertEquals(TimestampOrBuilder.class.getSimpleName(),
                     SimpleClassName.messageOrBuilder(Timestamp.class.getSimpleName())
                                    .value());
    }

    @Test
    @DisplayName("obtain value by descriptor")
    void obtain_value_by_descriptor() {
        assertEquals(Timestamp.class.getSimpleName(),
                     SimpleClassName.ofMessage(Timestamp.getDescriptor())
                                    .value());
    }
}
