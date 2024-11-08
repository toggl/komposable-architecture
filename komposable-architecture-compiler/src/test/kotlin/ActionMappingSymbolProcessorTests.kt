import com.toggl.komposable.processors.ActionMappingSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import sources.ActionSources
import kotlin.test.Test

class ActionMappingSymbolProcessorTests {

    @Test
    fun `Action mapping methods are generated`() {
        actionMappingShouldSucceed(
            sourceFiles = listOf(ActionSources.appAction, ActionSources.settingsAction),
            expectedResult = ActionSources.generatedActionExtensionsFile,
        )
    }

    @Test
    fun `Action mapping methods are generated even when the wrapper action has an extra interface`() {
        actionMappingShouldSucceed(
            sourceFiles = listOf(ActionSources.appActionWithExtraInterface, ActionSources.settingsAction),
            expectedResult = ActionSources.generatedActionExtensionsFile,
        )
    }

    @Test
    fun `Action mapping fails when parent class is not sealed`() {
        actionMappingShouldFail(
            sourceFiles = listOf(ActionSources.appActionNonSealedParent, ActionSources.settingsAction),
            errorMessage = "Parent action com.toggl.komposable.compiler.AppAction.Settings does not have a sealed super type",
        )
    }

    @Test
    fun `Action mapping methods are generated on files without a package`() {
        actionMappingShouldSucceed(
            sourceFiles = listOf(ActionSources.appActionWithoutPackage, ActionSources.settingsActionWithoutPackage),
            expectedResult = ActionSources.generatedActionExtensionsFileWithoutPackage,
        )
    }

    @Test
    fun `If a class annotated with @WrapperAction has multiple properties then the compilation fails`() {
        actionMappingShouldFail(
            sourceFiles = listOf(ActionSources.appActionWithMultipleProperties, ActionSources.settingsAction),
            errorMessage = "AppAction.Settings needs to have exactly one property to be annotated with @WrapperAction",
        )
    }

    private fun actionMappingShouldSucceed(sourceFiles: List<SourceFile>, expectedResult: String) {
        ActionMappingSymbolProcessorProvider().testCompilation(sourceFiles) {
            exitCode shouldBe KotlinCompilation.ExitCode.OK
            val sources = kspGeneratedSources()
            sources shouldHaveSize 1
            val fileText = sources.single().readText()
            fileText shouldBe expectedResult
        }
    }

    private fun actionMappingShouldFail(sourceFiles: List<SourceFile>, errorMessage: String) {
        ActionMappingSymbolProcessorProvider().testCompilation(sourceFiles) {
            exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
            messages shouldContain errorMessage
        }
    }
}
