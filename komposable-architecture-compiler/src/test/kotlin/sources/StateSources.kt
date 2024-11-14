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

    val settingsStateWithDeeperNestedMapping = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ParentPath
        
        data class SettingsState(
            @ParentPath("nestedLevel1.nestedLevel2.booleanValue")
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

    val complexSettingsState = SourceFile.kotlin(
        "SettingsState.kt",
        """
        package com.toggl.komposable.compiler

        import com.toggl.komposable.architecture.ParentPath
        
        data class SettingsState(
            val simpleValue: Boolean,
            @ParentPath("nestedLevel1.value1Level1")
            val settingsValue1Level1: Boolean,
            @ParentPath("nestedLevel1.value2Level1")
            val settingsValue2Level1: Boolean,
            @ParentPath("nestedLevel1.nestedLevel2.value1Level2")
            val settingsValue1Level2: String,
            @ParentPath("nestedLevel1.nestedLevel2.value2Level2")
            val settingsValue2Level2: String,
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

    val appStateWithDeeperNestedValue = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
    
        data class NestedValue2(
            val booleanValue: Boolean
        )

        data class NestedValue1(
            val nestedLevel2: NestedValue2
        )
        
        @ChildStates(SettingsState::class)
        data class AppState(
            val someList: List<String>,
            val nestedLevel1: NestedValue1
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

    val complexAppState = SourceFile.kotlin(
        "AppState.kt",
        """
        package com.toggl.komposable.compiler
        
        import com.toggl.komposable.architecture.ChildStates
        import com.toggl.komposable.compiler.SettingsState
        
        data class NestedValueLevel2(
            val value1Level2: String,
            val value2Level2: String
        )

        data class NestedValueLevel1(
            val value1Level1: Boolean,
            val value2Level1: Boolean,
            val nestedLevel2: NestedValueLevel2
        )

        @ChildStates(SettingsState::class, AuthState::class)
        data class AppState(
            val someList: List<String>,
            val simpleValue: Boolean,
            val nestedLevel1: NestedValueLevel1,
            val email: String
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
    val generatedStateExtensionsFileWithDeeperPathNestedMapping = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    value = appState.nestedLevel1.nestedLevel2.booleanValue,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        nestedLevel1 = appState.nestedLevel1.copy(
            nestedLevel2 = appState.nestedLevel1.nestedLevel2.copy(
                booleanValue = settingsState.value,
            )
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

    @Language("kotlin")
    val generatedStateExtensionsFileForComplexState = """package com.toggl.komposable.compiler

public fun mapAppStateToSettingsState(appState: AppState): SettingsState = SettingsState(
    simpleValue = appState.simpleValue,
    settingsValue1Level1 = appState.nestedLevel1.value1Level1,
    settingsValue2Level1 = appState.nestedLevel1.value2Level1,
    settingsValue1Level2 = appState.nestedLevel1.nestedLevel2.value1Level2,
    settingsValue2Level2 = appState.nestedLevel1.nestedLevel2.value2Level2,
)

public fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState): AppState =
    appState.copy(
        simpleValue = settingsState.simpleValue,
        nestedLevel1 = appState.nestedLevel1.copy(
            value1Level1 = settingsState.settingsValue1Level1,
            value2Level1 = settingsState.settingsValue2Level1,
            nestedLevel2 = appState.nestedLevel1.nestedLevel2.copy(
                value1Level2 = settingsState.settingsValue1Level2,
                value2Level2 = settingsState.settingsValue2Level2,
            )
        )
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
