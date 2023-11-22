import com.toggl.komposable.processors.StateMappingSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StateMappingSymbolProcessorTests {
    @Test
    fun `State mapping methods are generated`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFiles.settingsState, SourceFiles.appState)
            symbolProcessorProviders = listOf(StateMappingSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        // Act
        val result = compilation.compile()

        // Assert
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val sources = result.kspGeneratedSources()
        sources.size.shouldBe(1)
        sources.single().readText().shouldBe(SourceFiles.generatedStateExtensionsFile)
    }
}
