#!/usr/bin/env bash

#
# Copyright 2021, TeamDev. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Redistribution and use in source and/or binary forms, with or without
# modification, must retain the above copyright notice and the following
# disclaimer.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

distribution_dir=$(realpath "$0")
distribution_dir=$(dirname "$distribution_dir")
application_dir="$distribution_dir/protodata"

if [[ ! -d "$application_dir" ]]; then
  echo "Directory '$application_dir' doesn't exist."
  exit 1
fi

if [ -z "$(ls -A "$application_dir")" ]; then
  echo "Directory '$application_dir' is empty."
  exit 1
fi

if [ $# -eq 0 ]; then
    target="$HOME/Library"
else
    mkdir -p "$1"
    target="$1"
fi

echo "Installing ProtoData into '$target'..."

cp -r "$application_dir" "$target"

# Try to add the launcher script to PATH.
bin_dir="$target/protodata/bin"

if ! echo "$PATH" | grep -q "$bin_dir"; then
    echo "Adding '$bin_dir' to the PATH..."

    if echo "$SHELL" | grep -q "zsh"; then
        shell_rc="$HOME/.zshrc"
    elif echo "$SHELL" | grep -q "bash" ; then
        shell_rc="$HOME/.bash_profile"
    else
        echo "Shell '$SHELL' is not supported. Please add '$bin_dir' to PATH manually."
        exit 1
    fi

    echo "Adding executables to PATH via '$shell_rc'."

    chmod +x "$bin_dir/protodata"

    echo "export PATH=\"\$PATH:$bin_dir\"" >> "$shell_rc"

    echo "PATH is changed. To update, reload the terminal session or run:"
    echo "    source $shell_rc"
fi

echo "Done."
