package com.toggl.komposable.internal

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.map
import com.toggl.komposable.extensions.merge

internal class ForEachReducer<ElementState, ParentState, ElementAction, ParentAction, ID>(
    private val parentReducer: Reducer<ParentState, ParentAction>,
    private val elementReducer: Reducer<ElementState, ElementAction>,
    private val mapToElementAction: (ParentAction) -> Pair<ID, ElementAction>?,
    private val mapToElementState: (ParentState, ID) -> ElementState,
    private val mapToParentAction: (ElementAction, ID) -> ParentAction,
    private val mapToParentState: (ParentState, ElementState, ID) -> ParentState,
) : Reducer<ParentState, ParentAction> {
    override fun reduce(
        state: ParentState,
        action: ParentAction,
    ): ReduceResult<ParentState, ParentAction> {
        val (id: ID, elementAction: ElementAction) = mapToElementAction(action) ?: return parentReducer.reduce(state, action)
        val mapToLocalState: (ParentState) -> ElementState = { parentState: ParentState -> mapToElementState(parentState, id) }
        val mapToGlobalState: (ParentState, ElementState) -> ParentState = { parentState: ParentState, elementState: ElementState -> mapToParentState(parentState, elementState!!, id) }

        val (elementState, elementEffect) = elementReducer.reduce(mapToLocalState(state), elementAction)
        val (parentState, parentEffect) = parentReducer.reduce(mapToGlobalState(state, elementState), action)

        return ReduceResult(
            state = parentState,
            effect = elementEffect.map { mapToParentAction(it, id) }.merge(parentEffect),
        )
    }
}
