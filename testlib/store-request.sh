#!/bin/bash

#
# This script is a `protoc` plugin which stores the `CodeGeneratorRequest` passed
# to stdin of the script.
#
# See `build.gradle.kts` and the `protobuf` block for the code adding this script
# as a `protoc` plugin.
#

# Get the directory in which the script resides. It's the root of the subproject.
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# We put the file under the `build/resources/test` so that tests can pick it up.
TARGET_DIR="$SCRIPT_DIR/build/resources/test/pipeline-setup"
mkdir -p "$TARGET_DIR"

OUTPUT_FILE="$TARGET_DIR/CodeGeneratorRequest.binpb"

cat >"$OUTPUT_FILE"
