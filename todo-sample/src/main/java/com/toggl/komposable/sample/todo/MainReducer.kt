package com.toggl.komposable.sample.todo

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer

class MainReducer : Reducer<Any, Any> {
    override fun reduce(state: Mutable<Any>, action: Any): List<Effect<Any>> {
        TODO("Not yet implemented")
    }
}
