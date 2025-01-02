/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:Suppress("MaxLineLength")

package io.spine.protodata.params

import io.spine.protodata.plugin.Plugin

/**
 * The command-line parameter for specifying the name of the file which stores an instance of
 * [PipelineParameters] in [PROTO_JSON][io.spine.protodata.util.Format.PROTO_JSON] format.
 */
public object ParametersFileParam : Parameter(
    name = "--params",
    shortName = "-P",
    help = """
        The path to the file with the serialized instance of `PipelineParameters` to 
        be passed to the pipeline. The file must be in `pb.json` format.
    """.trimIndent()
)

/**
 * The part of the console output used for showing how to use the dollar sign for
 * a nested binary class name.
 *
 * The backslash symbol (`\\`) preceding the dollar sign (`\$)` is used for escaping
 * the dollar in the console input because otherwise it would be considered as
 * a reference to a shell script variable.
 */
@Suppress("TopLevelPropertyNaming") // for brevity.
private const val escDollar: String = "\\\$"

/**
 * The command-line parameter for specifying ProtoData plugins used in
 * the code generation process.
 */
public object PluginParam : Parameter(
    name = "--plugin",
    shortName = "-p",
    help = """
        The name of a Java or a Kotlin class, a subtype of `${Plugin::class.qualifiedName}`.
        For nested classes please use binary names, with the `$` delimiter before a nested class.
        To pass more than one plugin class, type:
           `<...> ${dash.p} com.foo.FirstPlugin ${dash.p} com.foo.Second${escDollar}NestedPlugin`.
        """
)

/**
 * The command-line parameter for composing the user-defined classpath.
 */
public object UserClasspathParam : Parameter(
    name = "--user-classpath",
    shortName = "--ucp",
    help = """
        The user classpath which contains all `${ddash.plugin}` classes, as well as all
        their dependencies, which are not included as a part of the ProtoData library. 
        This option may be omitted if the classes are already present in the ProtoData classpath. 
        May be one path to a JAR, a ZIP, or a directory. 
        Or, may be many paths separated by the system-dependent path separator (`$ps`).
        """
)

/**
 * The command-line parameter which turns the `INFO` logging level on.
 */
public object InfoLoggingParam : Parameter(
    name = "--info",
    shortName = "-I",
    help = """
        Set log level to `INFO`.        
    """
)

/**
 * The command-line parameter which turns the `DEBUG` logging level on.
 */
public object DebugLoggingParam : Parameter(
    name = "--debug",
    shortName = "-D",
    help = """
        Set log level to `DEBUG`.        
    """
)

/**
 * Abbreviations for short parameter names to be used inside `help` texts.
 */
@Suppress("ClassName") // for better readability
private object dash {
    val p = lazy { PluginParam.shortName }
}

/**
 * Abbreviations for long (double-dash) parameter names to be used in `help` texts.
 */
@Suppress("ClassName", "SpellCheckingInspection") // for better readability in `help` texts.
private object ddash {
    val plugin = lazy { PluginParam.name }
}
