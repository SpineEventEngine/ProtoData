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

package io.spine.protodata.java

/**
 * A parameterized name of the class.
 *
 * Example usages:
 *
 * ```
 * val listOfStrings = ParameterizedName(ClassName(List::class), ClassName(String::class))
 * println(listOfStrings) // java.util.List<java.lang.String>
 *
 * val genericMap = ParameterizedName(ClassName(Map::class), ParameterName.T, ParameterName.E)
 * println(genericMap) // java.util.Map<T, E>
 *
 * val comparatorOfLists = ParameterizedName(ClassName(Comparator::class), listOfStrings)
 * println(comparatorOfLists) // java.util.Comparator<java.util.List<java.lang.String>>
 *
 * val comparatorOfMaps = ParameterizedName(ClassName(Comparator::class), genericMap)
 * println(comparatorOfMaps) // java.util.Comparator<java.util.Map<T, E>>
 * ```
 *
 * @param base The parameterized class.
 * @param parameters The type parameters.
 */
public class ParameterizedClassName(base: ClassName, parameters: List<ObjectName>) : ObjectName() {

    init {
        require(parameters.isNotEmpty()) {
            "`${this::class.simpleName}` requires at least one parameter to be passed."
        }
    }

    public constructor(base: ClassName, vararg parameter: ObjectName) : this(base, parameter.toList())

    override val canonical: String = "$base<${parameters.joinToString()}>"
}
