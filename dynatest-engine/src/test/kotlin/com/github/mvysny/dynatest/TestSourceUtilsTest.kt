package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import com.github.mvysny.dynatest.engine.isRunningInsideGradle
import com.github.mvysny.dynatest.engine.toTestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FileSource
import kotlin.test.expect

class TestSourceUtilsTest: DynaTest({

    group("tests for StackTraceElement.toTestSource()") {
        test("this class resolves to FileSource") {
            val e = DynaNodeGroupImpl.computeTestSource()!!
            if (isRunningInsideGradle) {
                val src = e.toTestSource() as ClassSource
                expect(TestSourceUtilsTest::class.java.name) { src.className }
                expect(e.lineNumber) { src.position.get().line }
            } else {
                val src = e.toTestSource() as FileSource
                expect(
                    true,
                    src.file.absolutePath
                ) { src.file.absolutePath.endsWith("src/test/kotlin/com/github/mvysny/dynatest/TestSourceUtilsTest.kt") }
                expect(e.lineNumber) { src.position.get().line }
            }
        }

        test("InternalTestingClass resolves to FileSource") {
            val e = internalTestingClassGetTestSourceOfThis()
            if (isRunningInsideGradle) {
                val src = e.toTestSource() as ClassSource
                expect(InternalTestingClass::class.java.name) { src.className }
                expect(e.lineNumber) { src.position.get().line }
            } else {
                val src = e.toTestSource() as FileSource
                expect(
                    true,
                    src.file.absolutePath
                ) { src.file.absolutePath.endsWith("src/main/kotlin/com/github/mvysny/dynatest/InternalTestingClass.kt") }
                expect(12) { src.position.get().line }
            }
        }

        // a preparation test for gradleFreezingTest(). Generally Gradle freezes if it sees a mixture of FileSource and ClassSource.
        // see the gradleFreezingTest() for more info.
        test("InternalTestingClass") {
            expect(true, InternalTestingClass.getTestSourceOfThis().className) {
                InternalTestingClass.getTestSourceOfThis().className.startsWith(InternalTestingClass::class.java.name)
            }
        }
    }

    // A nasty test. This test will make Gradle freeze after last test.
    // A Test for https://github.com/gradle/gradle/issues/5737
    InternalTestingClass.gradleFreezingTest(this)
})
