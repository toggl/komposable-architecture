package com.toggl.komposable.sample.todo.edit.domain

sealed class EditAction {
    data class TitleChanged(val title: String) : EditAction()
    data class DescriptionChanged(val description: String) : EditAction()
    object CloseTapped : EditAction()
    object SaveTapped : EditAction()
}
