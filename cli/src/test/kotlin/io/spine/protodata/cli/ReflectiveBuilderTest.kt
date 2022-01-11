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

package io.spine.protodata.cli

import io.spine.protodata.cli.given.CtorWithArgsSpiImpl
import io.spine.protodata.cli.given.PrivateCtorSpiImpl
import io.spine.protodata.cli.given.TestReflectiveBuilder
import io.spine.protodata.cli.given.TestSpiImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class `'ReflectiveBuilder' should` {

    private val loader = this.javaClass.classLoader

    @Test
    fun `create an instance of an existing class`() {
        val builder = TestReflectiveBuilder()
        builder.createByName(TestSpiImpl::class.qualifiedName!!, loader)
    }

    @Test
    fun `fail to create an instance of a non-existing class`() {
        val builder = TestReflectiveBuilder()
        assertThrows<ClassNotFoundException> {
            builder.createByName("com.acme.Impl", loader)
        }
    }

    @Test
    fun `fail to create an instance if class does not have a public ctor`() {
        val builder = TestReflectiveBuilder()
        assertThrows<IllegalStateException> {
            builder.createByName(PrivateCtorSpiImpl::class.qualifiedName!!, loader)
        }
    }

    @Test
    fun `fail to create an instance if class does not have a ctor with no arguments`() {
        val builder = TestReflectiveBuilder()
        assertThrows<IllegalStateException> {
            builder.createByName(CtorWithArgsSpiImpl::class.qualifiedName!!, loader)
        }
    }
}
