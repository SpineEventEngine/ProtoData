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

package io.spine.protodata.renderer

/**
 * A name of a source code generator.
 *
 * This can be a `protoc` plugin or builtin, or a custom code generator tool.
 */
public sealed interface SourceGeneratorName {

    public val name: String
        get() = javaClass.simpleName.lowercase()
}

/**
 * The default generator is the default way for the Protobuf compiler to generate source code for
 * a given language.
 *
 * For example, in Java, the default generator, given a message `Foo`, would generate a message
 * classes and auxiliary types, such as classes `Foo`, `Foo.Builder`, `Foo.Parser`,
 * and the interface `FooOrBuilder`.
 *
 * Since the Protobuf compiler does not support all the existing programming languages,
 * the `DefaultGenerator` is only defined for those languages that are supported, such as Java, JS,
 * C++, etc. For other languages, as well as for other code generation scenarios,
 * see [CustomGenerator].
 */
public object DefaultGenerator : SourceGeneratorName

/**
 * A name of a custom source code generator.
 *
 * May represent a Protobuf compiler plugin, or any other code generator.
 *
 * Conventionally, the name of the generator should coincide with the name of the directory where
 * the generated files are placed. Users should follow this convention where possible, yet diverge
 * when necessary. For example, Java gRPC stubs should be marked with the `grpc` name. However,
 * files generated for Dart should be marked with the name `dart`, not `lib`.
 */
public class CustomGenerator(
    override val name: String
) : SourceGeneratorName
