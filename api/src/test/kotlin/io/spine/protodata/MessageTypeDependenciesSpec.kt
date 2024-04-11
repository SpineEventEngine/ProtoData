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

package io.spine.protodata

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.spine.protodata.Field.CardinalityCase.LIST
import io.spine.protodata.Field.CardinalityCase.MAP
import io.spine.protodata.Field.CardinalityCase.SINGLE
import io.spine.protodata.type.TypeSystem
import io.spine.test.type.Article
import io.spine.test.type.Author
import io.spine.test.type.AuthorName
import io.spine.test.type.Jungle
import io.spine.test.type.MagazineCover
import io.spine.test.type.MessageTypeDependenciesSpecProto
import io.spine.test.type.OopFun
import io.spine.test.type.Photo
import io.spine.test.type.ProjectId
import io.spine.test.type.ProjectName
import io.spine.test.type.ProjectProto
import io.spine.test.type.ProjectView
import io.spine.test.type.UserId
import io.spine.test.type.UserName
import io.spine.test.type.UserView
import io.spine.test.type.Volume
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`MessageTypeDependencies` should")
internal class MessageTypeDependenciesSpec {

    private val typeSystem: TypeSystem by lazy {
        val protoSources = setOf(
            MessageTypeDependenciesSpecProto.getDescriptor().toPbSourceFile(),
            ProjectProto.getDescriptor().toPbSourceFile(),
        )
        TypeSystem(protoSources)
    }

    private fun allDependenciesOf(type: MessageType): Set<MessageType> =
        MessageTypeDependencies(type, null, typeSystem).scan()

    @Test
    fun `collect no types if there are no fields with message type`() {
        val volumeType = messageTypeOf<Volume>()
        val found = allDependenciesOf(volumeType)
        found.shouldBeEmpty()
    }

    @Test
    fun `collect nested types`() {
        val coverType = messageTypeOf<MagazineCover>()
        val found = allDependenciesOf(coverType)
        found shouldHaveSize 4
        found.shouldContainExactlyInAnyOrder(
            messageTypeOf<MagazineCover.Headline>(),
            messageTypeOf<Photo>(),
            messageTypeOf<MagazineCover.Url>(),
            messageTypeOf<Article.Headline>(),
        )
    }

    @Test
    fun `collect types referenced from types of fields`() {
        val projectView = messageTypeOf<ProjectView>()
        val deps = MessageTypeDependencies(projectView, SINGLE, typeSystem).scan()
        deps shouldHaveSize 5
        deps.shouldContainExactlyInAnyOrder(
            messageTypeOf<ProjectId>(),
            messageTypeOf<ProjectName>(),
            messageTypeOf<UserView>(),
            messageTypeOf<UserId>(),
            messageTypeOf<UserName>(),
        )
    }

    @Test
    fun `collect a single instance of type in case of circular field references`() {
        val authorType = messageTypeOf<Author>()
        val found = allDependenciesOf(authorType)
        found shouldHaveSize 2
        found.shouldContainExactlyInAnyOrder(
            authorType,
            messageTypeOf<AuthorName>()
        )
    }


    @Nested inner class
    `allow gathering types by cardinality of fields obtaining` {
        private val funnyType = messageTypeOf<OopFun>()
        private val jungleType = messageTypeOf<Jungle>()
        private val treeType = messageTypeOf<Jungle.BananaTree>()
        private val bananaType = messageTypeOf<Jungle.BananaTree.Banana>()

        private val gorillaType = messageTypeOf<Jungle.Gorilla>()

        @Test
        fun `all fields`() {
            val allTypes = MessageTypeDependencies(funnyType, null, typeSystem).scan()

            allTypes shouldHaveSize 4
            allTypes.shouldContainExactlyInAnyOrder(
                jungleType,
                treeType,
                bananaType,
                gorillaType
            )
        }

        @Test
        fun `single fields`() {
            val singleTypes = MessageTypeDependencies(funnyType, SINGLE, typeSystem).scan()
            singleTypes shouldHaveSize 1
            singleTypes shouldContain jungleType
        }

        @Test
        fun `map fields`() {
            val mapTypes = MessageTypeDependencies(funnyType, MAP, typeSystem).scan()
            mapTypes shouldHaveSize 1
            mapTypes shouldContain gorillaType
        }
        @Test
        fun `repeated fields`() {
            val repeatedTypes = MessageTypeDependencies(funnyType, LIST, typeSystem).scan()
            repeatedTypes shouldHaveSize 2
            repeatedTypes shouldContain treeType
            repeatedTypes shouldContain bananaType
        }
    }
}
