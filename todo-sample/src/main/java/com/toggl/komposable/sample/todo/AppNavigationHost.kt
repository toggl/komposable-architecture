package com.toggl.komposable.sample.todo

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toggl.komposable.sample.todo.edit.EditPage
import com.toggl.komposable.sample.todo.list.ListPage

@Composable
fun AppNavigationHost(backStack: List<AppDestination>) {
    val navController = rememberNavController()

    // extremely naive implementation for purposes of this sample
    if (navController.currentDestination != null) {
        if (backStack.isNotEmpty()) {
            navController.navigate(backStack.last().route)
        } else {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.List.route
    ) {
        composable(AppDestination.List.route) { ListPage() }
        composable(AppDestination.Add.route) { EditPage() }
    }
}
