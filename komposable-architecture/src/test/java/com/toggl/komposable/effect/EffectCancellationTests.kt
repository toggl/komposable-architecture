package com.toggl.komposable.effect

import app.cash.turbine.turbineScope
import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.extensions.cancel
import com.toggl.komposable.extensions.cancellable
import com.toggl.komposable.extensions.merge
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

class EffectCancellationTests {
    @Test
    fun `effects can be canceled with cancel effect`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("effect1")

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("effect2")

        val cancelEffect1 = Effect.cancel("effect1")

        turbineScope {
            val effect1Turbine = effect1.actions().testIn(backgroundScope)
            val effect2Turbine = effect2.actions().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1

            cancelEffect1.actions().testIn(backgroundScope)
            effect1Turbine.awaitError().shouldBeInstanceOf<CancellationException>()
            // effect2 should not be canceled
            effect2Flow.value = 2
            effect2Turbine.awaitItem() shouldBe 2
        }
    }

    @Test
    fun `multiple effects with the same id should all be canceled at the same time`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("effect", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("effect", cancelInFlight = false)

        val cancelEffect = Effect.cancel("effect")

        turbineScope {
            val effect1Turbine = effect1.actions().testIn(backgroundScope)
            val effect2Turbine = effect2.actions().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1

            cancelEffect.actions().testIn(backgroundScope)
            effect1Turbine.awaitError().shouldBeInstanceOf<CancellationException>()
            effect2Turbine.awaitError().shouldBeInstanceOf<CancellationException>()
        }
    }

    @Test
    fun `merged effect should complete only after all effects are cancelled`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("effect1")

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("effect2")

        val mergedEffect = effect1.merge(effect2)

        val cancelEffect1 = Effect.cancel("effect1")
        val cancelEffect2 = Effect.cancel("effect2")

        turbineScope {
            val mergedEffectTurbine = mergedEffect.actions().testIn(backgroundScope)

            mergedEffectTurbine.awaitItem() shouldBe 0
            mergedEffectTurbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            mergedEffectTurbine.awaitItem() shouldBe 1

            effect2Flow.value = 2
            mergedEffectTurbine.awaitItem() shouldBe 2

            cancelEffect1.actions().testIn(backgroundScope)

            // mergedEffect should not be canceled because effect2Flow is still alive
            effect2Flow.value = 4
            mergedEffectTurbine.awaitItem() shouldBe 4

            cancelEffect2.actions().testIn(backgroundScope)

            // now it should be dead too
            effect2Flow.value = 5
            mergedEffectTurbine.awaitComplete()
        }
    }

    @Test
    fun `multiple effects with the same id should work in parallel when cancelInFlight = false`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("effect", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("effect", cancelInFlight = false)

        turbineScope {
            val effect1Turbine = effect1.actions().testIn(backgroundScope)
            val effect2Turbine = effect2.actions().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1
        }
    }

    @Test
    fun `multiple effects with the same id should cancel each other out when cancelInFlight = true`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("effect", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("effect", cancelInFlight = true)

        turbineScope {
            val effect1Turbine = effect1.actions().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            val effect2Turbine = effect2.actions().testIn(backgroundScope)
            effect1Turbine.awaitError().shouldBeInstanceOf<CancellationException>()

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1
        }
    }
}
