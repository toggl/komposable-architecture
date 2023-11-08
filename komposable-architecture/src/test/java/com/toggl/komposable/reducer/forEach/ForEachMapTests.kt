package com.toggl.komposable.reducer.forEach

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.forEachMap
import com.toggl.komposable.extensions.withoutEffect
import com.toggl.komposable.test.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

data class ParentMapState(
    val parentText: String,
    val lastEditedKey: Int?,
    val elementsTextLength: Int,
    val elements: Map<Int, ElementState>,
)

class ForEachMapTests {

    private val initialParentState = ParentMapState(
        parentText = "parent",
        lastEditedKey = null,
        elementsTextLength = initialElements.values.sumOf { it.elementText.length },
        elements = initialElements,
    )

    private val parentReducer =
        Reducer<ParentMapState, ParentAction> { state, action ->
            when (action) {
                is ParentAction.EditText ->
                    state.copy(parentText = action.text).withoutEffect()
                is ParentAction.ElementActionWrapper ->
                    state.copy(
                        lastEditedKey = action.id,
                        elementsTextLength = state.elements.values.sumOf { it.elementText.length },
                    ).withoutEffect()
            }
        }

    private val mergedReducer: Reducer<ParentMapState, ParentAction> = parentReducer.forEachMap(
        elementReducer = elementReducer,
        mapToElementAction = { action -> (action as? ParentAction.ElementActionWrapper)?.let { it.id to it.elementAction } },
        mapToElementMap = { state -> state.elements },
        mapToParentAction = { elementAction, id ->
            ParentAction.ElementActionWrapper(
                id,
                elementAction,
            )
        },
        mapToParentState = { state, elementMap -> state.copy(elements = elementMap) },
    )

    @Test
    fun `parent actions should work normally`() = runTest {
        mergedReducer.testReduceState(
            initialParentState,
            ParentAction.EditText("updated"),
        ) { state ->
            state shouldBe initialParentState.copy(
                parentText = "updated",
            )
        }
    }

    @Test
    fun `parent reducer should reduce on top of already reduced state by element reducer`() = runTest {
        mergedReducer.testReduceState(
            initialParentState,
            ParentAction.ElementActionWrapper(2, ElementAction.EditText("a")),
        ) { state ->
            state shouldBe initialParentState.copy(
                lastEditedKey = 2,
                elementsTextLength = 17, // "element1".length + "element2".length + "a".length
                elements = initialParentState.elements.toMutableMap().apply {
                    this[2] = ElementState("a")
                },
            )
        }
    }
}
