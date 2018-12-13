# Dynamically Loaded Gradle Task

[![](https://jitpack.io/v/Wasabi375/dynamicallyLoadedGradleTask.svg)](https://jitpack.io/#Wasabi375/dynamicallyLoadedGradleTask)

A gradle tasks that let's you load a jar file when executed and delegate the incremental task execution to this jar.

Usage:
```groovy
task loadTask(type: DynamicallyLoadedGradleTask) {
    
    targetJar = new File("path to jar")
    className = "fully qualified class name of Task"
    failOnNonexistentTarget = true  // fail the task if the jar does not exist, default true
    
    inputDir = file("input dir")
    outputDir = file("output dir")
}
```

The loaded task class needs to have a constructor which takes 2 File arguments, the input directory and the output.

```kotlin
class LoadedTask(inputDir: File, outputDir: File) : Task(inputDir, outputDir) {

    override fun execute(incrementalInput: IncrementalInput) {
        // implementation
    }
}
```