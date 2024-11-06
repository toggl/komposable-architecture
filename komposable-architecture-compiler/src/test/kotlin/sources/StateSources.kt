package sources

import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language

object StateSources {

    val authState = SourceFile.kotlin(
        "AuthState.kt",
        """
        package com.toggl.komposable.compiler
        
        data class AuthState(val email: String)
        """.trimIndent(),
    )

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

    val settingsStateWithInvalidMapping = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ParentPath
        
        data class SettingsState(
            @ParentPath("invalidPath")
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

    val settingsStateWithTwoNestedMapping = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ParentPath
        
        data class SettingsState(
            @ParentPath("nested.booleanValue1")
            val value1: Boolean,
            @ParentPath("nested.booleanValue2")
            val value2: Boolean
        )
        """.trimIndent(),
    )

    val settingsStateWithTwoValues = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        data class SettingsState(
            val booleanValue1: Boolean,
            val booleanValue2: Boolean
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

    val appStateMultipleChildStates = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
        
        @ChildStates(SettingsState::class, AuthState::class)
        data class AppState(
            val someList: List<String>,
            val booleanValue: Boolean,
            val email: String
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

    val appStateWithNestedValueInSeparateFile = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
    
        @ChildStates(SettingsState::class)
        data class AppState(
            val someList: List<String>,
            val nested: NestedValue
        )
        """.trimIndent(),
    )

    val standaloneNestedValue = SourceFile.kotlin(
        "NestedValue.kt",
        """
        package com.toggl.komposable.compiler
        
        data class NestedValue(
            val booleanValue: Boolean
        )
        """.trimIndent(),
    )

    val appStateWithTwoNestedValues = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
    
        data class NestedValue(
            val booleanValue1: Boolean,
            val booleanValue2: Boolean
        )
        
        @ChildStates(SettingsState::class)
        data class AppState(
            val someList: List<String>,
            val nested: NestedValue
        )
        """.trimIndent(),
    )

    val appStateWithTwoValues = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
        
        @ChildStates(SettingsState::class)
        data class AppState(
            val someList: List<String>,
            val booleanValue1: Boolean,
            val booleanValue2: Boolean
        )
        """.trimIndent(),
    )

    val appStateNonDataClass = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
        
        @ChildStates(SettingsState::class)
        class AppState(
            val someList: List<String>,
            val booleanValue: Boolean
        )
        """.trimIndent(),
    )

    val appStateWithNestedNonDataClassValue = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
    
        class NestedValue(
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

    @Language("kotlin")
    val generatedStateExtensionsFileWithTwoValues = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    booleanValue1 = appState.booleanValue1,
    booleanValue2 = appState.booleanValue2,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        booleanValue1 = settingsState.booleanValue1,
        booleanValue2 = settingsState.booleanValue2,
    )
"""

    @Language("kotlin")
    val generatedStateExtensionsFileWithTwoPathNestedMapping = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    value1 = appState.nested.booleanValue1,
    value2 = appState.nested.booleanValue2,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        nested = appState.nested.copy(
            booleanValue1 = settingsState.value1,
            booleanValue2 = settingsState.value2,
        )
    )
"""

    @Language("kotlin")
    val generatedStateExtensionsFileForMultipleChildStates = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    booleanValue = appState.booleanValue,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        booleanValue = settingsState.booleanValue,
    )

public fun mapAppStateToAuthState(appState: AppState): AuthState = AuthState(
    email = appState.email,
)

public fun mapAuthStateToAppState(appState: AppState, authState: AuthState): AppState =
    appState.copy(
        email = authState.email,
    )
"""
}
