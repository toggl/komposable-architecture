package com.toggl.komposable.sample.todo

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.toggl.komposable.sample.todo.data.Identity
import com.toggl.komposable.sample.todo.edit.EditAction
import com.toggl.komposable.sample.todo.list.AddTodoFab
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Composable
fun TodoScaffold() {
    val appStore = hiltViewModel<AppStoreViewModel>()
    val viewState by appStore.collectViewStateWhenStarted()
    val backStack = viewState.backStack
    val activity = LocalContext.current as AppCompatActivity

    activity.handleBackPressesEmitting {
        if (backStack.size < 2) activity.finish()
        else appStore.send(AppAction.BackPressed)
    }

    val currentDestination = backStack.last()

    Scaffold(
        floatingActionButton = { if (currentDestination == AppDestination.List) AddTodoFab() },
        floatingActionButtonPosition = FabPosition.Center,
        isFloatingActionButtonDocked = true,
        bottomBar = { TodoBottomAppBar(appStore, currentDestination, viewState.identity) }
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

@Composable
private fun TodoBottomAppBar(appStore: AppStoreViewModel, currentDestination: AppDestination, identity: Identity) {
    BottomAppBar(cutoutShape = RoundedCornerShape(100)) {
        if (currentDestination == AppDestination.Add) {
            OutlinedButton(
                onClick = { appStore.send(AppAction.Edit(EditAction.SaveTapped)) }
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Text(text = "Save")
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        val identityText = when (identity) {
            Identity.Unknown -> "Loading..."
            is Identity.User -> identity.username
        }
        Text(text = identityText, modifier = Modifier.padding(end = 12.dp))
    }
}
