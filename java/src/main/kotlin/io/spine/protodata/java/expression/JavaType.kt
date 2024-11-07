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

package io.spine.protodata.java.expression

/**
 * A Java type.
 *
 * The default coverage includes [singular][SingularTypes], [array][ArrayTypes]
 * and [list][ListTypes] types.
 *
 * To introduce a new type locally, prefer declaring an enum class implementing
 * this interface. Such may be useful when it is possible to add a more specific
 * type for your domain, or introduce a domain-specific types.
 */
public interface JavaType {

    /**
     * Name of this Java type.
     */
    public val name: String
}

/**
 * A singular Java type.
 *
 * Please note, although [MESSAGE] type is not Java-specific, within our codebase
 * Protobuf messages are used a lot.
 */
public enum class SingularTypes : JavaType {
    BYTE,
    INTEGER,
    LONG,
    STRING,
    BOOLEAN,
    OBJECT,
    MESSAGE,
}

/**
 * A typed Java array.
 */
public enum class ArrayTypes : JavaType {
    BYTES_ARRAY,
    INTEGER_ARRAY,
    LONG_ARRAY,
    STRING_ARRAY,
    BOOLEAN_ARRAY,
    OBJECT_ARRAY,
    MESSAGES_ARRAY,
}

/**
 * A typed Java list.
 */
public enum class ListTypes : JavaType {
    BYTES_LIST,
    INTEGER_LIST,
    LONG_LIST,
    STRING_LIST,
    BOOLEAN_LIST,
    OBJECT_LIST,
    MESSAGES_LIST,
}
