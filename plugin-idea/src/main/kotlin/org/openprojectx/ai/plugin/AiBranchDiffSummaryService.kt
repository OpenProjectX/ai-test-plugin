package org.openprojectx.ai.plugin

import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking

class AiBranchDiffSummaryService(private val project: Project) {

    fun generate(sourceBranch: String, targetBranch: String, diff: String): String {
        val prompt = buildPrompt(sourceBranch, targetBranch, diff)

        return LlmAuthSessionService.getInstance(project).withReloginOnUnauthorized { settings ->
            val provider = LlmProviderFactory.create(settings)
            runBlocking { provider.generateCode(prompt) }.trim()
        }
    }

    private fun buildPrompt(sourceBranch: String, targetBranch: String, diff: String): String {
        return AiPromptDefaults.render(
            AiPromptDefaults.BRANCH_DIFF_SUMMARY,
            mapOf(
                "sourceBranch" to sourceBranch,
                "targetBranch" to targetBranch,
                "diff" to shorten(diff)
            )
        )
    }

    private fun shorten(diff: String, maxChars: Int = 25_000): String {
        return if (diff.length <= maxChars) diff else diff.take(maxChars)
    }
}
