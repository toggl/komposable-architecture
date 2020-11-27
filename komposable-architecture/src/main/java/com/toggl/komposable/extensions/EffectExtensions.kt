package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.internal.MapEffect
import com.toggl.komposable.internal.SimpleEffect

fun <T, R> Effect<T>.map(mapFn: (T?) -> R?): Effect<R> =
    MapEffect(this, mapFn)

fun <Action> effectOf(action: Action): List<Effect<Action>> =
    effectOf(listOf(action))

fun <Action> effectOf(actions: List<Action>): List<Effect<Action>> =
    actions.map(::SimpleEffect)

fun <Action> effectOf(vararg actions: Action): List<Effect<Action>> =
    actions.toList().map(::SimpleEffect)

fun <Action> effectOf(effect: Effect<Action>): List<Effect<Action>> =
    listOf(effect)

fun <Action> effectOf(vararg effects: Effect<Action>): List<Effect<Action>> =
    effects.toList()

infix operator fun <Action> Effect<Action>.plus(otherEffect: Effect<Action>) = listOf(this, otherEffect)

fun noEffect(): List<Effect<Nothing>> =
    emptyList()
