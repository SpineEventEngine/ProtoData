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
import io.spine.protodata.ast.given.NotificationRequest
import io.spine.protodata.ast.given.NotificationService
import io.spine.protodata.ast.given.OptionExtsSpecProto
import io.spine.protodata.ast.given.Selector
import io.spine.protodata.given.value.Citizen
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Extensions for `Option` should")
internal class OptionExtsSpec {

    @Nested inner class
    `obtain options for`{

        @Test
        fun  `a file`() {
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
        fun `a message`() {
            val msg = Selector.getDescriptor()
            val options = msg.options()

            options.named("deprecated").run {
                doc.leadingComment shouldContain "This is a standard message option."
                doc.trailingComment shouldContain "A trailing comment for the option."

                span.run {
                    startLine shouldBe 55
                    startColumn shouldBe 5
                    endLine shouldBe 55
                    endColumn shouldBe 31
                }
            }

            options.named("entity").run {
                doc.leadingComment shouldContain "This is a custom Spine option."
                doc.trailingComment shouldContain "Another trailing comment."

                span.run {
                    startLine shouldBe 59
                    startColumn shouldBe 5
                    endLine shouldBe 62
                    endColumn shouldBe 7
                }
            }
        }

        @Test
        fun `a field`() {
            val field = Selector.getDescriptor().field("position")!!
            val options = field.options()

            options.named("required").run {
                doc.leadingComment.shouldBeEmpty()
                span .run {
                    startLine shouldBe 67
                    startColumn shouldBe 9
                    endLine shouldBe 67
                    endColumn shouldBe 26
                }
            }
        }

        @Test
        fun `a field group under oneof`() {
            val oneof = NotificationRequest.getDescriptor().oneofs.find { it.name == "channel" }!!
            val options = oneof.options()

            options.named("is_required").run {
                doc.leadingComment shouldContain "A channel must be selected."
                doc.trailingComment shouldContain "The trailing comment for the `oneof` option."

                span.run {
                    startLine shouldBe 104
                    startColumn shouldBe 9
                    endLine shouldBe 104
                    endColumn shouldBe 37
                }
            }
        }

        @Test
        fun `an enum`() {
            val enum = Selector.Position.getDescriptor()
            val options = enum.options()

            options.named("deprecated").run {
                doc.leadingComment shouldContain "A standard enum option."
                span.run {
                    startLine shouldBe 74
                    endLine shouldBe 74
                    startColumn shouldBe 9
                    endColumn shouldBe 35
                }
            }
        }

        @Test
        fun `an enum item`() {
            val item = Selector.Position.POSITION_LEFT.valueDescriptor
            val options = item.options()

            options.named("deprecated").run {
                // Enum item docs are not available from descriptors.
                doc.leadingComment.shouldBeEmpty()
                span.run {
                    startLine shouldBe 81
                    endLine shouldBe 81
                    startColumn shouldBe 13
                    endColumn shouldBe 31
                }
            }
        }

        @Test
        fun `a service`() {
            val service = NotificationService.getDescriptor()
            val options = service.options()

            options.named("deprecated").run {
                doc.leadingComment shouldContain "A standard service option."
                doc.trailingComment shouldContain "Trailing comment for the standard option."

                span.run {
                    startLine shouldBe 122
                    startColumn shouldBe 5
                    endLine shouldBe 122
                    endColumn shouldBe 31
                }
            }
        }

        @Test
        fun `a service method`() {
            val method = NotificationService.getDescriptor().methods.find {
                it.name == "SendNotification"
            }!!
            val options = method.options()

            options.named("idempotency_level").run {
                doc.leadingComment shouldContain "Ensure retry-safe behavior."
                doc.trailingComment shouldContain "Trailing comment for the standard method option."

                span.run {
                    startLine shouldBe 132
                    startColumn shouldBe 9
                    endLine shouldBe 132
                    endColumn shouldBe 47
                }
            }
        }
    }

    @Test
    fun `return value of a boolean option`() {
        val option = Citizen.getDescriptor().options()
            .first()
        option.boolValue shouldBe true
    }
}

private fun List<Option>.named(name: String): Option = find { it.name == name }!!
