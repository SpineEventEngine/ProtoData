# ProtoData Gradle plugin

This Gradle plugin allows Java developers launch ProtoData without extra CLI commands.

## Usage

### Applying the plugin

To apply the plugin to the project, use the `plugins { }` block syntax.

```kotlin
plugins {
    id("io.spine.proto-data") version("<ProtoData version>")
}
```

Or, alternatively, use the old-fashioned `buildscript { }` syntax.

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    
    dependencies {
        classpath("io.spine:proto-data:<ProtoData version>")
    }
}

apply(plugin = "io.spine.proto-data")
```

See the plugin [homepage](https://plugins.gradle.org/plugin/io.spine.proto-data) for more.

### Installing and launching ProtoData

When using ProtoData for the first time, you will have to install the executable.
To do so, run:
```
./gradlew installProtoData
```
After running the task, the ProtoData executable might not turn up in the `PATH` environment
variable at once. The command will print instructions on how to finalize the installation.

If your environment does not allow you to execute those instructions, e.g., on a CI instance, you
can specify the ProtoData installation location by hand:
```
./gradlew installProtoData -PprotoDataLocation="my/custom/path"
```

Now, as ProtoData is installed, simply run the build as usual. ProtoData will be launched
automatically before Java compilation.

If you used the custom installation path, just add the same Gradle property to all the other Gradle
invocations. For example:
```
./gradlew build -PprotoDataLocation="my/custom/path"
```

See [this doc](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties)
for more ways to define a Gradle property to avoid extra CLI arguments.

### Configuration

ProtoData requires Renderers and Plugins for meaningful operation. You can specify those and more
via a Gradle extension.

Here is the complete list of configuration options:

Name | Format | Description | Default value
--- | --- | --- | ---
`renderers` | Java class names | Implementations of `io.spine.protodata.renderer.Renderer`. Renderer ordering is preserved. | N/A
`plugins` | Java class names | Implementations of `io.spine.protodata.plugin.Plugin`. | N/A
`optionProviders` | Java class names | Implementations of `io.spine.protodata.option.OptionProvider`. | N/A
`requestFile` | File path | Where the serialized `CodeGeneratorRequest` should be stored. | A file under the `build` dir.
`source` | Directory path | Where the Protobuf sources are located. Files under `source` may be overridden by ProtoData. | `"$projectDir/generated/main/java"`
`generateProtoTasks` | Task names | Tasks which generate code from Protobuf and must be executed before ProtoData. | `"generateProto"`
Configuration `protoData` | Dependencies | The dependencies required to launch ProtoData with the given args. | N/A

A complete configuration may look as follows:
```kotlin
plugins {
    id("io.spine.proto-data") version("<ProtoData version>")
}

protoData {
    renderers("com.acme.MyInsertionPointPrinter", "com.acme.MyRenderer", "org.example.Renderer")
    plugins("com.acme.MyPlugin")
    optionProviders("com.acme.MyOptions")
    requestFile("${rootProject.buildDir}/commonRequestFile/request.bin")
    source("$projectDir/generated/main/customSourceSet/")
    generateProtoTasks("generateMyProto", "generateCustomSourceSet")
}

dependencies {
    protoData(project(":my-protodata-plugin"))
    protoData("org.example:protodata-plugin:1.42")
}

```

## Caveat

This plugin is still in its early development stages.
We do not yet support generation for two source sets in a single Project independently, e.g., for
`main` and `test` sources.

Also, the plugin relies on the Java Project structure, the Java Gradle plugin, the Protobuf Gradle
plugin, and the ProtoData Maven repository.

To make everything work, at this stage, users have to add the following config:

```kotlin
plugins {
    java
    id("com.google.protobuf") version("<Protobuf plugin version>")
    id("io.spine.proto-data") version("<ProtoData version>")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SpineEventEngine/ProtoData")
        credentials {
            username = "<GitHub Actor>"
            password = "<GitHub Token/Personal Access Token>"
        }
    }
}
```

Users who wish to extend ProtoData must also add the dependency to the API:
```kotlin
dependencies {
    api("io.spine.protodata:compiler:<ProtoData version>")
}
```
