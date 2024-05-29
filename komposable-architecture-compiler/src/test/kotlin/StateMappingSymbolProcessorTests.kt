import com.toggl.komposable.processors.StateMappingSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import sources.StateSources
import kotlin.test.Test

class StateMappingSymbolProcessorTests {
    @Test
    fun `State mapping methods are generated`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(StateSources.settingsState, StateSources.appState)
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
        sources.single().readText().shouldBe(StateSources.generatedStateExtensionsFile)
    }

    @Test
    fun `State mapping methods are generated even when there are @ParentPath annotated props`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(StateSources.settingsStateWithMapping, StateSources.appState)
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
        sources.single().readText().shouldBe(StateSources.generatedStateExtensionsFileWithPathMapping)
    }

    @Test
    fun `State mapping methods are generated even when there are nested @ParentPath annotated props`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(StateSources.settingsStateWithNestedMapping, StateSources.appStateWithNestedValue)
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
        sources.single().readText().shouldBe(StateSources.generatedStateExtensionsFileWithPathNestedMapping)
    }

    @Test
    fun `State mapping methods are generated even when there are multiple values`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(StateSources.settingsStateWithTwoValues, StateSources.appStateWithTwoValues)
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
        sources.single().readText().shouldBe(StateSources.generatedStateExtensionsFileWithTwoValues)
    }

    @Test
    fun `State mapping methods generation fails when parent class is not data class`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(StateSources.settingsState, StateSources.appStateNonDataClass)
            symbolProcessorProviders = listOf(StateMappingSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        // Act
        val result = compilation.compile()

        // Assert
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `State mapping methods generation fails when parent class's nested class is not data class`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(StateSources.settingsStateWithNestedMapping, StateSources.appStateWithNestedNonDataClassValue)
            symbolProcessorProviders = listOf(StateMappingSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        // Act
        val result = compilation.compile()

        // Assert
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }
}
