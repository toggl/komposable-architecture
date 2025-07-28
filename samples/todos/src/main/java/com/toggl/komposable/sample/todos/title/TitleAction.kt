package com.toggl.komposable.sample.todos.title

sealed class TitleAction {
    data class TitleChanged(
        val text: String,
    ) : TitleAction()

    data object StartEditing : TitleAction()

    data object StopEditing : TitleAction()
}
