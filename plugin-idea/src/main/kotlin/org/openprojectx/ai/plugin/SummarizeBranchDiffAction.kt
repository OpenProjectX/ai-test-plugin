package org.openprojectx.ai.plugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.vcs.log.VcsLogDataKeys

class SummarizeBranchDiffAction : AnAction(
    null,
    "Summarize Branch Differences",
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val branches = resolveComparedBranches(e)
        val changedFiles = resolveChangedFiles(e)

        if (branches == null && changedFiles.isEmpty()) {
            Notifications.warn(
                project,
                "Summarize Branch Diff",
                "Cannot detect diff context. Open a branch/file diff and try again."
            )
            return
        }

        val sourceBranch = branches?.first ?: "working-tree"
        val targetBranch = branches?.second ?: "HEAD"

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Summarizing Branch Diff", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Collecting branch diff for $sourceBranch vs $targetBranch..."
                    val diff = when {
                        changedFiles.isNotEmpty() -> GitDiffProvider.getDiffForFiles(project, changedFiles)
                        else -> GitDiffProvider.getDiffBetweenBranches(project, sourceBranch, targetBranch)
                    }

                    if (diff.isBlank()) {
                        Notifications.info(
                            project,
                            "Summarize Branch Diff",
                            "No changes found between $sourceBranch and $targetBranch."
                        )
                        return
                    }

                    indicator.text = "Generating summary..."
                    val summary = AiBranchDiffSummaryService(project).generate(
                        sourceBranch = sourceBranch,
                        targetBranch = targetBranch,
                        diff = buildSummaryInput(diff, changedFiles)
                    )

                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(project, summary.trim(), "Branch Diff Summary")
                    }
                } catch (ex: Exception) {
                    Notifications.error(
                        project,
                        "Summarize Branch Diff failed",
                        ex.message ?: ex.toString()
                    )
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    private fun resolveComparedBranches(e: AnActionEvent): Pair<String, String>? {
        val rawBranches = e.getData(VcsLogDataKeys.VCS_LOG_BRANCHES) as? Collection<*>
            ?: return null

        val branches = LinkedHashSet<String>()
        for (rawBranch in rawBranches) {
            val name = extractBranchName(rawBranch)?.trim().orEmpty()
            if (name.isNotEmpty()) {
                branches.add(name)
            }
        }

        if (branches.size != 2) return null

        val values = branches.toList()
        return Pair(values[0], values[1])
    }

    private fun extractBranchName(value: Any?): String? {
        return when (value) {
            null -> null
            is String -> value
            else -> runCatching {
                value.javaClass.getMethod("getName").invoke(value) as? String
            }.getOrNull() ?: value.toString()
        }
    }

    private fun resolveChangedFiles(e: AnActionEvent): List<String> {
        val files = linkedSetOf<String>()
        e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.mapTo(files) { it.path }

        e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?.path
            ?.let { files.add(it) }

        return files.toList()
    }

    private fun buildSummaryInput(diff: String, changedFiles: List<String>): String {
        if (changedFiles.isEmpty()) return diff
        val filesSection = changedFiles.joinToString(separator = "\n") { "- $it" }
        return "Changed files:\n$filesSection\n\nDiff:\n$diff"
    }

}
