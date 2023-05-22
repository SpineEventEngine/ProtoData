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
 * Configures code generation process performed by ProtoData.
 */
public interface CodegenSettings {

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.plugin.Plugin` classes.
     */
    public fun plugins(vararg classNames: String)

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.renderer.Renderer` classes.
     */
    public fun renderers(vararg classNames: String)

    /**
     * Passes given names of Java classes to ProtoData as
     * the `io.spine.protodata.option.OptionsProvider` classes.
     */
    public fun optionProviders(vararg classNames: String)

    @Deprecated("Left for compatibility reason. Has no effect. Planned for removal in v2.0.0.")
    public fun options(vararg classNames: String) {
    }

    /**
     * A directory where the serialized `CodeGeneratorRequest`s are stored.
     *
     * For each source set, we generate a separate request file. Files are named after
     * the associated source set with the `.bin` extension.
     */
    public var requestFilesDir: Any

    /**
     * The base directory where Protobuf Gradle Plugin placed generated source code files.
     */
    @Deprecated("Starting from v0.9.2 Protobuf Gradle Plugin uses" +
            " a fixed path `build/generated/source/proto`." +
            " Therefore this property cannot be set and will be removed.")
    public var srcBaseDir: Any

    /**
     * The subdirectory to which the files generated from Protobuf are placed.
     *
     * The default value is `"java"`.
     *
     * @see srcBaseDir
     */
    @Deprecated("Use `subDirs` instead.")
    public var subDir: String

    /**
     * The subdirectories to which the files generated from Protobuf are placed.
     *
     * If the code files that need processing are placed in a few subdirectories within [srcBaseDir]
     * (e.g. `java` and `spine`), after processing, the same directory structure is
     * preserved in the [targetBaseDir].
     *
     * The default value is `"java"`.
     *
     * @see srcBaseDir
     */
    public var subDirs: List<String>

    /**
     * The base directory where the files generated by ProtoData are placed.
     *
     * By default, points at the `$projectDir/generated/` directory.
     *
     * @see srcBaseDir
     */
    public var targetBaseDir: Any
}
