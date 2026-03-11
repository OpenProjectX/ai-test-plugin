package org.openprojectx.ai.plugin.pr

import kotlinx.serialization.Serializable

object PullRequestPromptBuilder {

    fun build(diff: String, sourceBranch: String, targetBranch: String): String {
        return """
            You are an expert software engineer creating a pull request.

            Generate:
            1. A concise PR title
            2. A clear PR description

            Requirements:
            - Output valid JSON only
            - JSON shape:
              {
                "title": "...",
                "description": "..."
              }
            - Title should be short and actionable
            - Description should explain:
              - what changed
              - why it changed
              - important implementation notes
            - Use markdown in description
            - Do not include code fences

            Source branch: $sourceBranch
            Target branch: $targetBranch

            Git diff:
            $diff
        """.trimIndent()
    }
}

@Serializable
data class GeneratedPullRequestContent(
    val title: String,
    val description: String
)