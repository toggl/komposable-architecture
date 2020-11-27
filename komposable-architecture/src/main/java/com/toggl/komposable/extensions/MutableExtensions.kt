package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable

fun <T, Action> Mutable<T>.mutateWithoutEffects(mutateFn: T.() -> T): List<Effect<Action>> =
    mutate(mutateFn).withoutEffects()

infix fun <T, Action> T.returnEffect(effect: Effect<Action>) = effectOf(effect)

infix fun <T, Action> T.returnEffect(effects: List<Effect<Action>>) = effects

fun <T> T.withoutEffects() = noEffect()
