package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.map
import com.toggl.komposable.extensions.noEffect

internal class ForEachReducer<ElementState, ParentState, ElementAction, ParentAction, ID>(
    private val parentReducer: Reducer<ParentState, ParentAction>,
    private val elementReducer: Reducer<ElementState, ElementAction>,
    private val mapToElementAction: (ParentAction) -> Pair<ID, ElementAction>?,
    private val mapToElementState: (ParentState, ID) -> ElementState,
    private val mapToParentAction: (ElementAction, ID) -> ParentAction,
    private val mapToParentState: (ParentState, ElementState, ID) -> ParentState,
) : Reducer<ParentState, ParentAction> {
    override fun reduce(
        state: Mutable<ParentState>,
        action: ParentAction,
    ): List<Effect<ParentAction>> {
        val (id: ID, elementAction: ElementAction) = mapToElementAction(action) ?: return parentReducer.reduce(state, action)
        val mapToLocalState: (ParentState) -> ElementState = { parentState: ParentState -> mapToElementState(parentState, id) }
        val mapToGlobalState: (ParentState, ElementState) -> ParentState = { parentState: ParentState, elementState: ElementState -> mapToParentState(parentState, elementState!!, id) }

        val originalState = state()
        var elementState = mapToLocalState(originalState)
        val elementMutableValue = Mutable({ elementState }) { elementState = it }
        val elementEffects = elementReducer
            .reduce(elementMutableValue, elementAction)
            .map { effect -> effect.map { action -> action?.run { mapToParentAction(this, id) } } }

        var newParentState = mapToGlobalState(originalState, elementState)
        val parentMutableValue = Mutable({ newParentState }) { newParentState = it }
        val parentEffect = parentReducer.reduce(parentMutableValue, action)

        state.mutate { newParentState }

        return elementEffects + parentEffect
    }
}