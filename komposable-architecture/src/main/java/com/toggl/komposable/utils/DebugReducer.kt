package com.toggl.komposable.utils

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.NamedEffect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class DebugReducer<State, Action>(
    private val reducer: Reducer<State, Action>,
    private val printer: DebugPrinter<State, Action> = simplePrinter("DebugReducer"),
    private val logger: (String) -> Unit = { },
) : Reducer<State, Action> {
    override fun reduce(state: State, action: Action): ReduceResult<State, Action> {
        val result = reducer.reduce(state, action)
        val emitPrinter = Effect.fromFlow(
            result.effect.run()
                .onStart { logger("Effect(${result.effect.name()}) started") }
                .onEach { emittedAction -> logger("Effect(${result.effect.name()}) emitted action $emittedAction") }
                .onCompletion { logger("Effect(${result.effect.name()}) completed") },
        ).takeUnless { result.effect is NoEffect }
        val log = printer.print(
            action = action,
            oldState = state,
            newState = result.state,
            effect = result.effect,
        )
        logger(log)
        return result.copy(effect = emitPrinter ?: result.effect)
    }
}

fun<Action> Effect<Action>.name(): String = when (this) {
    is NamedEffect -> name
    else -> this::class.simpleName ?: "UnnamedEffect"
}

fun interface DebugPrinter<State, Action> {
    fun print(action: Action, oldState: State, newState: State, effect: Effect<Action>): String
}

fun <State, Action> simplePrinter(
    name: String,
    actionPrinter: (Action) -> String = { it.toString() },
    statePrinter: (Any?) -> String = { it.toString() },
    selector: (State) -> Any? = { it },
): DebugPrinter<State, Action> = DebugPrinter { action, oldState, newState, effect ->
    """
    |$name
    |Action: ${actionPrinter(action)}
    |Old state: ${statePrinter(selector(oldState))}
    |New state: ${statePrinter(selector(newState))}
    |Effect: ${effect.name()}
    """.trimMargin()
}
