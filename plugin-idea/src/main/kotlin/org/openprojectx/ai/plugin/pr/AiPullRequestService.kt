package org.openprojectx.ai.plugin.pr

import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.openprojectx.ai.plugin.LlmProviderFactory
import org.openprojectx.ai.plugin.LlmSettingsLoader

class AiPullRequestService(private val project: Project) {

    private val json = Json { ignoreUnknownKeys = true }

    fun createAfterPush(
        remoteUrl: String,
        sourceBranch: String,
        targetBranch: String,
        diff: String,
        providerType: GitHostingProviderType,
        providerToken: String
    ): PullRequestResult {
        val llmSettings = LlmSettingsLoader.load(project)
        val llm = LlmProviderFactory.create(llmSettings)

        val prompt = PullRequestPromptBuilder.build(
            diff = shorten(diff),
            sourceBranch = sourceBranch,
            targetBranch = targetBranch
        )

        val raw = runBlocking { llm.generateCode(prompt) }.trim()
        val generated = json.decodeFromString<GeneratedPullRequestContent>(raw)

        val provider = GitHostingProviderFactory.create(
            type = providerType,
            http = org.openprojectx.ai.plugin.HttpClients.shared(),
            token = providerToken
        )

        val repository = when (providerType) {
            GitHostingProviderType.BITBUCKET -> GitRemoteParser.parseBitbucket(remoteUrl)
            GitHostingProviderType.GITHUB -> error("GitHub not implemented yet")
            GitHostingProviderType.GITLAB -> error("GitLab not implemented yet")
        }

        return runBlocking {
            provider.createPullRequest(
                PullRequestRequest(
                    repository = repository,
                    sourceBranch = sourceBranch,
                    targetBranch = targetBranch,
                    title = generated.title.trim(),
                    description = generated.description.trim()
                )
            )
        }
    }

    private fun shorten(diff: String, maxChars: Int = 25_000): String {
        return if (diff.length <= maxChars) diff else diff.take(maxChars)
    }
}