# ProtoData

_ProtoData_ is a collection of tools for generating quality domain models from Protobuf definitions.

The project is under development right now. Proceed with caution.

## Installation

### Gradle

If you use Java and Gradle, you can install and launch ProtoData easily.
See the [Gradle plugin](gradle-plugin/README.md) doc for more info.

### *nix

To install ProtoData on a *nix system:

 1. [Download](https://github.com/SpineEventEngine/ProtoData/packages/710696) 
    the `executable-<version>.jar` archive.

 2. Unzip the archive:
 ```
 unzip -d ./target/dir path/to/protodata.jar
 ```

 3. Run the installer script:
 ```
 ./target/dir/install.sh
 ```
 By default, the tool is installed into `$HOME/Library/` dir. To change the installation dir,
 pass it as a parameter to the `install.sh` script:
 ```
 ./target/dir/install.sh $HOME/custom/installation/dir
 ```
 In any case, the directory must exist. Otherwise, `install.sh` will exit with an error.

 4. If you use either `bash` or `zsh` shell, you will be asked to run a `source` command to update
 the `PATH` environmental variable. If you use another shell, you will have to update the `PATH`
 variable manually.

Now ProtoData CLI should be accessible. To check the correctness of the installation, run:
```
protodata -h
```

### Windows

There is currently no installation script for Windows.

Start using the app:

 1. [Download](https://github.com/SpineEventEngine/ProtoData/packages/710696) and extract 
    the `executable-<version>.jar` archive.

 2. To launch the app, launch the `bin/protodata.bat` script.

Note that it is necessary to keep the structure of the archive intact: the scripts in `bin` rely
on the relative path to other files in the archive.
