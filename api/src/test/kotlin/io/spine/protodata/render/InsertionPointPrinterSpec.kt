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

package io.spine.protodata.render

import io.spine.text.TextCoordinates
import io.spine.tools.code.Java
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`InsertionPointPrinter` should")
class InsertionPointPrinterSpec {

    /**
     * Tests that the printer does not accept points without labels passed to the constructor.
     *
     * It is still possible to pass such points when overriding the deprecated method
     * [InsertionPointPrinter.supportedInsertionPoints].
     *
     * Currently, the implementation of [InsertionPointPrinter.render] filters out such points.
     * We aim to remove the printing of insertion points as PSI-based code parsing
     * gets more ground.
     */
    @Test
    fun `not accept points without labels`() {
        assertThrows<IllegalArgumentException> {
            object : InsertionPointPrinter<Java>(Java, listOf(StubPoint())) {
                // No-op.
            }
        }
    }
}

private class StubPoint: io.spine.protodata.render.InsertionPoint {
    override val label: String = ""
    override fun locate(text: String): Set<TextCoordinates> = emptySet()
}