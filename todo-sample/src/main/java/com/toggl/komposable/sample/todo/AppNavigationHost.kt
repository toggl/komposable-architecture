package com.toggl.komposable.sample.todo

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toggl.komposable.sample.todo.edit.EditPage
import com.toggl.komposable.sample.todo.list.ListPage

@Composable
fun AppNavigationHost(backStack: List<AppDestination>, onBackPressed: (Boolean) -> Unit) {
    val navController = rememberNavController()

    // extremely naive implementation for purposes of this sample
    if (navController.currentDestination != null) {
        if (backStack.size > 1) {
            navController.navigate(backStack.last().route)
        } else {
            navController.popBackStack()
        }
    }

    val activity = LocalContext.current as AppCompatActivity

    activity.handleBackPressesEmitting {
        onBackPressed(backStack.size < 2)
    }

    NavHost(navController = navController, startDestination = AppDestination.List.route) {
        composable(AppDestination.List.route) { ListPage() }
        composable(AppDestination.Edit.route) { EditPage() }
    }
}
