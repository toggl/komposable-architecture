package com.toggl.komposable.sample.todo

sealed class AppDestination(val route: String) {
    object Edit : AppDestination("edit")
    object List : AppDestination("list")
}
