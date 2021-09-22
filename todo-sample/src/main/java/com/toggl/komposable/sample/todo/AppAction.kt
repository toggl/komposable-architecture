package com.toggl.komposable.sample.todo

import com.toggl.komposable.sample.todo.data.Identity
import com.toggl.komposable.sample.todo.edit.EditAction
import com.toggl.komposable.sample.todo.list.ListAction

sealed class AppAction {
    class List(override val action: ListAction) : AppAction(), ActionWrapper<ListAction>
    class Edit(override val action: EditAction) : AppAction(), ActionWrapper<EditAction>
    data class IdentityUpdated(val newIdentity: Identity) : AppAction()
    object BackPressed : AppAction()
}

interface ActionWrapper<WrappedAction> {
    val action: WrappedAction
}

inline fun <From, reified To> From.unwrap(): To? =
    if (this !is ActionWrapper<*> || this.action !is To) null
    else this.action as To
