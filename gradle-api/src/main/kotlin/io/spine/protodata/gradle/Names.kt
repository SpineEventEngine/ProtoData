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
 * The name of various objects in ProtoData Gradle API.
 */
public object Names {

    /**
     * The resource file containing the version of ProtoData.
     *
     * Such a resource name might be duplicated in other places in ProtoData code base.
     * The reason for this is to avoid creating an extra dependency for the Gradle plugin,
     * so that the users wouldn't have to declare a custom Maven repository to use the plugin.
     */
    public const val VERSION_RESOURCE: String = "version.txt"

    /**
     * The name of the `protoc` plugin exposed by ProtoData.
     */
    public const val PROTOC_PLUGIN: String = "protodata"

    /**
     * The name of the Gradle extension added by ProtoData Gradle plugin.
     */
    public const val EXTENSION_NAME: String = "protoData"

    /**
     * The name of the Gradle Configuration created by ProtoData Gradle plugin for holding
     * user-defined classpath.
     */
    public const val USER_CLASSPATH_CONFIGURATION_NAME: String = "protoData"
}
