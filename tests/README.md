# ProtoData integration tests

This Gradle project contains tests for the ProtoData tool and Gradle plugin.

Despite being part of `ProtoData` Git repository, it is a separate Gradle project.
In order to fetch artifacts from the primary Gradle project, `tests` includes it
via a Gradle included build.

## Running tests

To run integration tests from within this working directory, simply execute `./gradlew build` from
the `./tests` directory.

Also, tests run as a part of the build of the primary Gradle project. Just run `./gradlew build`
from the repo's root directory.

## Maintenance

`tests` project uses its own Gradle wrapper and a copy of `buildSrc`.

In order to update `buildSrc`, run `./pull` from the root directory of the repo.

In order to update the Gradle wrapper, run `./gradlew wrapper --gradle-version <version>` from
the `./tests` directory, just like with any other Gradle Project.
