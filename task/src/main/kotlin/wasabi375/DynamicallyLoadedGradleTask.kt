package wasabi375

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties


open class DynamicallyLoadedGradleTask : DefaultTask() {

    lateinit var targetJar: File
    lateinit var className: String

    var failOnNonexistentTarget: Boolean = true

    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    lateinit var input: String


    @TaskAction
    fun execute(inputs: IncrementalTaskInputs){

        if(!targetJar.exists()) {
            if(failOnNonexistentTarget) {
                throw Exception("No target jar found at ${targetJar.path}!")
            } else {
                throw StopExecutionException("No target jar found at ${targetJar.path}!")
            }
        }

        val classLoader = URLClassLoader(arrayOf(targetJar.toURI().toURL()))
        @Suppress("UNCHECKED_CAST") val clazz = Class.forName(className, true, classLoader) as Class<Any>

        val inputDirProperty = clazz.getPropertyWithAnnotation<Any, File>(InputDirectory::class.java)
        val outputDirProperty = clazz.getPropertyWithAnnotation<Any, File>(OutputDirectory::class.java)

        val inputProperty = clazz.getPropertyWithAnnotation<Any, String>(Input::class.java)

        val instance = clazz.newInstance()
        inputDirProperty.set(instance, inputDir)
        outputDirProperty.set(instance, outputDir)
        inputProperty.set(instance, input)

        val taskFunction = clazz.getFunctionWithAnnotation(TaskAction::class.java)

        taskFunction.call(inputs)
    }

    @Suppress("UNCHECKED_CAST")
    fun <R: Any, T>Class<R>.getPropertyWithAnnotation(annotation: Class<out Annotation>): KMutableProperty1<R, T> {

        val kclass = this.kotlin

        val members = kclass.memberProperties.filter { it.annotations.any { it.javaClass == annotation } }

        assert(members.size == 1)

        val prop = members.first()

        return prop as KMutableProperty1<R, T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <R: Any>Class<R>.getFunctionWithAnnotation(annotation: Class<*>): KFunction<R> {
        val kclass = this.kotlin

        val members = kclass.memberFunctions.filter { it.annotations.any { it.javaClass == annotation } }

        assert(members.size == 1)

        val prop = members.first()

        return prop as KFunction<R>
    }
}