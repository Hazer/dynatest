package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.reporting.ReportEntry
import kotlin.test.expect

/**
 * Tests the very general properties and generic error-handling capabilities of the DynaTestEngine itself. More specialized tests are located
 * at [DynaTestEngineTest].
 */
class GeneralDynaTestEngineTest {
    /**
     * The [TestEngine.discover] block must not fail even if the test discovery itself fails.
     */
    @Test
    fun failingTestSuiteMustNotFailInDiscover() {
        val engine = DynaTestEngine()
        withFail {
            engine.discover2(TestSuiteFailingInInit::class.java)
        }
    }

    private fun DynaTestEngine.discover2(vararg testClasses: Class<*>): TestDescriptor {
        require (testClasses.isNotEmpty())
        return discover(object : EngineDiscoveryRequest {
            override fun getConfigurationParameters(): ConfigurationParameters = EmptyConfigParameters
            override fun <T : DiscoveryFilter<*>?> getFiltersByType(filterType: Class<T>?): MutableList<T> = mutableListOf()
            override fun <T : DiscoverySelector> getSelectorsByType(selectorType: Class<T>): MutableList<T> =
                testClasses.map { it.toSelector() } .filterIsInstance(selectorType) .toMutableList()
        }, UniqueId.forEngine(id))
    }

    /**
     * The [TestEngine.discover] block must not fail even if the test discovery itself fails; instead it must produce an always-failing
     * test descriptor.
     */
    @Test
    fun failingTestSuiteMustFailInExecute() {
        val engine = DynaTestEngine()
        val tests: TestDescriptor = withFail { engine.discover2(TestSuiteFailingInInit::class.java) }
        expect<Class<*>>(InitFailedTestDescriptor::class.java) { tests.children.first().javaClass }
        expectThrows(RuntimeException::class) {
            engine.execute(ExecutionRequest(tests, ThrowingExecutionListener, EmptyConfigParameters))
        }
    }
}

/**
 * An execution listener which immediately throws when an exception occurs. Used together with [runTests] to fail eagerly.
 */
internal object ThrowingExecutionListener : EngineExecutionListener {
    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.throwable.isPresent) throw testExecutionResult.throwable.get()
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry) {}
    override fun executionSkipped(testDescriptor: TestDescriptor, reason: String) {
        throw RuntimeException("Unexpected")
    }
    override fun executionStarted(testDescriptor: TestDescriptor) {}
    override fun dynamicTestRegistered(testDescriptor: TestDescriptor) {
        throw RuntimeException("Unexpected")
    }
}

private fun Class<*>.toSelector(): ClassSelector {
    val c = ClassSelector::class.java.declaredConstructors.first { it.parameterTypes[0] == Class::class.java }
    c.isAccessible = true
    return c.newInstance(this) as ClassSelector
}

private var fail = false
private fun <T> withFail(block: ()->T): T {
    fail = true
    try {
        return block()
    } finally {
        fail = false
    }
}

class TestSuiteFailingInInit : DynaTest({
    if (fail) throw RuntimeException("Simulated")
})
