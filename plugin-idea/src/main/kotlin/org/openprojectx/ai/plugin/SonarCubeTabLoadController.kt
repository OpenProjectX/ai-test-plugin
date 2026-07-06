package org.openprojectx.ai.plugin

internal class SonarCubeTabLoadController(
    private val sonarTabTitle: String
) {
    private var loaded = false

    fun shouldLoadForSelection(selectedTitle: String?): Boolean {
        if (loaded || selectedTitle != sonarTabTitle) return false
        loaded = true
        return true
    }
}
