package com.toggl.komposable.sample.todos.title

import com.toggl.komposable.architecture.ParentPath

data class TitleState(
    val title: String,
    @ParentPath("isTitleEdited")
    val isEdited: Boolean,
)
