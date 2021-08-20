package com.toggl.komposable.sample.todo

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.toggl.komposable.extensions.dispatch
import com.toggl.komposable.sample.todo.list.AddTodoFab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
@Composable
fun TodoScaffold() {
    val appStore = hiltViewModel<AppStoreViewModel>()
    val backStack by appStore.state
        .map { it.backStack }
        .collectAsStateWhenStarted(initial = listOf(AppDestination.List))

    val activity = LocalContext.current as AppCompatActivity

    activity.handleBackPressesEmitting {
        if (backStack.size < 2) activity.finish()
        else appStore.dispatch(AppAction.BackPressed)
    }

    Scaffold(
        floatingActionButton = { if (backStack.last() == AppDestination.List) AddTodoFab() },
        floatingActionButtonPosition = FabPosition.Center,
        isFloatingActionButtonDocked = true,
        bottomBar = { TodoBottomAppBar() }
    ) {
        AppNavigationHost(
            backStack = backStack
        )
    }
}

@Composable
private fun TodoBottomAppBar() {
    BottomAppBar(
        cutoutShape = RoundedCornerShape(100)
    ) {
    }
}

fun AppCompatActivity.handleBackPressesEmitting(callback: () -> Unit) {
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            callback()
        }
    }

    lifecycle.addObserver(
        object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                onBackPressedDispatcher.addCallback(backPressedCallback)
            }

            override fun onPause(owner: LifecycleOwner) {
                backPressedCallback.remove()
            }
        }
    )
}

data class BackStackUpdate(val change: Int, val backStack: BackStack)
