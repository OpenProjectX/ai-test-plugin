package org.openprojectx.ai.plugin.pr

object GitRemoteParser {

    fun parseBitbucket(remoteUrl: String): RepositoryRef {
        val ssh = Regex("""git@([^:]+):([^/]+)/(.+?)(\.git)?$""")
        val httpsScm = Regex("""https?://([^/]+)/scm/([^/]+)/(.+?)(\.git)?$""", RegexOption.IGNORE_CASE)

        ssh.matchEntire(remoteUrl)?.let { m ->
            return RepositoryRef(
                provider = GitHostingProviderType.BITBUCKET,
                host = m.groupValues[1],
                projectKey = m.groupValues[2],
                repoSlug = m.groupValues[3]
            )
        }

        httpsScm.matchEntire(remoteUrl)?.let { m ->
            return RepositoryRef(
                provider = GitHostingProviderType.BITBUCKET,
                host = m.groupValues[1],
                projectKey = m.groupValues[2],
                repoSlug = m.groupValues[3]
            )
        }

        error("Unsupported Bitbucket remote URL: $remoteUrl")
    }
}