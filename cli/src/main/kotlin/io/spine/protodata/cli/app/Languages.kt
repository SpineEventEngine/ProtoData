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

package io.spine.protodata.cli.app

import io.spine.protodata.cli.knownLanguages
import io.spine.tools.code.Language
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible

/**
 * Parses a [Language] from this string.
 *
 * If the string represents a [well-known language name][knownLanguages], uses that language.
 * Otherwise, attempts to use this string as a class name and load an instance of that class.
 * If the class represents a Kotlin `object`, loads the instance of the object. Otherwise, calls
 * the no-argument constructor.
 */
internal fun String.toLanguage(): Language {
    require(this.isNotBlank()) { "Expected a language name of class, but got `$this`." }
    return toKnownLanguage() ?: loadLanguage()
}

private fun String.toKnownLanguage(): Language? {
    val key = lowercase()
    return knownLanguages[key]
}

private fun String.loadLanguage(): Language {
    val cls = languageClass()
    return cls.objectInstance ?: cls.instantiate()
}

private fun String.languageClass(): KClass<Language> {
    val javaClass = Class.forName(this)
    val ktClass = javaClass.kotlin
    require(ktClass.isSubclassOf(Language::class)) { "Expected a language class, but got `$this`." }
    @Suppress("UNCHECKED_CAST") // Ensured by the precondition above.
    return ktClass as KClass<Language>
}

private fun KClass<Language>.instantiate(): Language {
    val ctor = constructors.firstOrNull { it.parameters.isEmpty() }
    require(ctor != null) { "Language class `$this` must have a zero-parameter constructor." }
    ctor.isAccessible = true
    val instance = ctor.call()
    return instance
}
