package com.toggl.komposable.test.store

/**
 * Controls the level of exhaustiveness for state change and action receipt assertions in [TestStore].
 */
internal sealed class TestExhaustivity {
    /**
     * Represents an exhaustive level where all state changes and received actions must be asserted and
     * all effects must be done when finishing the test.
     */
    data object Exhaustive : TestExhaustivity()

    /**
     * Represents a non-exhaustive level where state changes and received actions can be skipped.
     * Note that actions fired by effects must still be received or skipped before new actions can be sent.
     * The final state of the store can be asserted using [TestStore.assert] after calling [TestStore.finish]
     */
    data class NonExhaustive(
        val logIgnoredReceivedActions: Boolean = true,
        val logIgnoredStateChanges: Boolean = true,
        val logIgnoredEffects: Boolean = true,
    ) : TestExhaustivity()
}
