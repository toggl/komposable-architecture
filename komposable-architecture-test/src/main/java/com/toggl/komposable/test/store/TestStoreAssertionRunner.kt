package com.toggl.komposable.test.store

import com.toggl.komposable.test.utils.info
import io.kotest.matchers.shouldBe
import kotlin.reflect.full.memberProperties

internal fun <State : Any, Action> TestExhaustivity.createAssertionRunner(
    testStore: TestStore<State, Action>,
): TestStore.AssertionRunner<State, Action> =
    if (this is TestExhaustivity.NonExhaustive) {
        NonExhaustiveAssertionRunner(
            testStore,
            this.logIgnoredStateChanges,
            this.logIgnoredReceivedActions,
            this.logIgnoredEffects,
        )
    } else {
        ExhaustiveAssertionRunner(testStore)
    }

internal class ExhaustiveAssertionRunner<State : Any, Action>(
    store: TestStore<State, Action>,
) : TestStore.AssertionRunner<State, Action> {
    private val reducer = store.reducer
    private val timeout = store.timeout
    override fun assertStateChange(
        previousState: State,
        currentState: State,
        assert: ((state: State) -> State)?,
    ) {
        if (assert != null) {
            currentState shouldBe assert(previousState)
        } else {
            currentState shouldBe previousState
        }
    }

    override fun hasReceivedActionToHandle(match: (Action) -> Boolean): Boolean =
        reducer.receivedActions.isNotEmpty()

    override fun skipNotMatchingActions(match: (Action) -> Boolean) {
        // no-op: exhaustive assertion runner doesn't skip ignored received actions
    }

    override fun assertEffectsAreDone() {
        if (reducer.inFlightEffects.isNotEmpty()) {
            val error = StringBuilder().apply {
                appendLine("🚨")
                appendLine("There are still ${reducer.inFlightEffects.size} effects in flight that didn't finish under the ${timeout}ms timeout:")
                reducer.inFlightEffects.forEach {
                    appendLine("-$it")
                }
            }
            throw AssertionError(error.toString())
        }
    }

    override suspend fun assertActionsWereReceived() {
        if (reducer.receivedActions.isNotEmpty()) {
            val error = StringBuilder().apply {
                appendLine("🚨")
                appendLine("There are still ${reducer.receivedActions.size} actions in the queue:")
                reducer.receivedActions.forEach {
                    appendLine("-$it")
                }
            }
            throw AssertionError(error.toString())
        }
    }
}

internal class NonExhaustiveAssertionRunner<State : Any, Action>(
    private val store: TestStore<State, Action>,
    private val logIgnoredStateChanges: Boolean,
    private val logIgnoredReceivedActions: Boolean,
    private val logIgnoredEffects: Boolean,
) : TestStore.AssertionRunner<State, Action> {
    private val reducer = store.reducer
    private val logger = store.logger
    private val reflectionHandler = store.reflectionHandler
    private val timeout = store.timeout

    override fun assertStateChange(
        previousState: State,
        currentState: State,
        assert: ((state: State) -> State)?,
    ) {
        if (assert == null) {
            if (logIgnoredStateChanges) {
                logger.info("No assertion was provided, skipping state assertion (state did ${if (previousState == currentState) "not " else ""}change)")
            }
        } else {
            val fields = previousState::class.memberProperties.toMutableSet()
            val previousStateModified = assert(previousState)
            val currentStateModified = assert(currentState)
            val assertedFieldChanges = reflectionHandler.filterAccessibleProperty(fields) {
                it.getter.call(previousState) != it.getter.call(previousStateModified) ||
                    it.getter.call(currentState) != it.getter.call(currentStateModified)
            }
            assertedFieldChanges.forEach {
                it.getter.call(currentState) shouldBe it.getter.call(previousStateModified)
            }
            if (logIgnoredStateChanges) {
                var changesHappened = false
                val log = StringBuilder().apply {
                    appendLine("⚠️")
                    appendLine("The following state changes were not asserted:")
                }
                reflectionHandler.forEachAccessibleProperty(fields) {
                    if (it.getter.call(previousStateModified) != it.getter.call(currentState)) {
                        changesHappened = true
                        log.appendLine(".${it.name}")
                        log.appendLine("-Before: ${it.getter.call(previousStateModified)}")
                        log.appendLine("-After: ${it.getter.call(currentState)}")
                    }
                }
                if (changesHappened) {
                    logger.info(log.toString())
                }
            }
        }
    }

    override fun hasReceivedActionToHandle(match: (Action) -> Boolean): Boolean =
        reducer.receivedActions.any { match(it.first) }

    override fun skipNotMatchingActions(match: (Action) -> Boolean) {
        if (reducer.receivedActions.none { match(it.first) }) {
            throw AssertionError("No action matching the predicate was found")
        }

        val skippedActions = mutableListOf<Action>()
        var foundAction = true
        try {
            while (!match(reducer.receivedActions.first().first)) {
                val (action, reducedState) = reducer.receivedActions.removeFirst()
                skippedActions.add(action)
                reducer.state = reducedState
            }
        } catch (ex: NoSuchElementException) {
            foundAction = false
        }

        if (logIgnoredReceivedActions) {
            if (skippedActions.isNotEmpty()) {
                val log = StringBuilder().apply {
                    appendLine("⚠️")
                    appendLine("The following received actions were skipped:")
                    skippedActions.forEach {
                        appendLine("-$it")
                    }
                    appendLine("Total: ${skippedActions.size}")
                }
                logger.info(log.toString())
            }
        }

        if (!foundAction) {
            throw AssertionError("No action matching the predicate was found")
        }
    }

    override fun assertEffectsAreDone() {
        if (reducer.inFlightEffects.isNotEmpty()) {
            val warning = StringBuilder().apply {
                appendLine("⚠️")
                appendLine("There are still ${reducer.inFlightEffects.size} effects in flight that didn't finish under the ${timeout}ms timeout:")
                reducer.inFlightEffects.forEach {
                    appendLine("-$it")
                }
            }
            if (logIgnoredEffects) {
                logger.info(warning.toString())
            }
        }
    }

    override suspend fun assertActionsWereReceived() {
        val hasReceivedActions = reducer.receivedActions.isNotEmpty()
        if (hasReceivedActions && logIgnoredReceivedActions) {
            val warning = StringBuilder().apply {
                appendLine("⚠️")
                appendLine("There are still ${reducer.receivedActions.size} actions in the queue:")
                reducer.receivedActions.forEach {
                    appendLine("-$it")
                }
            }
            logger.info(warning.toString())
        }
        store.skipReceivedActions(reducer.receivedActions.size)
    }
}
