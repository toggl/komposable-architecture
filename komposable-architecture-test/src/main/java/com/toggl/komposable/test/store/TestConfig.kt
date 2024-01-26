package com.toggl.komposable.test.store

import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.test.utils.Logger
import com.toggl.komposable.test.utils.NoopLogger
import kotlinx.coroutines.test.TestScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @property dispatcherProvider The [DispatcherProvider] to be used by the test store.
 * @property testCoroutineScope The [TestScope] to be used by the test store.
 * @property logger The [Logger] to be used by the test store to log errors and warnings
 * issued detected during tests.
 * @property timeout The [Duration] to be used by the test store to wait for effects to finish
 * running before failing the test.
 * @property reflectionHandler The [ReflectionHandler] to be used by the test store to filter
 * and iterate over properties of a class, in order to assert state changes property-by-property.
 * @property exhaustivity The [TestExhaustivity] to be used by the test store to determine
 * the level of exhaustiveness for state change and action receipt assertions.
 */
abstract class TestConfig {
    abstract val dispatcherProvider: DispatcherProvider
    abstract val testCoroutineScope: TestScope
    abstract val timeout: Duration
    abstract val logger: Logger
    abstract val reflectionHandler: ReflectionHandler
    internal abstract val exhaustivity: TestExhaustivity
}

data class ExhaustiveTestConfig(
    override val dispatcherProvider: DispatcherProvider,
    override val testCoroutineScope: TestScope,
    override val logger: Logger = NoopLogger(),
    override val timeout: Duration = 100.milliseconds,
    override val reflectionHandler: ReflectionHandler = PublicPropertiesReflectionHandler(),
) : TestConfig() {
    override val exhaustivity: TestExhaustivity = TestExhaustivity.Exhaustive
}

/**
 * @property logIgnoredReceivedActions Whether to log ignored received actions.
 * @property logIgnoredStateChanges Whether to log ignored state changes.
 * @property logIgnoredEffects Whether to log ignored effects.
 */
data class NonExhaustiveTestConfig(
    override val dispatcherProvider: DispatcherProvider,
    override val testCoroutineScope: TestScope,
    override val logger: Logger = NoopLogger(),
    override val timeout: Duration = 100.milliseconds,
    override val reflectionHandler: ReflectionHandler = PublicPropertiesReflectionHandler(),
    val logIgnoredReceivedActions: Boolean = true,
    val logIgnoredStateChanges: Boolean = true,
    val logIgnoredEffects: Boolean = true,
) : TestConfig() {
    override val exhaustivity: TestExhaustivity
        get() = TestExhaustivity.NonExhaustive(
            logIgnoredReceivedActions = logIgnoredReceivedActions,
            logIgnoredStateChanges = logIgnoredStateChanges,
            logIgnoredEffects = logIgnoredEffects,
        )
}
