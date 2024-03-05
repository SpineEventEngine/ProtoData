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

package io.spine.protodata.codegen.java.style

/**
 * The constant to be used in settings to designate "too many".
 *
 * For example, for star imports it would mean "start using on-demand import" when
 * the usage of this package is real madness.
 */
private const val A_LOT = 9999

/**
 * Creates an instance of [ImportOnDemand] with the default values.
 *
 * This function borrows some default values set by IntelliJ Platform and
 * alters others to suite conventions used by Spine SDK.
 *
 * @see [com.intellij.psi.codeStyle.JavaCodeStyleSettings]
 */
public fun importOnDemandDefaults(): ImportOnDemand = importOnDemand {
    // Unlike IntelliJ, which sets the `CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND` to `5`, our
    // default value forces NOT using on-demand imports as much as possible.
    classCount = A_LOT

    // Similarly to static imports, we want them all rather than star imports.
    nameCount = A_LOT
}
