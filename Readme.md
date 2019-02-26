# Dynamically Loaded Gradle Task

[![](https://jitpack.io/v/Wasabi375/dynamicallyLoadedGradleTask.svg)](https://jitpack.io/#Wasabi375/dynamicallyLoadedGradleTask)

A gradle task that let's you run a program and delegate the incremental task execution to it. 

The file information is send via the program arguments:

* inputDir
* outputDir
* isIncremental
* number of files total
* number of Modified
* modified file paths
* number of Added
* added file paths
* number of Removed
* removed file paths
* number of unchanged (should be 0 with current system)
* unchanged file paths


## Result

If the program terminates properly, this is handled as a success case. In case of an unexpected termination (Exception) 
the gradle task fails. 

## Logging

Anything printed to std-out and std-err streams will just be delegated to the gradle std-out and std-err streams.