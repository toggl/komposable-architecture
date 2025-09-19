package com.toggl.komposable.sample.petsnavigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

interface TopLevelRoute {
    val label: String
    val icon: ImageVector
    val clickAction: PetsAction
}

@Serializable
data object AnimalListRoute : NavKey, TopLevelRoute {
    override val label: String = "Pets"
    override val icon: ImageVector = Icons.Filled.Pets
    override val clickAction: PetsAction = PetsAction.AnimalsTabClicked
}

@Serializable
data class AnimalDetailRoute(val id: Int) : NavKey

@Serializable
data object SettingsRoute : NavKey, TopLevelRoute {
    override val label: String = "Settings"
    override val icon: ImageVector = Icons.Filled.Settings
    override val clickAction: PetsAction = PetsAction.SettingsTabClicked
}

@Serializable
data object AboutRoute : NavKey
