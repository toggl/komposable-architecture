package com.toggl.komposable.common

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.ReduceResult
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

fun StoreCoroutineTest.createTestStore(
    initialState: TestState = TestState(),
    reducer: Reducer<TestState, TestAction> = TestReducer(),
    subscription: Subscription<TestState, TestAction> = TestSubscription(),
    defaultExceptionHandler: ExceptionHandler = TestStoreExceptionHandler(),
    dispatcherProvider: DispatcherProvider = this.dispatcherProvider,
    storeScopeProvider: StoreScopeProvider = StoreScopeProvider { this.testCoroutineScope },
) = MutableStateFlowStore.create(
    initialState,
    reducer,
    subscription,
    defaultExceptionHandler,
    storeScopeProvider,
    dispatcherProvider,
)

data class TestState(val testProperty: String = "")

sealed class TestAction {
    data class ChangeTestProperty(val testProperty: String) : TestAction()
    data class AddToTestProperty(val testPropertySuffix: String) : TestAction()
    data class StartEffectAction(val effect: Effect<TestAction>) : TestAction()
    object ClearTestPropertyFromEffect : TestAction()
    object DoNothingAction : TestAction()
    object StartExceptionThrowingEffectAction : TestAction()
    object DoNothingFromEffectAction : TestAction()
    object ThrowExceptionAction : TestAction()
}

class TestEffect(private val action: TestAction = TestAction.DoNothingFromEffectAction) : Effect<TestAction> {
    override suspend fun execute(): TestAction = action
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
    override fun reduce(state: TestState, action: TestAction): ReduceResult<TestState, TestAction> =
        when (action) {
            is TestAction.ChangeTestProperty ->
                ReduceResult(state.copy(testProperty = action.testProperty), noEffect())
            is TestAction.AddToTestProperty ->
                ReduceResult(state.copy(testProperty = state.testProperty + action.testPropertySuffix), noEffect())
            TestAction.ClearTestPropertyFromEffect ->
                ReduceResult(state.copy(testProperty = ""), noEffect())
            is TestAction.StartEffectAction ->
                ReduceResult(state, effectOf(action.effect))
            TestAction.DoNothingAction -> ReduceResult(state, noEffect())
            TestAction.DoNothingFromEffectAction -> ReduceResult(state, noEffect())
            TestAction.ThrowExceptionAction -> throw TestException
            TestAction.StartExceptionThrowingEffectAction -> ReduceResult(state, effectOf(TestExceptionEffect()))
        }
}

object TestException : Exception()
