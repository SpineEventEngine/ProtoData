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

package io.spine.protodata.backend

import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Generates a random URL-safe base64-encoded string.
 *
 * @see <a href="https://neilmadden.blog/2018/08/30/moving-away-from-uuids/">
 *     Moving away from UUIDs</a>
 * @see <a href="https://github.com/LableOrg/java-uniqueid>java-uniqueid">java-uniqueid</a>
 * @see <a href="https://github.com/nikbucher/j-nanoid">j-nanoid</a>
 */
@OptIn(ExperimentalEncodingApi::class)
internal object SecureRandomString {

    private const val DEFAULT_SIZE = 20

    private val random: SecureRandom by lazy {
        SecureRandom()
    }

    private val encoder: Base64 by lazy {
        Base64.UrlSafe
    }

    fun generate(size: Int = DEFAULT_SIZE): String {
        val buffer = ByteArray(size)
        random.nextBytes(buffer)
        return encoder.encode(buffer)
    }
}
