import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language

object SourceFiles {

    val settingsState = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        data class SettingsState(val booleanValue: Boolean)
        """.trimIndent(),
    )

    val settingsAction = SourceFile.kotlin(
        "SettingsAction.kt",
        """
    package com.toggl.komposable.compiler
    
    sealed interface SettingsAction {
        data class ChangeSomeSetting(val newValue: Boolean) : SettingsAction
    }
        """.trimIndent(),
    )

    val settingsActionWithoutPackage = SourceFile.kotlin(
        "SettingsAction.kt",
        """
    sealed interface SettingsAction {
        data class ChangeSomeSetting(val newValue: Boolean) : SettingsAction
    }
        """.trimIndent(),
    )

    val appState = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
        
        @ChildStates(SettingsState::class)
        data class AppState(
            val someList: List<String>,
            val booleanValue: Boolean
        )
        """.trimIndent(),
    )

    val appAction = SourceFile.kotlin(
        "AppAction.kt",
        """
    package com.toggl.komposable.compiler
    
    import com.toggl.komposable.architecture.WrapperAction
    
    sealed interface AppAction {
        data object ClearList : AppAction
    
        @WrapperAction
        data class Settings(val settingsAction: SettingsAction) : AppAction
    }
        """.trimIndent(),
    )

    val appActionWithoutPackage = SourceFile.kotlin(
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
    package com.toggl.komposable.compiler
    
    import com.toggl.komposable.architecture.WrapperAction
    
    sealed interface AppAction {
        data object ClearList : AppAction
    
        @WrapperAction
        data class Settings(val settingsAction: SettingsAction, val foo: String) : AppAction
    }
        """.trimIndent(),
    )

    @Language("kotlin")
    val generatedActionExtensionsFile = """package com.toggl.komposable.compiler

import com.toggl.komposable.compiler.AppAction.Settings

public fun mapAppActionToSettingsAction(appAction: AppAction): SettingsAction? = if(appAction is
    AppAction.Settings) appAction.settingsAction else null

public fun mapSettingsActionToAppAction(settingsAction: SettingsAction): AppAction.Settings =
    AppAction.Settings(settingsAction)
"""

    @Language("kotlin")
    val generatedActionExtensionsFileWithoutPackage = """import AppAction.Settings

public fun mapAppActionToSettingsAction(appAction: AppAction): SettingsAction? = if(appAction is
    AppAction.Settings) appAction.settingsAction else null

public fun mapSettingsActionToAppAction(settingsAction: SettingsAction): AppAction.Settings =
    AppAction.Settings(settingsAction)
"""

    @Language("kotlin")
    val generatedStateExtensionsFile = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    booleanValue = appState.booleanValue,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        booleanValue = settingsState.booleanValue,
    )
"""
}
