package com.toggl.komposable.reducer.forEach

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.forEachList
import com.toggl.komposable.test.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

data class ParentListState(
    val parentText: String,
    val lastEditedIndex: Int?,
    val elementsTextLength: Int,
    val elements: List<ElementState>,
)

class ForEachListTests {
    private val initialParentState =
        ParentListState(
            parentText = "parent",
            lastEditedIndex = null,
            elementsTextLength = initialElements.values.sumOf { it.elementText.length },
            elements = initialElements.values.toList(),
        )
    private val parentReducer =
        Reducer<ParentListState, ParentAction> { state, action ->
            when (action) {
                is ParentAction.EditText ->
                    ReduceResult(state.copy(parentText = action.text), NoEffect)
                is ParentAction.ElementActionWrapper ->
                    ReduceResult(
                        state.copy(
                            lastEditedIndex = action.id,
                            elementsTextLength = state.elements.sumOf { it.elementText.length },
                        ),
                        NoEffect,
                    )
            }
        }

    private val mergedReducer: Reducer<ParentListState, ParentAction> =
        parentReducer.forEachList(
            elementReducer = elementReducer,
            mapToElementAction = { action -> (action as? ParentAction.ElementActionWrapper)?.let { it.id to it.elementAction } },
            mapToElementList = { state -> state.elements },
            mapToParentAction = { elementAction, id ->
                ParentAction.ElementActionWrapper(
                    id,
                    elementAction,
                )
            },
            mapToParentState = { state, elementList -> state.copy(elements = elementList) },
        )

    @Test
    fun `parent actions should work normally`() =
        runTest {
            mergedReducer.testReduceState(
                initialParentState,
                ParentAction.EditText("updated"),
            ) { state ->
                state shouldBe
                    initialParentState.copy(
                        parentText = "updated",
                    )
            }
        }

    @Test
    fun `parent reducer should reduce on top of already reduced state by element reducer`() =
        runTest {
            mergedReducer.testReduceState(
                initialParentState,
                ParentAction.ElementActionWrapper(2, ElementAction.EditText("a")),
            ) { state ->
                state shouldBe
                    initialParentState.copy(
                        lastEditedIndex = 2,
                        elementsTextLength = 17, // "element1".length + "element2".length + "a".length
                        elements =
                        initialParentState.elements.toMutableList().apply {
                            this[2] = ElementState("a")
                        },
                    )
            }
        }
}
