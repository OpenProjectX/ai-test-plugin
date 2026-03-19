package org.openprojectx.ai.plugin

import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import java.io.File

object GitDiffProvider {

    fun getDiff(project: Project): String {
        val basePath = project.basePath ?: error("Project base path is unavailable")
        val repo = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
            ?: error("No Git repository found for project")

        val process = ProcessBuilder(
            "git",
            "diff",
            "--cached",
            "--",
            "."
        )
            .directory(File(repo.root.path))
            .redirectErrorStream(true)
            .start()

        val staged = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()

        if (staged.isNotBlank()) return staged

        val workingTreeProcess = ProcessBuilder(
            "git",
            "diff",
            "--",
            "."
        )
            .directory(File(repo.root.path))
            .redirectErrorStream(true)
            .start()

        val unstaged = workingTreeProcess.inputStream.bufferedReader().use { it.readText() }
        workingTreeProcess.waitFor()

        return unstaged
    }


    fun getDiffBetweenBranches(project: Project, sourceBranch: String, targetBranch: String): String {
        val repo = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
            ?: error("No Git repository found for project")

        val process = ProcessBuilder(
            "git",
            "diff",
            "$targetBranch...$sourceBranch"
        )
            .directory(File(repo.root.path))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            error("Failed to compare branches: $output")
        }

        return output
    }

    fun getDiffForFiles(project: Project, filePaths: List<String>): String {
        val repo = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
            ?: error("No Git repository found for project")
        if (filePaths.isEmpty()) return ""

        val process = ProcessBuilder(
            listOf("git", "diff", "--") + filePaths
        )
            .directory(File(repo.root.path))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            error("Failed to collect file diff: $output")
        }

        return output
    }

}
