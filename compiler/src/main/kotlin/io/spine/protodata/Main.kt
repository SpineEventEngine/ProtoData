/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.RendererBuilder
import io.spine.protodata.renderer.SourceSet
import io.spine.protodata.subscriber.Subscriber
import io.spine.protodata.subscriber.SubscriberBuilder
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Launches the CLI application.
 *
 * When the application is done or an unhandled error occurs, exits the process.
 */
public fun main(args: Array<String>): Unit = Run().main(args)

/**
 * Launches the CLI application without finishing the process after execution.
 */
@VisibleForTesting
internal fun launchApp(vararg argv: String) {
    Run().parse(argv.toList())
}

/**
 * The main CLI command which performs the ProtoData code generation tasks.
 *
 * The command accepts class names for [Subscriber]s and a [Renderer] via the `--subscriber` and
 * `--renderer` parameters. Then, using the classpath of the app and the extra classpath supplied
 * via the `--extra-classpath` parameter, loads those classes. Subscribers listen to the Protobuf
 * compiler events, regarding the Protobuf types, listed in
 * the `CodeGeneratorRequest.file_to_generate` as loaded from the `--request` parameter. Finally,
 * the code enhancements produced by the subscribers are supplied to the renderer, which applies
 * them to the source set with the root path, supplied in the `--source-root` parameter.
 */
private class Run : CliktCommand() {

    val subscribers: List<String> by option("--subscriber", "-s", help = """
        The name of a Java class, a subtype of `${Subscriber::class.qualifiedName}`.
        There can be multiple subscribers. To pass more then one value, type:
        
             <...> -s com.foo.Bar -s com.foo.Baz
        
    """.trimIndent()).multiple(required = true)
    val renderer: String by option("--renderer", "-r", help = """
        The name of a Java class, a subtype of `${Renderer::class.qualifiedName}`.
        There can only be one renderer command line per call.
    """.trimIndent()).required()
    val codegenRequestFile: File by option("--request", "-t", help =
    "The path to the binary file containing a serialized instance of " +
            "`${CodeGeneratorRequest.getDescriptor().name}`."
    ).file(
        mustExist = true,
        canBeDir = false,
        canBeSymlink = false,
        mustBeReadable = true
    ).required()
    val sourceRoot: Path by option("--source-root", "--src", help = """
        The path to a directory which contains the source files to be processed.
    """.trimIndent()
    ).path(
        mustExist = true,
        canBeFile = false,
        canBeSymlink = false
    ).required()
    val classPath: List<Path>? by option("--extra-classpath", "--xcp", help = """
        The extra classpath which contains all the `--subscriber` and `--renderer` classes, as well
        as all their dependencies, which are not included as a part of the ProtoData library.
        This may be omitted if the classes are already present in the application's classpath.
        May be one path to a JAR, a ZIP, or a directory. Or may be many paths separated by
        the `${File.pathSeparator}` separator char.
    """.trimIndent()
    ).path(
        mustExist = true,
        mustBeReadable = true
    ).split(File.pathSeparator)

    override fun run() {
        val classLoader = loadExtraClasspath()
        val subscribers = loadSubscribers(classLoader)
        processProtobuf(subscribers)
        val renderer = loadRenderer(subscribers, classLoader)
        val sourceSet = SourceSet.fromContentsOf(sourceRoot)
        val enhanced = renderer.render(sourceSet)
        enhanced.files.forEach {
            it.write()
        }
    }

    private fun loadExtraClasspath(): ClassLoader {
        val contextClassLoader = Thread.currentThread().contextClassLoader!!
        return if (classPath != null) {
            val urls = classPath!!
                .map { it.toUri().toURL() }
                .toTypedArray()
            URLClassLoader(urls, contextClassLoader)
        } else {
            contextClassLoader
        }
    }

    private fun processProtobuf(subscribers: List<Subscriber<*>>) {
        ProtoDataContext.build(*subscribers.toTypedArray())
        val codegenRequest = codegenRequestFile.inputStream().use {
            CodeGeneratorRequest.parseFrom(it)
        }
        ProtobufCompilerContext.emittedEventsFor(codegenRequest)
    }

    private fun loadSubscribers(classLoader: ClassLoader): List<Subscriber<*>> {
        val subscriberBuilder = SubscriberBuilder()
        return subscribers.map {
            subscriberBuilder.createByName(it, classLoader)
        }
    }

    private fun loadRenderer(subscribers: List<Subscriber<*>>, classLoader: ClassLoader): Renderer {
        val rendererBuilder = RendererBuilder()
        subscribers.forEach { rendererBuilder.add(it.producedEnhancements) }
        return rendererBuilder.createByName(renderer, classLoader)
    }
}
