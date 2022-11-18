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

package io.spine.protodata.cli.param

/**
 * A parameter passed to the ProtoData command-line application.
 */
public sealed class Parameter(

    /**
     * A long name of the parameter, which usually comes with the `--` prefix.
     */
    public val name: String,

    /**
     * Alternative long parameter name.
     */
    public val altName: String = name,

    /**
     * A short name of the parameter, which conventionally comes with the `-` prefix
     * if the short name is one letter, and with `--` prefix for two or more letters.
     */
    public val shortName: String,

    /**
     * Description of the parameter with the usage instructions which
     * could be passed as a raw string.
     */
    help: String
) {

    /**
     * Description of the parameter with the usage instructions.
     */
    public val help: String

    init {
        this.help = help.trimIndent()
    }

    final override fun hashCode(): Int = name.hashCode()

    final override fun toString(): String = name

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parameter) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (shortName != other.shortName) return false
        if (help != other.help) return false

        return true
    }
}
