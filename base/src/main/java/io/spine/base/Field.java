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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import io.spine.value.ValueHolder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;

/**
 * A reference to a Protobuf message field.
 *
 * @apiNote This class aggregates {@link FieldPath} for augmenting this generated class with
 * useful methods (instead of using static utilities). This approach is used (instead of using
 * {@link io.spine.annotation.GeneratedMixin GeneratedMixin}) because Model Compiler itself depends
 * on this package. Thus, {@code GeneratedMixin} cannot be used for augmenting generated classes
 * that belong to it.
 */
@Immutable
public final class Field extends ValueHolder<FieldPath> {

    private static final String SEPARATOR = ".";
    private static final Joiner joiner = Joiner.on(SEPARATOR);
    private static final Splitter dotSplitter = Splitter.on(SEPARATOR)
                                                        .trimResults();
    private static final long serialVersionUID = 0L;

    private Field(FieldPath path) {
        super(path);
    }

    private static Field create(FieldPath path) {
        checkNotEmpty(path);
        return new Field(path);
    }

    /**
     * Creates a new field reference by the passed path.
     */
    public static Field withPath(FieldPath path) {
        checkNotNull(path);
        return create(path);
    }

    /**
     * Parses the passed field path.
     *
     * @param path
     *         non-empty field path
     * @return the field reference parsed from the path
     */
    public static Field parse(String path) {
        checkNotNull(path);
        FieldPath fp = doParse(path);
        return create(fp);
    }

    /**
     * Creates a new field reference by its name.
     *
     * <p>The passed string is the direct reference to the field, not a field path.
     * Therefore it must not contain the dot separator.
     */
    public static Field named(String fieldName) {
        checkName(fieldName);
        FieldPath path = create(ImmutableList.of(fieldName));
        Field result = create(path);
        return result;
    }

    /**
     * Creates a new field reference by taking its number if the passed message type.
     *
     * @param number
     *         the number of the field as defined in the proto message
     * @param message
     *         the descriptor of the message
     * @return the field reference
     * @throws IllegalStateException
     *         if there is no field with the passed number in this message type
     */
    public static Field withNumberIn(int number, Descriptor message) {
        String name = nameOf(number, message);
        Field result = named(name);
        return result;
    }

    /**
     * Obtains the name of the field with the passed number in the message type specified
     * by the passed descriptor.
     *
     * @param fieldNumber
     *         the number of the field as defined in the proto message
     * @param message
     *         the descriptor of the message
     * @return the name of the field
     * @throws IllegalStateException
     *         if there is no field with the passed number in this message type
     */
    public static String nameOf(int fieldNumber, Descriptor message) {
        checkNotNull(message);
        checkArgument(fieldNumber > 0);
        String result = message
                .getFields()
                .stream()
                .filter(f -> f.getNumber() == fieldNumber)
                .findFirst()
                .map(FieldDescriptor::getName)
                .orElseThrow(() -> newIllegalStateException(
                        "Unable to find the field with the number %d in the type `%s`.",
                        fieldNumber,
                        message.getFullName()
                ));
        return result;
    }

    /**
     * Appends a field name to the current field path to form a nested field.
     */
    public Field nested(String fieldName) {
        checkName(fieldName);
        FieldPath newPath = path().toBuilder()
                                  .addFieldName(fieldName)
                                  .build();
        return create(newPath);
    }

    /**
     * Appends the path enclosed by the {@code other} to the current field path.
     */
    public Field nested(Field other) {
        checkNotNull(other);
        ProtocolStringList fieldNames = other.path()
                                             .getFieldNameList();
        FieldPath newPath = path().toBuilder()
                                  .addAllFieldName(fieldNames)
                                  .build();
        return create(newPath);
    }

    /** Obtains the path of the field. */
    public FieldPath path() {
        return value();
    }


    /**
     * Checks if the field is present (as top-level or nested) in the given message type.
     */
    public boolean presentIn(Descriptor message) {
        Optional<FieldDescriptor> descriptor = findDescriptor(message);
        boolean result = descriptor.isPresent();
        return result;
    }

    /**
     * Obtains a descriptor of the referenced field in the passed message type.
     *
     * @return the descriptor, if there is such a field in the passed type, or empty
     *  {@code Optional} if the field is not declared
     */
    public Optional<FieldDescriptor> findDescriptor(Descriptor message) {
        @Nullable FieldDescriptor field = fieldIn(path(), message);
        return Optional.ofNullable(field);
    }

    /**
     * Checks if the field is a nested field.
     */
    public boolean isNested() {
        int pathComponents = path().getFieldNameCount();
        boolean result = pathComponents > 1;
        return result;
    }

    /**
     * Obtains a string value of the field path.
     *
     * <p>Unlike {@link Message#toString()}, which produces diagnostics output, this method
     * returns the string form of {@code some_field.nested_field.nested_deeper}.
     */
    @Override
    public String toString() {
        return join(value().getFieldNameList());
    }

    /** Creates a new path containing the passed elements. */
    private static FieldPath create(List<String> elements) {
        elements.forEach(Field::checkName);
        FieldPath result = FieldPath
                .newBuilder()
                .addAllFieldName(elements)
                .build();
        return result;
    }

    /** Creates a path instance by parsing the passed non-empty string. */
    @VisibleForTesting
    static FieldPath doParse(String fieldPath) {
        checkArgument(!fieldPath.isEmpty(), "A field path must not be empty.");
        List<String> pathElements = dotSplitter.splitToList(fieldPath);
        return create(pathElements);
    }

    /**
     * Obtains the descriptor of the field in the passed message type.
     *
     * @return the descriptor or {@code null} if the message type does not declare this field
     */
    private static @Nullable FieldDescriptor fieldIn(FieldPath path, Descriptor message) {
        Descriptor current = message;
        FieldDescriptor field = null;
        for (Iterator<String> iterator = path.getFieldNameList().iterator(); iterator.hasNext(); ) {
            String fieldName = iterator.next();
            field = current.findFieldByName(fieldName);
            if (field == null) {
                return null;
            }
            if (iterator.hasNext()) {
                checkArgument(field.getType() == MESSAGE,
                              "Field `%s` of the type `%s` is not a message field.");
                current = field.getMessageType();
            }
        }
        return field;
    }

    /** Ensures that the passed filed name does not contain the path separator. */
    private static void checkName(String fieldName) {
        checkNotEmptyOrBlank(fieldName);
        checkArgument(
                !fieldName.contains(SEPARATOR),
                "A field name cannot contain path separator. Found: `%s`.", fieldName
        );
    }

    /** Ensures that the passed path has at least one element. */
    private static void checkNotEmpty(FieldPath path) throws IllegalArgumentException {
        checkArgument(path.getFieldNameCount() > 0, "Field path must not be empty.");
    }

    /**
     * Joins the passed path elements into the string representation of the path.
     */
    private static String join(Iterable<String> elements) {
        String result = joiner.join(elements);
        return result;
    }
}
