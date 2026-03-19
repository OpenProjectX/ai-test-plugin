package org.openprojectx.ai.plugin

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DumbAwareAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.vcs.log.VcsLogDataKeys

open class SummarizeBranchDiffAction(
    tooltip: String = "Summarize Branch Differences (Default)",
    iconPath: String = "/icons/git-probe-01.svg",
    private val sourceTag: String = "default"
) : DumbAwareAction(
    null,
    tooltip,
    IconLoader.getIcon(iconPath, SummarizeBranchDiffAction::class.java)
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val branches = resolveComparedBranches(e)
        val changedFiles = resolveChangedFiles(e)

        if (branches == null && changedFiles.isEmpty()) {
            Notifications.warn(
                project,
                "Summarize Branch Diff",
                "[$sourceTag] Cannot detect diff context. Open a branch/file diff and try again."
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
                            "[$sourceTag] No changes found between $sourceBranch and $targetBranch."
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
                        "Summarize Branch Diff failed [$sourceTag]",
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

class SummarizeProbeVcsLogToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Vcs.Log.Toolbar]",
    iconPath = "/icons/git-probe-01.svg",
    sourceTag = "Vcs.Log.Toolbar"
)

class SummarizeProbeVcsLogInternalToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Vcs.Log.Toolbar.Internal]",
    iconPath = "/icons/git-probe-02.svg",
    sourceTag = "Vcs.Log.Toolbar.Internal"
)

class SummarizeProbeDiffViewerToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Diff.ViewerToolbar]",
    iconPath = "/icons/git-probe-03.svg",
    sourceTag = "Diff.ViewerToolbar"
)

class SummarizeProbeVcsDiffToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Vcs.Diff.Toolbar]",
    iconPath = "/icons/git-probe-04.svg",
    sourceTag = "Vcs.Diff.Toolbar"
)

class SummarizeProbeChangesViewToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe ChangesViewToolbar]",
    iconPath = "/icons/git-probe-05.svg",
    sourceTag = "ChangesViewToolbar"
)

class SummarizeProbeDiffToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Diff.Toolbar]",
    iconPath = "/icons/git-probe-06.svg",
    sourceTag = "Diff.Toolbar"
)

class SummarizeProbeDiffEditorToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Diff.EditorToolbar]",
    iconPath = "/icons/git-probe-07.svg",
    sourceTag = "Diff.EditorToolbar"
)

class SummarizeProbeFileHistoryToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Vcs.FileHistory.Toolbar]",
    iconPath = "/icons/git-probe-08.svg",
    sourceTag = "Vcs.FileHistory.Toolbar"
)

class SummarizeProbeGitBranchesToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Git.Branches.ToolBar]",
    iconPath = "/icons/git-probe-09.svg",
    sourceTag = "Git.Branches.ToolBar"
)

class SummarizeProbeCommitMessageToolbarAction : SummarizeBranchDiffAction(
    tooltip = "Summarize Branch Differences [Probe Vcs.MessageActionGroup]",
    iconPath = "/icons/git-probe-10.svg",
    sourceTag = "Vcs.MessageActionGroup"
)
