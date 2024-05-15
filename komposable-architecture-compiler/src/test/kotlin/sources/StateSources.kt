package sources

import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language

object StateSources {

    val settingsState = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        data class SettingsState(val booleanValue: Boolean)
        """.trimIndent(),
    )

    val settingsStateWithMapping = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ParentPath
        
        data class SettingsState(
            @ParentPath("booleanValue")
            val value: Boolean
        )
        """.trimIndent(),
    )

    val settingsStateWithNestedMapping = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ParentPath
        
        data class SettingsState(
            @ParentPath("nested.booleanValue")
            val value: Boolean
        )
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

    val appStateWithNestedValue = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
    
        data class NestedValue(
            val booleanValue: Boolean
        )
        
        @ChildStates(SettingsState::class)
        data class AppState(
            val someList: List<String>,
            val nested: NestedValue
        )
        """.trimIndent(),
    )

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

    @Language("kotlin")
    val generatedStateExtensionsFileWithPathMapping = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    value = appState.booleanValue,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        booleanValue = settingsState.value,
    )
"""

    @Language("kotlin")
    val generatedStateExtensionsFileWithPathNestedMapping = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    value = appState.nested.booleanValue,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        nested = appState.nested.copy(
            booleanValue = settingsState.value,
        )
    )
"""
}