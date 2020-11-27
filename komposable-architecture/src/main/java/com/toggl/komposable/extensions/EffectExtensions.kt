package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.internal.MapEffect
import com.toggl.komposable.internal.SimpleEffect

/**
 * Transforms and effect by mapping the resulting action into a different type.
 * @param mapFn          The function to transform the returned action.
 * @return An effect whose return action type has been mapped
 */
fun <T, R> Effect<T>.map(mapFn: (T?) -> R?): Effect<R> =
    MapEffect(this, mapFn)

/**
 * Creates a list containing a single effect that when executed returns an action.
 * @param action          The action returned by the single effect.
 * @return A list of effects containing a single effect
 */
fun <Action> effectOf(action: Action): List<Effect<Action>> =
    effectOf(listOf(action))

/**
 * Creates a list of effects that immediately return a single action
 * @param actions          The actions returned by the effects.
 * @return A list of effects
 */
fun <Action> effectOf(actions: List<Action>): List<Effect<Action>> =
    actions.map(::SimpleEffect)

/**
 * Creates a list of effects that immediately return a single action
 * @param actions          The actions returned by the effects.
 * @return A list of effects
 */
fun <Action> effectOf(vararg actions: Action): List<Effect<Action>> =
    actions.toList().map(::SimpleEffect)

/**
 * Creates a list out of a single effect
 * @param effect          The effect to be added to the list.
 * @return A list of effects
 */
fun <Action> effectOf(effect: Effect<Action>): List<Effect<Action>> =
    listOf(effect)

/**
 * Creates a list out of a bunch of effect
 * @param effects          The effects to be added to the list.
 * @return A list of effects
 */
fun <Action> effectOf(vararg effects: Effect<Action>): List<Effect<Action>> =
    effects.toList()

infix operator fun <Action> Effect<Action>.plus(otherEffect: Effect<Action>) = listOf(this, otherEffect)

/**
 * Used to indicate that a reducer does not have any effects other than mutating the state
 * @return An empty list of effects
 */
fun noEffect(): List<Effect<Nothing>> =
    emptyList()
