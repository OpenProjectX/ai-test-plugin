package org.openprojectx.ai.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.vcs.log.VcsLogDataKeys

class SummarizeBranchDiffAction : AnAction("Summarize Branch Diff") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val branches = resolveComparedBranches(e)

        if (branches == null) {
            Notifications.warn(
                project,
                "Summarize Branch Diff",
                "This action is only available while comparing two branches."
            )
            return
        }

        val (sourceBranch, targetBranch) = branches

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Summarizing Branch Diff", false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Collecting branch diff for $sourceBranch vs $targetBranch..."
                    val diff = GitDiffProvider.getDiffBetweenBranches(project, sourceBranch, targetBranch)

                    if (diff.isBlank()) {
                        Notifications.info(
                            project,
                            "Summarize Branch Diff",
                            "No changes found between $sourceBranch and $targetBranch."
                        )
                        return
                    }

                    indicator.text = "Generating summary..."
                    val summary = AiBranchDiffSummaryService(project).generate(sourceBranch, targetBranch, diff)

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
        e.presentation.isEnabledAndVisible = e.project != null && resolveComparedBranches(e) != null
    }

    private fun resolveComparedBranches(e: AnActionEvent): Pair<String, String>? {
        val branches = e.getData(VcsLogDataKeys.VCS_LOG_BRANCHES)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: return null

        if (branches.size != 2) return null

        return branches[0] to branches[1]
    }
}
