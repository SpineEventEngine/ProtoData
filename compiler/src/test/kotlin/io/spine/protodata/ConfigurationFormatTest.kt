/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata

import com.google.common.truth.Truth.assertThat
import io.spine.protodata.config.ConfigurationFormat.JSON
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.config.ConfigurationFormat.PROTO_BINARY
import io.spine.protodata.config.ConfigurationFormat.PROTO_JSON
import io.spine.protodata.config.ConfigurationFormat.RCF_UNKNOWN
import io.spine.protodata.config.ConfigurationFormat.YAML
import io.spine.protodata.config.extensions
import org.junit.jupiter.api.Test

class `ConfigurationFormat should` {

    @Test
    fun `provide allowed extensions`() {
        assertThat(RCF_UNKNOWN.extensions)
            .isEmpty()

        assertThat(JSON.extensions)
            .containsExactly("json")
        assertThat(PROTO_JSON.extensions)
            .containsExactly("pb.json")
        assertThat(PROTO_BINARY.extensions)
            .containsExactly("pb", "bin")
        assertThat(YAML.extensions)
            .containsExactly("yml", "yaml")
        assertThat(PLAIN.extensions)
            .isEmpty()
    }
}
