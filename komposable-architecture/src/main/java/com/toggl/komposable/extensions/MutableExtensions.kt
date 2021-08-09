package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable

/**
 * Convenience method used to both mutate a Mutable and return no effects.
 * @param mutateFn          The function that mutates the Mutable.
 * @return An empty list of effects
 */
fun <T, Action> Mutable<T>.mutateWithoutEffects(mutateFn: T.() -> T): List<Effect<Action>> =
    mutate(mutateFn).withoutEffects()

/**
 * Used to create a fluent api for mutating and returning an effect
 */
infix fun <T, Action> T.returnEffect(effect: Effect<Action>) = effectOf(effect)

/**
 * Used to create a fluent api for mutating and returning an effect
 */
infix fun <T, Action> T.returnEffect(effects: List<Effect<Action>>) = effects

/**
 * Used to create a fluent api for mutating and returning an effect
 */
fun <T> T.withoutEffects() = noEffect()
