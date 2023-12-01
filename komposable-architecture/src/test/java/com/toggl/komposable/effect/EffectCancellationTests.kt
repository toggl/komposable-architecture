package com.toggl.komposable.effect

import app.cash.turbine.turbineScope
import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.extensions.EffectCancellationException.AllEffectsCancelledManually
import com.toggl.komposable.extensions.EffectCancellationException.EffectsCancelledManually
import com.toggl.komposable.extensions.EffectCancellationException.InFlightEffectsCancelled
import com.toggl.komposable.extensions.cancel
import com.toggl.komposable.extensions.cancelAll
import com.toggl.komposable.extensions.cancellable
import com.toggl.komposable.extensions.merge
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class EffectCancellationTests {
    @Test
    fun `effects can be canceled with cancel effect`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("cancelId01")

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("cancelId02")

        val cancelEffect1 = Effect.cancel("cancelId01")

        turbineScope {
            val effect1Turbine = effect1.run().testIn(backgroundScope)
            val effect2Turbine = effect2.run().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1

            cancelEffect1.run().testIn(backgroundScope)
            effect1Turbine.awaitError().apply {
                shouldBeInstanceOf<EffectsCancelledManually>()
                id shouldBe "cancelId01"
            }

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
        }.cancellable("cancelId03", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("cancelId03", cancelInFlight = false)

        val cancelEffect = Effect.cancel("cancelId03")

        turbineScope {
            val effect1Turbine = effect1.run().testIn(backgroundScope)
            val effect2Turbine = effect2.run().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1

            cancelEffect.run().testIn(backgroundScope)
            effect1Turbine.awaitError().apply {
                shouldBeInstanceOf<EffectsCancelledManually>()
                id shouldBe "cancelId03"
            }
            effect2Turbine.awaitError().apply {
                shouldBeInstanceOf<EffectsCancelledManually>()
                id shouldBe "cancelId03"
            }
        }
    }

    @Test
    fun `merged effect should complete only after all effects are cancelled`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("cancelId04")

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("cancelId05")

        val mergedEffect = effect1.merge(effect2)

        val cancelEffect1 = Effect.cancel("cancelId04")
        val cancelEffect2 = Effect.cancel("cancelId05")

        turbineScope {
            val mergedEffectTurbine = mergedEffect.run().testIn(backgroundScope)

            mergedEffectTurbine.awaitItem() shouldBe 0
            mergedEffectTurbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            mergedEffectTurbine.awaitItem() shouldBe 1

            effect2Flow.value = 2
            mergedEffectTurbine.awaitItem() shouldBe 2

            cancelEffect1.run().testIn(backgroundScope)

            // mergedEffect should not be canceled because effect2Flow is still alive
            effect2Flow.value = 4
            mergedEffectTurbine.awaitItem() shouldBe 4

            cancelEffect2.run().testIn(backgroundScope)

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
        }.cancellable("cancelId06", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("cancelId06", cancelInFlight = false)

        turbineScope {
            val effect1Turbine = effect1.run().testIn(backgroundScope)
            val effect2Turbine = effect2.run().testIn(backgroundScope)

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
        }.cancellable("cancelId07", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("cancelId07", cancelInFlight = true)

        turbineScope {
            val effect1Turbine = effect1.run().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            val effect2Turbine = effect2.run().testIn(backgroundScope)
            effect1Turbine.awaitError().apply {
                shouldBeInstanceOf<InFlightEffectsCancelled>()
                id shouldBe "cancelId07"
            }

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1
        }
    }

    @Test
    fun `all the effects are cancelled when cancelAll is used`() = runTest {
        val effect1Flow = MutableStateFlow(0)
        val effect1 = Effect {
            effect1Flow
        }.cancellable("cancelId08", cancelInFlight = false)

        val effect2Flow = MutableStateFlow(0)
        val effect2 = Effect {
            effect2Flow
        }.cancellable("cancelId08", cancelInFlight = false)

        val effect3Flow = MutableStateFlow(0)
        val effect3 = Effect {
            effect3Flow
        }.cancellable("cancelId09", cancelInFlight = false)

        val cancelAllEffect = Effect.cancelAll()

        turbineScope {
            val effect1Turbine = effect1.run().testIn(backgroundScope)
            val effect2Turbine = effect2.run().testIn(backgroundScope)
            val effect3Turbine = effect3.run().testIn(backgroundScope)

            effect1Turbine.awaitItem() shouldBe 0
            effect1Flow.value = 1
            effect1Turbine.awaitItem() shouldBe 1

            effect2Turbine.awaitItem() shouldBe 0
            effect2Flow.value = 1
            effect2Turbine.awaitItem() shouldBe 1

            effect3Turbine.awaitItem() shouldBe 0
            effect3Flow.value = 1
            effect3Turbine.awaitItem() shouldBe 1

            cancelAllEffect.run().testIn(backgroundScope)

            effect1Turbine.awaitError().shouldBeInstanceOf<AllEffectsCancelledManually>()
            effect2Turbine.awaitError().shouldBeInstanceOf<AllEffectsCancelledManually>()
            effect3Turbine.awaitError().shouldBeInstanceOf<AllEffectsCancelledManually>()
        }
    }
}
