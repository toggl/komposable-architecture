package com.toggl.komposable.common

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.internal.MutableStateFlowStore
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
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

data class TestState(val testProperty: String = "", val testIntProperty: Int = 0)

sealed class TestAction {
    data class LocalActionWrapper(val action: LocalTestAction) : TestAction()
    data class ChangeTestProperty(val testProperty: String) : TestAction()
    data class AddToTestProperty(val testPropertySuffix: String) : TestAction()
    data class StartEffectAction(val effect: Effect<TestAction>) : TestAction()
    data object ClearTestPropertyFromEffect : TestAction()
    data object DoNothingAction : TestAction()
    data object StartExceptionThrowingEffectAction : TestAction()
    data object DoNothingFromEffectAction : TestAction()
    data object ThrowExceptionAction : TestAction()
}

class TestEffect(private vararg val actions: TestAction = arrayOf(TestAction.DoNothingFromEffectAction)) : Effect<TestAction> {
    override fun run(): Flow<TestAction> = flowOf(*actions)
}

class TestExceptionEffect : Effect<TestAction> {
    override fun run(): Flow<TestAction> = throw TestException
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
            is TestAction.LocalActionWrapper -> ReduceResult(state, NoEffect)
            is TestAction.ChangeTestProperty ->
                ReduceResult(state.copy(testProperty = action.testProperty), NoEffect)
            is TestAction.AddToTestProperty ->
                ReduceResult(state.copy(testProperty = state.testProperty + action.testPropertySuffix), NoEffect)
            TestAction.ClearTestPropertyFromEffect ->
                ReduceResult(state.copy(testProperty = ""), NoEffect)
            is TestAction.StartEffectAction ->
                ReduceResult(state, action.effect)
            TestAction.DoNothingAction -> ReduceResult(state, NoEffect)
            TestAction.DoNothingFromEffectAction -> ReduceResult(state, NoEffect)
            TestAction.ThrowExceptionAction -> throw TestException
            TestAction.StartExceptionThrowingEffectAction -> ReduceResult(state, TestExceptionEffect())
        }
}

object TestException : Exception()

data class LocalTestState(val testIntProperty: Int = 0)
sealed class LocalTestAction {
    data class ChangeTestIntProperty(val testIntProperty: Int) : LocalTestAction()
    data class StartEffectAction(val effect: Effect<LocalTestAction>) : LocalTestAction()
    data object DoNothingLocalAction : LocalTestAction()
    data object DoNothingFromEffectAction : LocalTestAction()
}

class LocalTestReducer : Reducer<LocalTestState, LocalTestAction> {
    override fun reduce(
        state: LocalTestState,
        action: LocalTestAction,
    ): ReduceResult<LocalTestState, LocalTestAction> =
        when (action) {
            is LocalTestAction.ChangeTestIntProperty ->
                ReduceResult(state.copy(testIntProperty = action.testIntProperty), NoEffect)
            is LocalTestAction.StartEffectAction -> ReduceResult(state, action.effect)
            LocalTestAction.DoNothingLocalAction,
            LocalTestAction.DoNothingFromEffectAction,
            -> ReduceResult(state, NoEffect)
        }
}
