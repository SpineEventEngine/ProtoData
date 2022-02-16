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

package io.spine.protodata.gradle

/**
 * Constants for locating ProtoData in Maven repositories.
 */
public object Artifacts {

    /**
     * The Maven group of the ProtoData artifacts.
     */
    public const val group: String = "io.spine.protodata"

    /**
     * The name of the artifact of ProtoData Compiler.
     */
    public const val compiler: String = "protodata-compiler"

    /**
     * The infix to be used in an artifact name before a submodule name.
     */
    private const val infix: String = "protodata"

    /**
     * Obtains Maven coordinates of the `fat-cli` variant of command-line application.
     *
     * "fat-cli" is an all-in-one distribution of ProtoData, published somewhat in the past.
     * Ironically, we need it in ProtoData development.
     * It removes the dependency conflicts between ProtoData-s.
     */
    public fun fatCli(version: String): String = "$group:$infix-fat-cli:$version"

    /**
     * Obtains Maven coordinates for ProtoData command-line application.
     */
    public fun cli(version: String): String = "$group:$infix-cli:$version"

    /**
     * Obtains Maven coordinates for the ProtoData plugin to Google Protobuf Compiler (`protoc`).
     */
    public fun protocPlugin(version: String): String = "$group:$infix-protoc:$version:exe@jar"
}
