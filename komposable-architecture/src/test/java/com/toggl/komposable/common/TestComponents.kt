package com.toggl.komposable.common

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.extensions.effectOf
import com.toggl.komposable.extensions.noEffect
import com.toggl.komposable.internal.MutableStateFlowStore
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.lang.Exception

fun CoroutineTest.createTestStore(
    initialState: TestState = TestState(),
    reducer: Reducer<TestState, TestAction> = TestReducer(),
    subscription: Subscription<TestState, TestAction> = TestSubscription(),
    defaultExceptionHandler: ExceptionHandler = TestStoreExceptionHandler(),
    dispatcherProvider: DispatcherProvider = this.dispatcherProvider,
    storeScopeProvider: StoreScopeProvider = StoreScopeProvider { this.testCoroutineScope }
) = MutableStateFlowStore.create(
    initialState,
    reducer,
    subscription,
    defaultExceptionHandler,
    storeScopeProvider,
    dispatcherProvider
)

data class TestState(val testProperty: String = "")

sealed class TestAction {
    data class ChangeTestProperty(val testProperty: String) : TestAction()
    data class AddToTestProperty(val testPropertySuffix: String) : TestAction()
    object DoNothingAction : TestAction()
    object StartEffectAction : TestAction()
    object StartExceptionThrowingEffectAction : TestAction()
    object DoNothingFromEffectAction : TestAction()
    object ThrowExceptionAction : TestAction()
}

class TestEffect : Effect<TestAction> {
    override suspend fun execute(): TestAction = TestAction.DoNothingFromEffectAction
}

class TestExceptionEffect : Effect<TestAction> {
    override suspend fun execute(): TestAction = throw TestException
}

class TestSubscription : Subscription<TestState, TestAction> {
    val stateFlow = MutableStateFlow<TestAction?>(null)
    override fun subscribe(state: Flow<TestState>): Flow<TestAction> =
        stateFlow.asStateFlow().filterNotNull()
}

class TestStoreExceptionHandler : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean {
        return false
    }
}

class TestReducer : Reducer<TestState, TestAction> {
    override fun reduce(state: Mutable<TestState>, action: TestAction): List<Effect<TestAction>> =
        when (action) {
            is TestAction.ChangeTestProperty -> {
                state.mutate { copy(testProperty = action.testProperty) }
                noEffect()
            }
            is TestAction.AddToTestProperty -> {
                state.mutate { copy(testProperty = testProperty + action.testPropertySuffix) }
                noEffect()
            }
            TestAction.DoNothingAction -> noEffect()
            TestAction.StartEffectAction -> effectOf(TestEffect())
            TestAction.DoNothingFromEffectAction -> noEffect()
            TestAction.ThrowExceptionAction -> throw TestException
            TestAction.StartExceptionThrowingEffectAction -> effectOf(TestExceptionEffect())
        }
}

object TestException : Exception()
