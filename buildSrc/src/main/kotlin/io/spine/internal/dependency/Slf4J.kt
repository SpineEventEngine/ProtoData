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

package io.spine.internal.dependency

/**
 * Spine uses own [logging library][Spine.Logging].
 *
 * SLF4J was used in early versions of Spine. Then we used Flogger.
 *
 * The primary purpose of having this dependency object is for cases when SLF4J is
 * used as a logging backend (e.g. like in [Flogger.Runtime.slf4JBackend]).
 *
 * Some third-party libraries may clash with different versions of the library.
 * Thus, we specify this version and force it via [forceVersions].
 * Please see `DependencyResolution.kt` for details.
 *
 * @see <a href="https://https://github.com/qos-ch/slf4j/tags">SLF4J releases at GitHub</a>
 */
@Suppress("unused", "ConstPropertyName")
object Slf4J {
    private const val version = "2.0.7"
    const val lib = "org.slf4j:slf4j-api:${version}"
    const val jdk14 = "org.slf4j:slf4j-jdk14:${version}"
    const val reload4j = "org.slf4j:slf4j-reload4j:${version}"
    const val simple = "org.slf4j:slf4j-simple:${version}"
}
