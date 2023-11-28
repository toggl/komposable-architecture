import com.toggl.komposable.processors.ActionMappingSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ActionMappingSymbolProcessorTests {
    @Test
    fun `Action mapping methods are generated`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFiles.appAction, SourceFiles.settingsAction)
            symbolProcessorProviders = listOf(ActionMappingSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        // Act
        val result = compilation.compile()

        // Assert
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val sources = result.kspGeneratedSources()
        sources.size.shouldBe(1)
        sources.single().readText().shouldBe(SourceFiles.generatedActionExtensionsFile)
    }

    @Test
    fun `Action mapping methods are generated on files without a package`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFiles.appActionWithoutPackage, SourceFiles.settingsActionWithoutPackage)
            symbolProcessorProviders = listOf(ActionMappingSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        // Act
        val result = compilation.compile()

        // Assert
        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        val sources = result.kspGeneratedSources()
        sources.size.shouldBe(1)
        sources.single().readText().shouldBe(SourceFiles.generatedActionExtensionsFileWithoutPackage)
    }

    @Test
    fun `If a class annotated with @WrapperAction has multiple properties then the compilation fails`() {
        // Arrange
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFiles.appActionWithMultipleProperties, SourceFiles.settingsAction)
            symbolProcessorProviders = listOf(ActionMappingSymbolProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }

        // Act
        val result = compilation.compile()

        // Assert
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR

        result.messages.contains("AppAction.Settings needs to have exactly one property to be annotated with @WrapperAction")

        val sources = result.kspGeneratedSources()
        sources.size.shouldBe(0)
    }
}
