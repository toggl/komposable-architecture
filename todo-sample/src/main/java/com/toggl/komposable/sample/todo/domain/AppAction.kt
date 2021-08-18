package com.toggl.komposable.sample.todo.domain

import com.toggl.komposable.sample.todo.edit.domain.EditAction
import com.toggl.komposable.sample.todo.list.domain.ListAction

sealed class AppAction {
    class List(override val action: ListAction) : AppAction(), ActionWrapper<ListAction>
    class Edit(override val action: EditAction) : AppAction(), ActionWrapper<EditAction>
}

interface ActionWrapper<WrappedAction> {
    val action: WrappedAction
}

inline fun <From, reified To> From.unwrap(): To? =
    if (this !is ActionWrapper<*> || this.action !is To) null
    else this.action as To
