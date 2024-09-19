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

package io.spine.protodata.test

import io.spine.protodata.render.InsertionPointPrinter
import io.spine.text.TextCoordinates
import io.spine.tools.code.Kotlin

/**
 * An insertion point printer for the [CompanionFrame].
 */
public class CompanionFramer : InsertionPointPrinter<Kotlin>(Kotlin, setOf(CompanionFrame()))

private const val COMPANION_MARKER = " companion "

/**
 * An insertion point that frames the word `companion` in companion object declarations
 * from both sides.
 */
public class CompanionFrame : io.spine.protodata.render.InsertionPoint {

    override val label: String
        get() = "CompanionFrame"

    override fun locate(text: String): Set<TextCoordinates> =
        text.lines()
            .asSequence()
            .mapIndexed { index, line -> index to line }
            .filter { it.second.contains(" companion object") }
            .flatMap { (index, line) ->
                val indexOfCompanion = line.indexOf(COMPANION_MARKER)
                listOf(
                    at(index, indexOfCompanion),
                    at(index, indexOfCompanion + COMPANION_MARKER.length)
                )
            }.toSet()
}
