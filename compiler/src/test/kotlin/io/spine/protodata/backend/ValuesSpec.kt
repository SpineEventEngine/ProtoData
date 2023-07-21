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

import com.google.protobuf.FieldMask
import com.google.protobuf.fieldMask
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.shouldBe
import io.spine.base.Time
import io.spine.protodata.qualifiedName
import io.spine.protodata.value
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.spine.protodata.test.postcard

@DisplayName("`Values` should")
class ValuesSpec {

    @Test
    fun `convert a simple message to a Value`() {
        val v = Values.from(Time.currentTime())
        v.messageValue.type.simpleName shouldBe "Timestamp"
        v.messageValue.fieldsMap shouldHaveKey "seconds"
        v.messageValue.fieldsMap["seconds"]!!.intValue shouldBeGreaterThan 0
    }

    @Test
    fun `convert a repeated field`() {
        val v = Values.from(fieldMask { paths.addAll(listOf("foo", "bar", "baz")) })
        v.messageValue.type.qualifiedName() shouldBe FieldMask.getDescriptor().fullName
        v.messageValue.fieldsMap shouldHaveKey "paths"
        val list = v.messageValue.fieldsMap["paths"]!!.listValue
        list.getValues(0).stringValue shouldBe "foo"
        list.getValues(1).stringValue shouldBe "bar"
        list.getValues(2).stringValue shouldBe "baz"
    }

    @Test
    fun `convert a map field`() {
        val v = Values.from(postcard {
            congratulation = "Happy birthday!"
            signatures.put("John", "JD")
            signatures.put("Alan", "Big Al")
        })
        v.messageValue.fieldsMap shouldHaveKey "signatures"
        val map = v.messageValue.fieldsMap["signatures"]!!.mapValue
        map.getValue(0).key.stringValue shouldBe "John"
        map.getValue(0).value.stringValue shouldBe "JD"
        map.getValue(1).key.stringValue shouldBe "Alan"
        map.getValue(1).value.stringValue shouldBe "Big Al"
    }
}
