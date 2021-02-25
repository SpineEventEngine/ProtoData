/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata.given

import io.spine.protodata.FileEntered
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.subscriber.CodeEnhancement
import io.spine.protodata.subscriber.Subscriber

/**
 * A [Subscriber] which accumulates all the info about files upon first discovery.
 */
class TestQueryingSubscriber : Subscriber<FileEntered>(FileEntered::class.java) {

    /**
     * The files processed by this subscriber.
     *
     * Despite the fact that the subscriber processes with the [FileEntered] event, which does not
     * have all the data about a file, these files are the complete models of the actual Protobuf
     * source files, thanks to the querying capabilities of the subscriber.
     */
    val files = mutableListOf<ProtobufSourceFile>()

    override fun process(event: FileEntered): Iterable<CodeEnhancement> {
        val file = select(ProtobufSourceFile::class.java)
            .withId(event.file.path)
            .execute()
            .first()
        files.add(file)
        return emptyList()
    }
}
