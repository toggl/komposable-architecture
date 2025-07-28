@file:OptIn(ExperimentalCompilerApi::class)

import com.toggl.komposable.processors.StateMappingSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Ignore
import sources.StateSources
import kotlin.test.Test

class StateMappingSymbolProcessorTests {
    @Test
    fun `State mapping methods are generated`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appState, StateSources.settingsState),
            expectedResult = StateSources.generatedStateExtensionsFile,
        )
    }

    @Test
    fun `State mapping methods are generated even when there are @ParentPath annotated props`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appState, StateSources.settingsStateWithMapping),
            expectedResult = StateSources.generatedStateExtensionsFileWithPathMapping,
        )
    }

    @Test
    fun `State mapping methods are generated even when there are nested @ParentPath annotated props`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appStateWithNestedValue, StateSources.settingsStateWithNestedMapping),
            expectedResult = StateSources.generatedStateExtensionsFileWithPathNestedMapping,
        )
    }

    @Test
    fun `State mapping methods are generated even when there are nested @ParentPath annotated props and the nested class is in a separate file`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(
                StateSources.appStateWithNestedValueInSeparateFile,
                StateSources.standaloneNestedValue,
                StateSources.settingsStateWithNestedMapping,
            ),
            expectedResult = StateSources.generatedStateExtensionsFileWithPathNestedMapping,
        )
    }

    @Test
    fun `State mapping methods are generated even when there are multiple values`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appStateWithTwoValues, StateSources.settingsStateWithTwoValues),
            expectedResult = StateSources.generatedStateExtensionsFileWithTwoValues,
        )
    }

    @Test
    fun `State mapping methods are generated even when there are multiple nested @ParentPath annotated props`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appStateWithTwoNestedValues, StateSources.settingsStateWithTwoNestedMapping),
            expectedResult = StateSources.generatedStateExtensionsFileWithTwoPathNestedMapping,
        )
    }

    @Test
    fun `State mapping methods are generated even when there are deeper nested @ParentPath annotated props`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appStateWithDeeperNestedValue, StateSources.settingsStateWithDeeperNestedMapping),
            expectedResult = StateSources.generatedStateExtensionsFileWithDeeperPathNestedMapping,
        )
    }

    @Test
    fun `State mapping methods are generated for multiple child states`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.appStateMultipleChildStates, StateSources.settingsState, StateSources.authState),
            expectedResult = StateSources.generatedStateExtensionsFileForMultipleChildStates,
        )
    }

    @Test
    fun `State mapping methods are generated multilevel complex cases`() {
        stateMappingShouldSucceed(
            sourceFiles = listOf(StateSources.complexAppState, StateSources.complexSettingsState, StateSources.authState),
            expectedResult = StateSources.generatedStateExtensionsFileForComplexState,
        )
    }

    @Test
    fun `State mapping methods generation fails when parent class is not data class`() {
        stateMappingShouldFail(
            sourceFiles = listOf(StateSources.appStateNonDataClass, StateSources.settingsState),
            errorMessage = "AppState must be a data class to create state mappings.",
        )
    }

    @Ignore("We don't yet guarantee this kind of validation")
    @Test
    fun `State mapping methods generation fails when parent class's nested class is not data class`() {
        stateMappingShouldFail(
            sourceFiles = listOf(StateSources.appStateWithNestedNonDataClassValue, StateSources.settingsStateWithNestedMapping),
            errorMessage = "TODO",
        )
    }

    @Ignore("We don't yet guarantee this kind of validation")
    @Test
    fun `State mapping methods generation fails when there are invalid @ParentPath properties`() {
        stateMappingShouldFail(
            sourceFiles = listOf(StateSources.appState, StateSources.settingsStateWithInvalidMapping),
            errorMessage = "TODO",
        )
    }

    private fun stateMappingShouldSucceed(sourceFiles: List<SourceFile>, expectedResult: String) {
        StateMappingSymbolProcessorProvider().testCompilation(sourceFiles) {
            exitCode shouldBe KotlinCompilation.ExitCode.OK
            val sources = kspGeneratedSources()
            sources shouldHaveSize 1
            val fileText = sources.single().readText()
            fileText shouldBe expectedResult
        }
    }

    private fun stateMappingShouldFail(sourceFiles: List<SourceFile>, errorMessage: String) {
        StateMappingSymbolProcessorProvider().testCompilation(sourceFiles) {
            exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
            messages shouldContain errorMessage
        }
    }
}
