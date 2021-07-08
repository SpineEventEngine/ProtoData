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

package io.spine.protodata.test.uuid;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * A {@code String.format()} style code template.
 */
final class Template {

    private static final Splitter NEW_LINE_SPLITTER = Splitter.on(System.lineSeparator());
    private static final Joiner NEW_LINE_JOINER = Joiner.on(System.lineSeparator());

    private final String code;

    private Template(String[] lines) {
        this.code = NEW_LINE_JOINER.join(lines);
    }

    /**
     * Creates a new template from the given code lines.
     *
     * <p>Lines may have {@code String.format()} style insertion points,
     * such as {@code %s}, {@code %d}, etc.
     *
     * <p>If a line contains a new line character, it will be split into two (or more) lines when
     * formatting the template.
     */
    static Template from(String... lines) {
        checkNotNull(lines);
        return new Template(lines);
    }

    /**
     * Inserts the given positional arguments into the template.
     *
     * @return formatted code lines
     */
    ImmutableList<String> format(Object... positionalArgs) {
        String formatted = String.format(code, positionalArgs);
        ImmutableList<String> lines = NEW_LINE_SPLITTER.splitToStream(formatted)
                                                       .collect(toImmutableList());
        return lines;
    }
}
