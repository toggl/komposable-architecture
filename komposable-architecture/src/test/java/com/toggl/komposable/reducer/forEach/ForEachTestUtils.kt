package com.toggl.komposable.reducer.forEach

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer

sealed class ParentAction {
    data class EditText(val text: String) : ParentAction()
    data class ElementActionWrapper(val id: Int, val elementAction: ElementAction) : ParentAction()
}

sealed class ElementAction {
    data class EditText(val text: String) : ElementAction()
}

data class ElementState(val elementText: String)

internal val elementReducer =
    Reducer<ElementState, ElementAction> { state, action ->
        when (action) {
            is ElementAction.EditText ->
                ReduceResult(state.copy(elementText = action.text), NoEffect)
        }
    }

internal val initialElements = mapOf(
    0 to ElementState("element1"),
    1 to ElementState("element2"),
    2 to ElementState("element3"),
)
