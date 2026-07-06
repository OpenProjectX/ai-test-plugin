package org.openprojectx.ai.plugin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextBoxTabVisibilityTest {
    @Test
    fun `hides skill manager tab by default`() {
        assertFalse(ContextBoxTabVisibility.showSkillManager(AiTestSettingsModel()))
    }

    @Test
    fun `shows skill manager tab when advanced setting is enabled`() {
        assertTrue(ContextBoxTabVisibility.showSkillManager(AiTestSettingsModel(showSkillManagerTab = true)))
    }
}
