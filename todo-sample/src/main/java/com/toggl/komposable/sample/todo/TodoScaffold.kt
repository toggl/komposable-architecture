package com.toggl.komposable.sample.todo

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.FabPosition
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.toggl.komposable.sample.todo.edit.EditAction
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

    val currentDestination = backStack.last()

    Scaffold(
        floatingActionButton = { if (currentDestination == AppDestination.List) AddTodoFab() },
        floatingActionButtonPosition = FabPosition.Center,
        isFloatingActionButtonDocked = true,
        bottomBar = {
            BottomAppBar(cutoutShape = RoundedCornerShape(100)) {
                if (currentDestination == AppDestination.Add) {
                    OutlinedButton(
                        onClick = { appStore.dispatch(AppAction.Edit(EditAction.SaveTapped)) }
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Text(text = "Save")
                    }
                }
            }
        }
    ) {
        AppNavigationHost(
            backStack = backStack
        )
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
