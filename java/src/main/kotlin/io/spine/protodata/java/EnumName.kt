/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.protodata.java

import kotlin.reflect.KClass

/**
 * A fully qualified name of a Java enum type.
 */
public class EnumName internal constructor(
    packageName: String,
    simpleNames: List<String>
) : ClassOrEnumName(packageName, simpleNames) {

    /**
     * Creates a new enum name from the given package name and a simple name.
     *
     * If an enum is nested inside another class, the [simpleName] parameter must
     * contain all the names from the outermost class to the innermost one, and
     * finished with the enum type name.
     */
    public constructor(packageName: String, vararg simpleName: String) :
            this(packageName, simpleName.toList())

    /**
     * Obtains the name of the given Java enum via its class.
     */
    public constructor(enum: Class<out Enum<*>>) :
            this(enum.`package`?.name ?: "", enum.nestedNames())

    /**
     * Obtains the name of the given Kotlin enum via its class.
     */
    public constructor(enum: KClass<out Enum<*>>) : this(enum.java)
}
