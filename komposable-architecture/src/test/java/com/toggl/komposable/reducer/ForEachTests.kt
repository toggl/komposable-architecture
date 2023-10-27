package com.toggl.komposable.reducer

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.forEachMap
import com.toggl.komposable.extensions.noEffect
import com.toggl.komposable.test.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ForEachTests {

    private val initialElements = mapOf(
        1 to ElementState(1, "element1"),
        2 to ElementState(2, "element2"),
        3 to ElementState(3, "element3"),
    )
    private val initialParentState = ParentState(
        parentText = "parent",
        lastEditedId = null,
        elementsTextLength = initialElements.values.sumOf { it.elementText.length },
        elements = initialElements,
    )
    private val parentReducer =
        Reducer<ParentState, ParentAction> { state, action ->
            when (action) {
                is ParentAction.EditText ->
                    ReduceResult(state.copy(parentText = action.text), noEffect())
                is ParentAction.ElementActionWrapper ->
                    ReduceResult(
                        state.copy(
                            lastEditedId = action.id,
                            elementsTextLength = state.elements.values.sumOf { it.elementText.length },
                        ),
                        noEffect(),
                    )
            }
        }

    private val elementReducer =
        Reducer<ElementState, ElementAction> { state, action ->
            when (action) {
                is ElementAction.EditText ->
                    ReduceResult(state.copy(elementText = action.text), noEffect())
            }
        }

    private val mergedReducer: Reducer<ParentState, ParentAction> = parentReducer.forEachMap(
        elementReducer = elementReducer,
        mapToElementAction = { action -> (action as? ParentAction.ElementActionWrapper)?.let { it.id to it.elementAction } },
        mapToElementMap = { state -> state.elements },
        mapToParentAction = { elementAction, id -> ParentAction.ElementActionWrapper(id, elementAction) },
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
    fun `parent reducer should handle the action after element reduced, the state should also be already modified by element reducer`() = runTest {
        mergedReducer.testReduceState(
            initialParentState,
            ParentAction.ElementActionWrapper(3, ElementAction.EditText("a")),
        ) { state ->
            state shouldBe initialParentState.copy(
                lastEditedId = 3,
                elementsTextLength = 17,
                elements = initialParentState.elements.toMutableMap().apply {
                    this[3] = ElementState(3, "a")
                },
            )
        }
    }
}

sealed class ParentAction {
    data class EditText(val text: String) : ParentAction()
    data class ElementActionWrapper(val id: Int, val elementAction: ElementAction) : ParentAction()
}

sealed class ElementAction {
    data class EditText(val text: String) : ElementAction()
}

data class ParentState(
    val parentText: String,
    val lastEditedId: Int?,
    val elementsTextLength: Int,
    val elements: Map<Int, ElementState>,
)

data class ElementState(val id: Int, val elementText: String)
