package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.internal.CompositeSubscription

/**
 * @param subscriptions      List of subscriptions which should be merged.
 * @return A [CompositeSubscription] of a given subscriptions.
 * @see CompositeSubscription
 */
fun <State, Action : Any> mergeSubscriptions(vararg subscriptions: Subscription<State, Action>): Subscription<State, Action> =
    CompositeSubscription(subscriptions.toList())

/**
 * @param subscriptions      List of subscriptions which should be merged.
 * @return A [CompositeSubscription] of a given subscriptions.
 * @see CompositeSubscription
 */
fun <State, Action : Any> mergeSubscriptions(subscriptions: List<Subscription<State, Action>>): Subscription<State, Action> =
    CompositeSubscription(subscriptions)

/**
 * @receiver First subscription.
 * @param subscription      Second subscription that will be merged with the receiver.
 * @return A [CompositeSubscription] of a given subscriptions.
 * @see CompositeSubscription
 */
infix fun <State, Action : Any> Subscription<State, Action>.mergeWith(subscription: Subscription<State, Action>): Subscription<State, Action> =
    mergeSubscriptions(this, subscription)
