package com.toggl.komposable.reducer.pullback

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.common.LocalTestReducer
import com.toggl.komposable.common.LocalTestState
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestState
import com.toggl.komposable.extensions.pullback
import io.mockk.spyk

class PullbackTests : BasePullbackTests() {
    override val localReducer = spyk(LocalTestReducer())
    override val pulledBackReducer: Reducer<TestState, TestAction> = localReducer.pullback(
        mapToLocalState = { LocalTestState(it.testIntProperty) },
        mapToLocalAction = { if (it is TestAction.LocalActionWrapper) it.action else null },
        mapToGlobalState = { globalState, localState -> globalState.copy(testIntProperty = localState.testIntProperty) },
        mapToGlobalAction = { TestAction.LocalActionWrapper(it) },
    )
}
