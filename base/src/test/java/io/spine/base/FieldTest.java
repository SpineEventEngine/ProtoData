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

package io.spine.base;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Truth8;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Timestamp;
import io.spine.test.protobuf.AnyHolder;
import io.spine.test.protobuf.StringHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Field.nameOf;
import static io.spine.base.Field.named;
import static io.spine.base.Field.parse;
import static io.spine.testing.Assertions.assertIllegalArgument;
import static io.spine.testing.Assertions.assertIllegalState;
import static io.spine.testing.Assertions.assertNpe;

/**
 * Tests for {@link Field}.
 *
 * <p>See {@code field_paths_test.proto} for definition of proto types used in these tests.
 */
@DisplayName("`Field` should")
class FieldTest {

    @Test
    @DisplayName("pass null tolerance check")
    void nullCheck() {
        new NullPointerTester().testAllPublicStaticMethods(Field.class);
    }


    @Nested
    @DisplayName("parse the passed path")
    class Parsing {

        @Test
        @DisplayName("with immediate field name")
        void immediate() {
            String expected = "val";
            Field field = Field.parse(expected);
            assertThat(field.path().getFieldNameList())
                    .containsExactly(expected);
        }

        @Test
        @DisplayName("delimited with dots")
        void nested() {
            String path = "highway.to.hell";
            Field field = Field.parse(path);
            assertThat(field.toString())
                    .isEqualTo(path);
            assertThat(field.path().getFieldNameList())
                    .containsExactly("highway", "to", "hell");
        }

        @Test
        @DisplayName("rejecting empty path")
        void rejectingEmpty() {
            assertIllegalArgument(() -> parse(""));
        }
    }

    @Test
    @DisplayName("create the instance by the path")
    void byPath() {
        FieldPath expected = Field.doParse("road_to.mandalay");
        assertThat(Field.withPath(expected).path())
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("create the field by its name")
    class SingleField {

        @Test
        @DisplayName("accepting non-empty string")
        void byName() {
            String name = "my_way";
            assertThat(Field.named(name).toString())
                    .isEqualTo(name);
        }

        @Test
        @DisplayName("rejecting `.` in the field name")
        void noSeparatorInName() {
            assertRejects("with.dot");
        }

        @Test
        @DisplayName("rejecting empty, null or blank names")
        void rejectEmpty() {
            assertRejects("");
            assertRejects(" ");
            assertRejects("  ");
            assertNpe(() -> named(null));
        }

        void assertRejects(String illegalValue) {
            assertIllegalArgument(() -> named(illegalValue));
        }
    }

    @Test
    @DisplayName("check that the field is present in the message type")
    void checkPresent() {
        Field field = Field.parse("val");
        Descriptor message = StringHolder.getDescriptor();
        assertThat(field.presentIn(message)).isTrue();
    }

    @Test
    @DisplayName("check that the field is not present in the message type")
    void checkNotPresent() {
        Field field = Field.parse("some_other_field");
        Descriptor message = StringHolder.getDescriptor();
        assertThat(field.presentIn(message)).isFalse();
    }

    @Nested
    @DisplayName("obtain the descriptor of the field")
    class GettingDescriptor {

        @Test
        @DisplayName("if present")
        void ifFound() {
            Truth8.assertThat(Field.named("val")
                                   .findDescriptor(AnyHolder.getDescriptor()))
                  .isPresent();
        }

        @Test
        @DisplayName("returning empty `Optional` if not found")
        void notFound() {
            Truth8.assertThat(Field.named("value") // the real name is `val`.
                                   .findDescriptor(StringHolder.getDescriptor()))
                  .isEmpty();
        }
    }

    @Nested
    @DisplayName("obtain the name of the field by its number")
    class ByNumber {

        private final Descriptor message = Timestamp.getDescriptor();

        @Test
        @DisplayName("returning the short name of the field, if present")
        void nameValue() {
            String name = Field.nameOf(Timestamp.NANOS_FIELD_NUMBER, message);
            assertThat(name).isEqualTo("nanos");
        }

        @Nested
        @DisplayName("throwing")
        class Throwing {

            @Test
            @DisplayName("`IllegalArgumentException` for non-positive number")
            void zeroOrNegative() {
                assertIllegalArgument(() -> nameOf(-1, message));
                assertIllegalArgument(() -> nameOf(0, message));
            }

            @Test
            @DisplayName("`IllegalStateException` if there is no field with such number")
            void noField() {
                assertIllegalState(() -> nameOf(100, message));
            }
        }
    }

    @Test
    @DisplayName("obtain the instance which is a nested field in the current field type")
    void returnNested() {
        String topLevelField = "top-level-field";
        Field topLevel = Field.named(topLevelField);
        String nestedField = "nested-field";
        Field nested = topLevel.nested(nestedField);

        FieldPath path = nested.path();
        assertThat(path.getFieldNameList()).containsExactly(topLevelField, nestedField);
    }

    @Test
    @DisplayName("check that the field is a nested field")
    void checkNested() {
        Field field = Field.parse("some.nested.field");
        assertThat(field.isNested()).isTrue();
    }

    @Test
    @DisplayName("check that the field is not a nested field")
    void checkTopLevel() {
        Field field = Field.parse("top_level_field");
        assertThat(field.isNested()).isFalse();
    }
}
