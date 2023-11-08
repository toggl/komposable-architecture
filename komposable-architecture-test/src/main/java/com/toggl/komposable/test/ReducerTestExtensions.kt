package com.toggl.komposable.test

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.Reducer
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

suspend fun <State, Action> Reducer<State, Action>.testReduce(
    initialState: State,
    action: Action,
    testCase: suspend (State, Effect<Action>) -> Unit,
) {
    val (state, effect) = reduce(initialState, action)
    testCase(state, effect)
}

fun <State, Action, EX : Exception> Reducer<State, Action>.testReduceException(
    initialState: State,
    action: Action,
    exception: KClass<EX>,
) {
    assertFailsWith(exception) {
        runTest {
            testReduce(initialState, action) { _, _ -> }
        }
    }
}

suspend fun <State, Action> Reducer<State, Action>.testReduceState(
    initialState: State,
    action: Action,
    testCase: suspend (State) -> Unit,
) = testReduce(initialState, action) { state, _ -> testCase(state) }

suspend fun <State, Action> Reducer<State, Action>.testReduceEffect(
    initialState: State,
    action: Action,
    testCase: suspend (Effect<Action>) -> Unit,
) = testReduce(initialState, action) { _, effect -> testCase(effect) }

suspend fun <State, Action> Reducer<State, Action>.testReduceNoEffect(
    initialState: State,
    action: Action,
) = testReduce(initialState, action, ::assertNoEffectWereReturned)

suspend fun <State, Action> Reducer<State, Action>.testReduceNoOp(
    initialState: State,
    action: Action,
) = testReduce(initialState, action) { state, effect ->
    assertEquals(initialState, state)
    assertEquals(NoEffect, effect)
}

@Suppress("UNUSED_PARAMETER")
fun <State, Action> assertNoEffectWereReturned(state: State, effect: Effect<Action>) {
    assertEquals(NoEffect, effect)
}
