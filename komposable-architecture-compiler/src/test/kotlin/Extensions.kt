import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
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

fun SymbolProcessorProvider.testCompilation(
    sourceFiles: List<SourceFile>,
    assert: KotlinCompilation.Result.() -> Unit,
) {
    // Arrange
    val compilation = KotlinCompilation().apply {
        sources = sourceFiles
        symbolProcessorProviders = listOf(this@testCompilation)
        inheritClassPath = true
        messageOutputStream = System.out
    }

    // Act
    val result: KotlinCompilation.Result = compilation.compile()

    // Assert
    result.assert()
}
