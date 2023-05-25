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

@file:Suppress("MaxLineLength")

package io.spine.protodata.cli

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.option.OptionsProvider
import io.spine.protodata.config.ConfigurationFormat.JSON
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.config.ConfigurationFormat.PROTO_JSON
import io.spine.protodata.config.ConfigurationFormat.YAML
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.Renderer
import java.io.File.pathSeparator

public object PluginParam : Parameter(
    name = "--plugin",
    shortName = "-p",
    help = """
        The name of a Java class, a subtype of `${Plugin::class.qualifiedName}`.
        There can be multiple providers. To pass more than one value, type:
           `<...> ${dash.p} com.foo.MyEntitiesPlugin ${dash.p} com.foo.OtherEntitiesPlugin`.
        """
)

public object RendererParam : Parameter(
    name = "--renderer",
    shortName = "-r",
    help = """
        The name of a Java class, a subtype of `${Renderer::class.qualifiedName}`.
        There can only be multiple renderers. To pass more than one value, type:
           `<...> ${dash.r} com.foo.MyJavaRenderer ${dash.r} com.foo.MyKotlinRenderer`.
        """
)

public object OptionProviderParam : Parameter(
    name = "--option-provider",
    shortName = "--op",
    help = """
        The name of a Java class, a subtype of `${OptionsProvider::class.qualifiedName}`.
        There can be multiple providers.
        Spine SDK `options.proto` and `time_options.proto` are provided by default.
        To pass more than one value, type:
           `<...> ${dash.op} com.foo.TypeOptionsProvider ${dash.op} com.foo.FieldOptionsProvider`.
        """
)

public object RequestParam : Parameter(
    name = "--request",
    shortName = "-t", // "-r" is already taken.
    help = "The path to the binary file containing a serialized instance of " +
            "`${CodeGeneratorRequest.getDescriptor().name}`."
)

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

public object TargetRootParam : Parameter(
    name = "--target-root",
    altName = "--destination",
    shortName = "-d",
    help = """
        The path where the processed files should be placed.
        May be the same as `${SourceRootParam.name}`. For editing files in-place, skip this option.
        
        Multiple directories can be listed separated by the system-dependent path separator (`$ps`). 
        In such a case, the number of directories must match the number of `${dash.src}` directories.
        Source and target directories are paired up according to the order they are provided in,
        so that the files from first source are written to the first target and so on.
        """
)

public object UserClasspathParam : Parameter(
    name = "--user-classpath",
    shortName = "--ucp",
    help = """
        The user classpath which contains all `${ddash.renderer}` classes, user-defined policies,
        views, events, etc., as well as all their dependencies, which are not included as a part of
        the ProtoData library. This option may be omitted if the classes are already present in
        the ProtoData classpath. May be one path to a JAR, a ZIP, or a directory. Or, may be many
        paths separated by the system-dependent path separator (`$ps`).
        """
)

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

public object ConfigValueParam : Parameter(
    name = "--configuration-value",
    shortName = "--cv",
    help = """
        Custom configuration for ProtoData.
        May be a JSON or a YAML.
        Must be used alongside with `${ddash.confFmt}`.
        """
)

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

public object InfoLoggingParam : Parameter(
    name = "--info",
    shortName = "-I",
    help = """
        Set log level to `INFO`.        
    """
)

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
    val r = lazy { RendererParam.shortName }
    val op = lazy { OptionProviderParam.shortName }
    val src = lazy { SourceRootParam.shortName }
}

/**
 * Abbreviations for long plugin names to be used in `help` texts.
 */
@Suppress("ClassName") // for better readability
private object ddash {
    val tr = lazy { TargetRootParam.name }
    val confVal = lazy { ConfigValueParam.name }
    val confFmt = lazy { ConfigFormatParam.name }
    val renderer = lazy { RendererParam.name }
}

/**
 * Abbreviation for using inside strings.
 */
private val ps = pathSeparator
