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

package io.spine.protodata.codegen.java

import com.google.protobuf.StringValue
import io.kotest.matchers.shouldBe
import io.spine.protodata.codegen.java.given.TypesTestEnv.javaPackageOption
import io.spine.protodata.codegen.java.given.TypesTestEnv.messageTypeName
import io.spine.protodata.codegen.java.given.TypesTestEnv.typeSystem
import io.spine.protodata.value
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.spine.protobuf.unpack

@DisplayName("`JavaTypeSystem` should")
class JavaTypeSystemSpec {

    @Test
    fun `convert a message type name into a Java class name`() {
        val cls = typeSystem.convertTypeName(messageTypeName)
        cls.simpleName shouldBe messageTypeName.simpleName
        val expectedPackage = javaPackageOption.value.unpack<StringValue>().value
        cls.binary shouldBe expectedPackage + '.' +  messageTypeName.simpleName
    }
}
