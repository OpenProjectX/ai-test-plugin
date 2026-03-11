package org.openprojectx.ai.plugin.pr

import io.ktor.client.HttpClient

object GitHostingProviderFactory {

    fun create(
        type: GitHostingProviderType,
        http: HttpClient,
        token: String
    ): GitHostingProvider {
        return when (type) {
            GitHostingProviderType.BITBUCKET -> BitbucketPullRequestProvider(http, token)
            GitHostingProviderType.GITHUB -> error("GitHub PR provider is not implemented yet")
            GitHostingProviderType.GITLAB -> error("GitLab PR provider is not implemented yet")
        }
    }
}