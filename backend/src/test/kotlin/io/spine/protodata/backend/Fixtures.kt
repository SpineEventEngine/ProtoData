/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.protodata.protobuf.ProtoFileList
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.type.TypeSystem
import io.spine.tools.prototap.CompiledProtosFile
import java.io.File
import java.net.URLClassLoader

/**
 * Creates a type system using the list of compiled proto files from the `test-env` module.
 */
internal fun createTypeSystem(request: CodeGeneratorRequest): TypeSystem {
    val testEnvJar = DoctorProto::class.java.protectionDomain.codeSource.location
    // Create the class loader without the parent because the parent is checked first.
    // We only interested in that particular JAR.
    val urlClassLoader = URLClassLoader(arrayOf(testEnvJar), null)
    val protoFiles = CompiledProtosFile(urlClassLoader)
        .listFiles { File(it) }
    val typeSystem = request.toTypeSystem(
        ProtoFileList(protoFiles)
    )
    return typeSystem
}
