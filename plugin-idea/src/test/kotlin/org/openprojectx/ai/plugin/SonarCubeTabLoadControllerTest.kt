package org.openprojectx.ai.plugin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SonarCubeTabLoadControllerTest {
    @Test
    fun `loads SonarQube panel only when Sonar tab is selected`() {
        val controller = SonarCubeTabLoadController("Sonar Cube")

        assertFalse(controller.shouldLoadForSelection("Guide"))
        assertFalse(controller.shouldLoadForSelection("Context"))

        assertTrue(controller.shouldLoadForSelection("Sonar Cube"))
        assertFalse(controller.shouldLoadForSelection("Sonar Cube"))
    }
}
