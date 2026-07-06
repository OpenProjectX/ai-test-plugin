package org.openprojectx.ai.plugin

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SonarQubeAiFixCodeContextTest {
    private val source = """
        package demo

        class Sample {
            fun healthy(): String {
                return "ok"
            }

            fun target(input: String?): Int {
                return input!!.length
            }

            fun untouched(): Int {
                return 42
            }
        }
    """.trimIndent()

    @Test
    fun `extracts only the method containing the issue line`() {
        val context = SonarQubeAiFixCodeContext.findTarget(source, issueLine = 8)

        assertNotNull(context)
        assertContains(context.code, "fun target")
        assertContains(context.code, "return input!!.length")
        assertFalse(context.code.contains("fun healthy"))
        assertFalse(context.code.contains("fun untouched"))
    }

    @Test
    fun `includes containing class context while keeping target method separate`() {
        val context = SonarQubeAiFixCodeContext.findTarget(source, issueLine = 8)

        assertNotNull(context)
        assertContains(context.containingClassCode, "class Sample")
        assertContains(context.containingClassCode, "fun healthy")
        assertContains(context.containingClassCode, "fun target")
        assertContains(context.containingClassCode, "fun untouched")
        assertFalse(context.containingClassCode.contains("package demo"))
        assertFalse(context.code.contains("fun healthy"))
    }

    @Test
    fun `merges a fixed target method back into the original file`() {
        val context = SonarQubeAiFixCodeContext.findTarget(source, issueLine = 8)
        assertNotNull(context)

        val fixedMethod = """
            fun target(input: String?): Int {
                return input?.length ?: 0
            }
        """.trimIndent()

        val merged = SonarQubeAiFixCodeContext.mergeFixedTarget(source, context, fixedMethod)

        assertContains(merged, "fun healthy(): String")
        assertContains(merged, "return input?.length ?: 0")
        assertContains(merged, "fun untouched(): Int")
        assertFalse(merged.contains("return input!!.length"))
        assertEquals(source.lines().size, merged.lines().size)
    }
}
