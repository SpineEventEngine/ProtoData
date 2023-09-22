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
import io.spine.protodata.config.ConfigurationFormat.JSON
import io.spine.protodata.config.ConfigurationFormat.PLAIN
import io.spine.protodata.config.ConfigurationFormat.PROTO_JSON
import io.spine.protodata.config.ConfigurationFormat.YAML
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.renderer.Renderer
import io.spine.tools.code.Language
import java.io.File.pathSeparator

/**
 * The command-line parameter for specifying ProtoData plugins used in
 * the code generation process.
 */
public object PluginParam : Parameter(
    name = "--plugin",
    shortName = "-p",
    help = """
        The name of a Java class, a subtype of `${Plugin::class.qualifiedName}`.
        There can be multiple providers. To pass more than one value, type:
           `<...> ${dash.p} com.foo.MyEntitiesPlugin ${dash.p} com.foo.OtherEntitiesPlugin`.
        """
)

/**
 * The command-line parameter for specifying renderers used in
 * the code generation process.
 */
public object RendererParam : Parameter(
    name = "--renderer",
    shortName = "-r",
    help = """
        The name of a Java class, a subtype of `${Renderer::class.qualifiedName}`.
        There can only be multiple renderers. To pass more than one value, type:
           `<...> ${dash.r} com.foo.MyJavaRenderer ${dash.r} com.foo.MyKotlinRenderer`.
        """
)

/**
 * The command-line parameter for specifying the path to the file with
 * serialized [CodeGeneratorRequest].
 */
public object RequestParam : Parameter(
    name = "--request",
    shortName = "-t", // "-r" is already taken.
    help = "The path to the binary file containing a serialized instance of " +
            "`${CodeGeneratorRequest.getDescriptor().name}`."
)

/**
 * The command-line parameter for specifying the source and target paths.
 */
public object PathsParam : Parameter(
    name = "--paths",
    shortName = "-P",
    help = """
        Paths specifying where the files for processing are located and where to put them after
        processing.
        
        The value should consist of three parts, separated by the path separator character (`$ps`):
          1. The language that the files use and the name of the code generator that created
             the files. The language should be a fully qualified name of 
             a `${Language::class.qualifiedName}` subclass that represents the language.
             Alternatively, the language can be one of the preset values: 
             ${knownLanguages.keys.joinToString()}.
             The code generator name is a plain string that identifies the name of the software 
             component, a `protoc` plugin or a separate program, that generated these code files.
             The code generator name should go after the language in parentheses with no
             white space. When using the default Protobuf code generation, the code generator name
             may be omitted.
          2. The source path. Must be an absolute path where the code files are located. If there
             are no code files, i.e. ProtoData should generate all the code from scratch, this part
             may be left blank. 
          3. The target path. Must be an absolute path where the code files should be placed after
             processing. This path must either lead to an empty directory, or not lead to any
             existing FS object.
             
        For specifying several source sets, supply this param multiple times.
        
        Example 1.
            `<...> ${ddash.path} java(grpc):/Users/me/foo/temp/generated/main/grpc:/Users/me/foo/gen/main/grpc`
            This example provides a source set for Java gRPC stubs placed under
            `./temp/generated/main/grpc`. After ProtoData finishes processing the files, it should
            place them under `./gen/main/grpc`.
            
        Example 2.
            `<...> ${ddash.path} org.example.Fortran(4gen):/a/b/c:a/b/d \
                   ${ddash.path} org.example.Cobol(cobolmine):/x/y:/x/w`
            In this example we supply two source sets with different languages to be processed in
            the same run.
            
        Example 3.
            `<...> ${ddash.path} kotlin;D:\Foo\Bar;D:\Foo\Baz`
            In this example we supply the Kotlin source set generated by the default. Note the
            absence of the code generator name after the language name.
        
        Example 4.
            `<...> ${ddash.path} javascript(myself)::/a/b/c`
            In this example we supply a JS source set that does not provide any existing files.
            Here, it is expected that ProtoData will generate all the files into the target
            directory `/a/b/c` from scratch.
        """
)

/**
 * The command-line parameter for composing the user-defined classpath.
 */
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

/**
 * The command-line parameter for specifying the path to the file with custom
 * configuration for ProtoData.
 */
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
 * The command-line parameter for specifying custom configuration values.
 */
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
    val r = lazy { RendererParam.shortName }
}

/**
 * Abbreviations for long plugin names to be used in `help` texts.
 */
@Suppress("ClassName", "SpellCheckingInspection") // for better readability in `help` texts.
private object ddash {
    val confVal = lazy { ConfigValueParam.name }
    val confFmt = lazy { ConfigFormatParam.name }
    val renderer = lazy { RendererParam.name }
    val path = lazy { PathsParam.name }
}

/**
 * Abbreviation for using inside strings.
 */
private val ps = pathSeparator
