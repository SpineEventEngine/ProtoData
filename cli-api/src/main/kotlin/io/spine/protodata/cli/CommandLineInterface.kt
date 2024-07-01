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

package io.spine.protodata.cli

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.settings.Format.JSON
import io.spine.protodata.settings.Format.PLAIN
import io.spine.protodata.settings.Format.PROTO_JSON
import io.spine.protodata.settings.Format.YAML

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
 * The command-line parameter for specifying the path to the file with
 * serialized [CodeGeneratorRequest].
 */
public object RequestParam : Parameter(
    name = "--request",
    shortName = "-t", // "-r" was already taken, now it's available. Shall we rename?
    help = "The path to the binary file containing a serialized instance of " +
            "`${CodeGeneratorRequest.getDescriptor().name}`."
)

/**
 * The command-line parameter for specifying the path to the directory with
 * source files to be processed.
 */
public object SourceRootParam : Parameter(
    name = "--source-root",
    shortName = "--src",
    help = """
        The path to a directory which contains the source files to be processed.
        Skip this argument if there is no initial source to modify.
        
        Multiple directories can be listed separated by the system-dependent path separator (`$ps`). 
        In such a case, the number of directories must match the number of ${ddash.tr}
        directories; source and target directories are paired up according to the order
        they are provided in, so that the files from first source are written to
        the first target and so on.
        
        When specifying multiple directories, some of them are allowed to be non-existent.
        They will just be ignored along with their paired targets. But at least one directory
        must exist. Otherwise, the process will end up with an error.
        """
)

/**
 * The command-line parameter for specifying the path to the directory where
 * to put the processed files.
 */
public object TargetRootParam : Parameter(
    name = "--target-root",
    shortName = "--target",
    help = """
        The path where the processed files should be placed.
        May be the same as `${SourceRootParam.name}`. For editing files in-place, skip this option.
        
        Multiple directories can be listed separated by the system-dependent path separator (`$ps`). 
        In such a case, the number of directories must match the number of `${dash.src}` directories.
        Source and target directories are paired up according to the order they are provided in,
        so that the files from first source are written to the first target and so on.
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
 * The command-line parameter for specifying the path to the file with custom
 * configuration for ProtoData.
 */
@Deprecated(message = "Use `SettingsDirParam` instead.")
public object ConfigFileParam : Parameter(
    name = "--configuration-file",
    shortName = "-c",
    help = """
        File which contains the custom configuration for ProtoData.

        May be a JSON, a YAML, or a binary Protobuf file.
        JSON files must have `.json` extension.
        JSON files with Protobuf JSON format must have `.pb.json` extension.
        YAML files must have `.yml` or `.yaml` extension.
        Protobuf binary files must have `.pb` or `.bin` extension.
        Messages must not be delimited.
        """
)

/**
 * The command-line parameter for specifying the path to the directory with
 * setting files for ProtoData plugins.
 */
public object SettingsDirParam : Parameter (
    name = "--settings-dir",
    shortName = "-d",
    help = """
        A directory which contains setting files for ProtoData plugins.
        
        Setting files may be a JSON, a YAML, or a binary Protobuf file.
        A name of the file must match the name of the plugin class, with the extension
        corresponding to the format of the file:
         * JSON files must have `.json` extension.
         * JSON files with Protobuf JSON format must have `.pb.json` extension.
         * YAML files must have `.yml` or `.yaml` extension.
         * Protobuf binary files must have `.pb` or `.bin` extension.
        Messages must not be delimited.
        """
)

/**
 * The command-line parameter for specifying custom configuration values.
 */
@Deprecated(message = "Use `SettingsDirParam` instead.")
public object ConfigValueParam : Parameter(
    name = "--configuration-value",
    shortName = "--cv",
    help = """
        Custom configuration for ProtoData.
        May be a JSON or a YAML.
        Must be used alongside with `${ddash.confFmt}`.
        """
)

/**
 * The command-line parameter for specifying the format of a custom configuration.
 */
@Deprecated(message = "Use `SettingsDirParam` instead.")
public object ConfigFormatParam : Parameter(
    name = "--configuration-format",
    shortName = "--cf",
    help = """
        The format of the custom configuration.
        Must be one of: `yaml`, `json`, `proto_json`, `plain`.
        Must be used alongside with `${ddash.confVal}`.
        """
) {
    public fun options(): List<String> =
        setOf(YAML, JSON, PROTO_JSON, PLAIN).map { it.name.lowercase() }
}

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
    val src = lazy { SourceRootParam.shortName }
}

/**
 * Abbreviations for long (double-dash) parameter names to be used in `help` texts.
 */
@Suppress("ClassName", "SpellCheckingInspection") // for better readability in `help` texts.
private object ddash {
    val tr = lazy { TargetRootParam.name }
    @Suppress("DEPRECATION")
    val confVal = lazy { ConfigValueParam.name }
    @Suppress("DEPRECATION")
    val confFmt = lazy { ConfigFormatParam.name }
    val plugin = lazy { PluginParam.name }
}
