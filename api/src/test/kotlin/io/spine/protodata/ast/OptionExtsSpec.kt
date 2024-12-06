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

package io.spine.protodata.ast

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.spine.protobuf.field
import io.spine.protodata.ast.given.OptionExtsSpecProto
import io.spine.protodata.ast.given.Selector
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Extensions for `Option` should")
internal class OptionExtsSpec {

    @Test
    fun  `obtain options for a file`() {
        val file = OptionExtsSpecProto.getDescriptor()
        val options = file.options()

        options.named("type_url_prefix").run {
            doc.leadingComment shouldContain "We use type URL prefix"
            span.run {
                startLine shouldBe 34
                startColumn shouldBe 1
                endLine shouldBe 34
                endColumn shouldBe 44
            }
        }

        options.named("java_package").run {
            doc.leadingComment shouldContain "The preferred package"
            span.run {
                startLine shouldBe 37
                startColumn shouldBe 1
                endLine shouldBe 37
                endColumn shouldBe 54
            }
        }
    }

    @Test
    fun `obtain options for a message`() {
        val msg = Selector.getDescriptor()
        val options = msg.options()

        options.named("deprecated").run {
            doc.leadingComment shouldContain "This is a standard message option."
            doc.trailingComment shouldContain "A trailing comment for the option."

            span.run {
                startLine shouldBe 51
                startColumn shouldBe 5
                endLine shouldBe 51
                endColumn shouldBe 31
            }
        }

        options.named("entity").run {
            doc.leadingComment shouldContain "This is a custom Spine option."
            doc.trailingComment shouldContain "Another trailing comment."

            span.run {
                startLine shouldBe 55
                startColumn shouldBe 5
                endLine shouldBe 58
                endColumn shouldBe 7
            }
        }
    }

    @Test
    fun `obtain options for a field`() {
        val field = Selector.getDescriptor().field("position")!!
        val options = field.options()

        options.named("required").run {
            doc.leadingComment.shouldBeEmpty()
            span .run {
                startLine shouldBe 63
                startColumn shouldBe 9
                endLine shouldBe 63
                endColumn shouldBe 26
            }
        }
    }

    @Test
    fun `obtain options for an enum`() {
        val enum = Selector.Position.getDescriptor()
        val options = enum.options()

        options.named("deprecated").run {
            doc.leadingComment shouldContain "A standard enum option."
            span.run {
                startLine shouldBe 70
                endLine shouldBe 70
                startColumn shouldBe 9
                endColumn shouldBe 35
            }
        }
    }

    @Test
    fun `obtain options for an enum item`() {
        val item = Selector.Position.POSITION_LEFT.valueDescriptor
        val options = item.options()

        options.named("deprecated").run {
            // Enum item docs are not available from descriptors.
            doc.leadingComment.shouldBeEmpty()
            span.run {
                startLine shouldBe 77
                endLine shouldBe 77
                startColumn shouldBe 13
                endColumn shouldBe 31
            }
        }
    }
}

private fun List<Option>.named(name: String): Option = find { it.name == name }!!
