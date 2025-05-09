package cz.lukynka.astral

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.timerTask
import kotlin.time.Duration

abstract class AstralTest {

    private val steps: MutableList<Step> = mutableListOf()

    abstract fun setup()

    abstract fun cleanup()

    abstract fun createTestSteps()

    private val latch = CountDownLatch(1)
    private var hasFail = false

    @Test
    fun runTests() {
        setup()
        println(" ")
        println(" \uD83D\uDD36 Test: ${this::class.simpleName!!} ")
        println(" ")

        createTestSteps()
        val first = steps.firstOrNull() ?: throw IllegalArgumentException("No steps in test runner")
        handleStep(first)
        cleanup()
        if (hasFail) {
            Assertions.fail<AstralTest>("Failed")
        }
    }

    private fun handleStep(step: Step) {
        val index = steps.indexOf(step)
        val timer = Timer("astral_test_runner => ${this::class.simpleName}")

        val icon = if (step::class.simpleName!!.lowercase().toString().contains("assert")) "\uD83D\uDD39" else "\uD83D\uDD38"
        println(" $icon [Step #$index] ${step.name}")

        when (step) {
            is NormalStep -> TestHandlers.handleNormal(step, index)
            is WaitUntilStep -> TestHandlers.handleWait(step, index)
            is AssertStep -> TestHandlers.handleAssertStep(step, index)
            is AssertThrowsStep -> TestHandlers.handleAssertThrowsStep(step, index)
            is CleanupStep -> TestHandlers.handleCleanup(step, index)
        }

        val task = timerTask {
            if (step.finished) {
                this.cancel()
                timer.cancel()
                if (step.failReason != null) {
                    val reason = step.failReason!!
                    val message = FailReason.getReasonMessage(reason, index)
                    hasFail = true
                    if (step.failReason!!.exception != null) {
                        steps.clear()
                    }
                    println(message)
                }
                val next = steps.getOrNull(index + 1)
                if (next != null) {
                    handleStep(next)
                } else {
                    latch.countDown()
                }
            }
        }

        timer.scheduleAtFixedRate(task, 0, 1)
        latch.await()
    }

    protected fun addStep(name: String, unit: () -> Unit) {
        steps.add(NormalStep(name, unit))
    }

    protected fun addWaitUntil(name: String, unit: () -> Boolean) {
        steps.add(WaitUntilStep(name, unit, Duration.INFINITE))
    }

    protected fun addWaitUntil(name: String, timeout: Duration, unit: () -> Boolean) {
        steps.add(WaitUntilStep(name, unit, timeout))
    }

    protected fun addAssert(name: String, unit: () -> Boolean) {
        steps.add(AssertStep(name, unit))
    }

    protected fun addAssertThrows(name: String, unit: () -> Boolean) {
        steps.add(AssertThrowsStep(name, unit))
    }

    protected fun addCleanup(unit: () -> Unit) {
        steps.add(CleanupStep(unit))
    }

    abstract class Step(val name: String) {
        var finished = false
        var failReason: FailReason? = null
        var trace = Thread.currentThread().stackTrace

        fun finish() {
            finished = true
        }

        open fun fail(reason: String, exception: Exception? = null) {
            failReason = NormalFailReason(this, reason, exception)
            finish()
        }

        open fun fail(reason: String, waited: Duration, exception: Exception? = null) {
            if (this is WaitUntilStep) {
                failReason = WaitFailReason(this, reason, waited, exception)
                finish()
            } else {
                fail(reason)
            }
        }
    }

    abstract class FailReason(val step: Step, val reason: String, val exception: Exception? = null) {

        companion object {
            fun getReasonMessage(failReason: FailReason, index: Int): String {
                return buildString {
                    appendLine("")
                    appendLine(" \uD83D\uDCA5 [Step #${index}] Failed")
                    if (failReason is WaitFailReason) append(" (Waited ${failReason.waited.inWholeMilliseconds}ms)")
                    appendLine("   - Step: ${failReason.step.name} (${failReason.step::class.simpleName!!})")
                    appendLine("   - Reason: ${failReason.reason}")
                    appendLine("   - At: ${failReason.getLocation()}")

                    if (failReason.exception != null) {
                        appendLine("   - Exception: ")
                        appendLine(failReason.exception.stackTraceToString())
                    }

                    appendLine("")
                }
            }
        }

        var trace = step.trace.firstOrNull { element ->
            !element.className.toString().startsWith("cz.lukynka.astral") &&
                    !element.className.toString().startsWith("java.util.TimerThread") &&
                    !element.className.toString().startsWith("java.lang.Thread")
        }

        fun getLocation(): String {
            val location = if (trace != null) {
                ".(${trace!!.fileName}:${trace!!.lineNumber})"
            } else {
                "Unknown location (could not determine call site from stack trace)"
            }
            return location
        }
    }

    class NormalFailReason(step: Step, reason: String, exception: Exception? = null) : FailReason(step, reason, exception)
    class WaitFailReason(step: WaitUntilStep, reason: String, val waited: Duration, exception: Exception?) : FailReason(step, reason, exception)

    class NormalStep(name: String, val unit: () -> Unit) : Step(name)
    class WaitUntilStep(name: String, val unit: () -> Boolean, val timeout: Duration) : Step(name)
    class AssertStep(name: String, val unit: () -> Boolean) : Step(name)
    class AssertThrowsStep(name: String, val unit: () -> Boolean) : Step(name)
    class CleanupStep(val unit: () -> Unit) : Step("Cleanup")
}