package cz.lukynka.astral

import java.util.*
import kotlin.concurrent.timerTask
import kotlin.time.Duration.Companion.milliseconds

object TestHandlers {

    fun handleNormal(step: AstralTest.NormalStep, index: Int) {
        try {
            step.unit.invoke()
            step.finish()
        } catch (exception: Exception) {
            step.fail("${exception::class.simpleName} was thrown", exception = exception)
        }
    }

    fun handleCleanup(step: AstralTest.CleanupStep, index: Int) {
        try {
            step.unit.invoke()
            step.finish()
        } catch (exception: Exception) {
            step.fail("${exception::class.simpleName} was thrown", exception = exception)
        }
    }

    fun handleAssertThrowsStep(step: AstralTest.AssertThrowsStep, index: Int) {
        try {
            if (step.unit.invoke()) {
                step.fail("Unit did not throw")
            }
        } catch (exception: Exception) {
            step.finish()
        }
    }

    fun handleAssertStep(step: AstralTest.AssertStep, index: Int) {
        try {
            if (step.unit.invoke()) {
                step.finish()
            } else {
                step.fail("Assertion failed")
            }
        } catch (exception: Exception) {
            step.fail("${exception::class.simpleName} was thrown", exception = exception)
        }
    }

    fun handleWait(step: AstralTest.WaitUntilStep, index: Int) {
        val timer = Timer("astral_wait_test => ${step.name}")
        val timeout = step.timeout.inWholeMilliseconds

        var ms = 0
        val task = timerTask {
            ms++
            if (ms >= timeout) {
                step.fail("Timed out", ms.milliseconds)
                this.cancel()
                timer.cancel()
            }
            try {
                if (step.unit.invoke()) {
                    println(" ⏳ [Step #$index] \"${step.name}\" Took ${ms}ms")
                    step.finish()
                    this.cancel()
                    timer.cancel()
                }
            } catch (exception: Exception) {
                step.fail("${exception::class.simpleName} was thrown", exception = exception, waited = ms.milliseconds)
            }
        }
        timer.scheduleAtFixedRate(task, 0, 1)
    }
}