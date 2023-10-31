import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language

object SourceFiles {

    val settingsAction = SourceFile.kotlin(
        "SettingsAction.kt",
        """
    sealed interface SettingsAction {
        data class ChangeSomeSetting(val newValue: Boolean) : SettingsAction
    }
        """.trimIndent(),
    )

    val appAction = SourceFile.kotlin(
        "AppAction.kt",
        """
    import com.toggl.komposable.architecture.WrapperAction
    
    sealed interface AppAction {
        data object ClearList : AppAction
    
        @WrapperAction
        data class Settings(val settingsAction: SettingsAction) : AppAction
    }
        """.trimIndent(),
    )

    val appActionWithMultipleProperties = SourceFile.kotlin(
        "AppAction.kt",
        """
    import com.toggl.komposable.architecture.WrapperAction
    
    sealed interface AppAction {
        data object ClearList : AppAction
    
        @WrapperAction
        data class Settings(val settingsAction: SettingsAction, val foo: String) : AppAction
    }
        """.trimIndent(),
    )

    @Language("kotlin")
    val generatedSettingsFile = """import AppAction.Settings

public fun mapAppActionToSettingsAction(appAction: AppAction): SettingsAction? = if(appAction is
    AppAction.Settings) appAction.settingsAction else null

public fun mapSettingsActionToAppAction(settingsAction: SettingsAction): AppAction.Settings =
    AppAction.Settings(settingsAction)
    """.trimIndent()
}
