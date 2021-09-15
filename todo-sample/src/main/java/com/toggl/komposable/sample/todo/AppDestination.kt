package com.toggl.komposable.sample.todo

sealed class AppDestination(val route: String) {
    object List : AppDestination("list")
    object Add : AppDestination("add")
}
