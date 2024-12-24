/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata.settings

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.spine.protodata.util.Format.JSON
import io.spine.protodata.util.Format.PLAIN
import io.spine.protodata.util.Format.PROTO_BINARY
import io.spine.protodata.util.Format.PROTO_JSON
import io.spine.protodata.util.Format.RCF_UNKNOWN
import io.spine.protodata.util.Format.YAML
import io.spine.protodata.util.extensions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Format` should")
class FormatSpec {

    @Test
    fun `provide allowed extensions`() {
        RCF_UNKNOWN.extensions shouldBe emptyList()

        PROTO_BINARY.extensions.shouldContainInOrder(
            "binpb", "pb", "bin"
        )
        PROTO_JSON.extensions.shouldContainInOrder(
            "pb.json"
        )
        JSON.extensions.shouldContainInOrder(
            "json"
        )
        YAML.extensions.shouldContainInOrder(
            "yml", "yaml"
        )
        PLAIN.extensions.shouldContainInOrder(
            "txt"
        )
    }
}
