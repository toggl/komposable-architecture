package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.internal.CompositeReducer
import com.toggl.komposable.internal.ForEachReducer
import com.toggl.komposable.internal.OptionalReducer
import com.toggl.komposable.internal.PullbackReducer

/**
 * Allows multiple reducers of the same type to be composed into a single reducer.
 * This is usually used in combination with [pullback] to create the main reducer which is
 * then used to create a store.
 * @see createStore
 * @see pullback
 */
fun <State, Action> combine(vararg reducers: Reducer<State, Action>): Reducer<State, Action> =
    CompositeReducer(reducers.toList())

/**
 * Allows a reducer to handle a subset of actions from another reducer.
 * Use decoration to share action handling behavior across multiple reducers.
 * A good use case for this is entity creation boilerplate for an entity that can be created
 * by multiple different views.
 * @see com.toggl.komposable.architecture.Store.view
 */
fun <LocalState, GlobalState, LocalAction, GlobalAction> Reducer<GlobalState, GlobalAction>.decorateWith(
    reducer: Reducer<LocalState, LocalAction>,
    mapToLocalState: (GlobalState) -> LocalState,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction,
): Reducer<GlobalState, GlobalAction> =
    CompositeReducer(listOf(this, reducer.pullback(mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)))

/**
 * Wraps a reducer to change its action and state type. This allows combining multiple child
 * reducers into a single parent reducer. If the child reducer relies on optional state, use
 * [optionalPullback] instead.
 * @see combine
 * @see optionalPullback
 */
fun <LocalState, GlobalState, LocalAction, GlobalAction>
    Reducer<LocalState, LocalAction>.pullback(
        mapToLocalState: (GlobalState) -> LocalState,
        mapToLocalAction: (GlobalAction) -> LocalAction?,
        mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
        mapToGlobalAction: (LocalAction) -> GlobalAction,
    ): Reducer<GlobalState, GlobalAction> =
    PullbackReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)

/**
 * Wraps a reducer to change its action and state type. This allows combining multiple child
 * reducers into a single parent reducer. If the child reducer does not rely on optional state,
 * use [pullback] instead.
 * @see combine
 * @see pullback
 */
fun <LocalState, GlobalState, LocalAction, GlobalAction>
    Reducer<LocalState, LocalAction>.optionalPullback(
        mapToLocalState: (GlobalState) -> LocalState?,
        mapToLocalAction: (GlobalAction) -> LocalAction?,
        mapToGlobalState: (GlobalState, LocalState?) -> GlobalState,
        mapToGlobalAction: (LocalAction) -> GlobalAction,
    ): Reducer<GlobalState, GlobalAction> =
    OptionalReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)

/**
 * A specialized version of [forEach] for handling a parent state with a [Map] of elements,
 * each associated with a unique key.
 * For a [List] based element collection see [forEachList]. For anything else, see [forEach].
 * @see forEach
 */
fun <ParentState, ElementState, ParentAction, ElementAction, ID>
    Reducer<ParentState, ParentAction>.forEachMap(
        elementReducer: Reducer<ElementState, ElementAction>,
        mapToElementAction: (ParentAction) -> Pair<ID, ElementAction>?,
        mapToElementMap: (ParentState) -> Map<ID, ElementState>,
        mapToParentAction: (ElementAction, ID) -> ParentAction,
        mapToParentState: (ParentState, Map<ID, ElementState>) -> ParentState,
    ): Reducer<ParentState, ParentAction> =
    ForEachReducer(
        parentReducer = this,
        elementReducer = elementReducer,
        mapToElementAction = mapToElementAction,
        mapToElementState = { state, key -> mapToElementMap(state)[key] ?: throw NoSuchElementException("Element with key=$key not found") },
        mapToParentAction = mapToParentAction,
        mapToParentState = { state, elementState, key ->
            val newElementMap = mapToElementMap(state).toMutableMap().apply {
                this[key] = elementState
            }
            mapToParentState(state, newElementMap)
        },
    )

/**
 * A specialized version of [forEach] for handling a parent state with a [List] of elements.
 * For a [Map] based element collection see [forEachMap]. For anything else, see [forEach].
 * @see forEachMap
 * @see forEach
 */
fun <ParentState, ElementState, ParentAction, ElementAction>
        Reducer<ParentState, ParentAction>.forEachList(
    elementReducer: Reducer<ElementState, ElementAction>,
    mapToElementAction: (ParentAction) -> Pair<Int, ElementAction>?,
    mapToElementList: (ParentState) -> List<ElementState>,
    mapToParentAction: (ElementAction, Int) -> ParentAction,
    mapToParentState: (ParentState, List<ElementState>) -> ParentState,
): Reducer<ParentState, ParentAction> =
    ForEachReducer(
        parentReducer = this,
        elementReducer = elementReducer,
        mapToElementAction = mapToElementAction,
        mapToElementState = { state, index -> mapToElementList(state)[index] ?: throw NoSuchElementException("Element with index=$index not found") },
        mapToParentAction = mapToParentAction,
        mapToParentState = { state, elementState, index ->
            val newElementList = mapToElementList(state).toMutableList().apply {
                set(index, elementState)
            }
            mapToParentState(state, newElementList)
        },
    )

/**
 * Embeds a child reducer within a parent domain, allowing it to operate on elements of a collection
 * within the parent's state.
 *
 * For example, if a parent feature manages an array of child states, you can utilize the `forEach`
 * operator to execute both the parent and child's logic:
 *
 * The `forEach` function ensures a specific order of operations, first running the child reducer and
 * then the parent reducer. Reversing this order could lead to subtle bugs, as the parent feature
 * might remove the child state from the array before the child can react to the action.
 */
fun <ParentState, ElementState, ParentAction, ElementAction, ID>
    Reducer<ParentState, ParentAction>.forEach(
        elementReducer: Reducer<ElementState, ElementAction>,
        mapToElementAction: (ParentAction) -> Pair<ID, ElementAction>?,
        mapToElementState: (ParentState, ID) -> ElementState,
        mapToParentAction: (ElementAction, ID) -> ParentAction,
        mapToParentState: (ParentState, ElementState, ID) -> ParentState,
    ): Reducer<ParentState, ParentAction> =
    ForEachReducer(
        parentReducer = this,
        elementReducer = elementReducer,
        mapToElementAction = mapToElementAction,
        mapToElementState = mapToElementState,
        mapToParentAction = mapToParentAction,
        mapToParentState = mapToParentState,
    )
