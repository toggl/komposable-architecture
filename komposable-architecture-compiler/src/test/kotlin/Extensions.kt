import com.tschuchort.compiletesting.KotlinCompilation
import java.io.File

private val KotlinCompilation.Result.workingDir: File get() = outputDirectory.parentFile!!

// HACK: Workaround for finding KSP files. See:
// https://github.com/tschuchortdev/kotlin-compile-testing/issues/129
fun KotlinCompilation.Result.kspGeneratedSources(): List<File> {
    val kspWorkingDir = workingDir.resolve("ksp")
    val kspGeneratedDir = kspWorkingDir.resolve("sources")
    val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
    return kotlinGeneratedDir.walkTopDown().filter { it.isFile }.toList()
}
