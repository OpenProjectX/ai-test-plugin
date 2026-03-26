package org.openprojectx.ai.plugin

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.UIManager

class ContextBoxToolWindowFactory : ToolWindowFactory, DumbAware {

    data class PromptLeaf(
        val name: String,
        val content: String
    ) {
        override fun toString(): String = name
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val stateService = ContextBoxStateService.getInstance(project)

        val commonFont = UIManager.getFont("Label.font")
            ?.deriveFont(Font.PLAIN, 13f)
            ?: Font("SansSerif", Font.PLAIN, 13)
        val bgColor = Color(0x0D, 0x0D, 0x0D)
        val fgColor = Color(0xFF, 0xFF, 0xFF)
        val borderColor = Color(0x22, 0x22, 0x22)

        val resultArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = commonFont
            background = bgColor
            foreground = fgColor
            caretColor = fgColor
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        val promptArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = commonFont
            background = bgColor
            foreground = fgColor
            caretColor = fgColor
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            text = "Select a prompt from the list to view full content."
        }

        val promptCategoryTabs = JTabbedPane().apply {
            background = bgColor
            foreground = fgColor
        }

        fun styledScrollPane(component: java.awt.Component): JBScrollPane =
            JBScrollPane(component).apply {
                viewport.background = bgColor
                background = bgColor
                border = BorderFactory.createLineBorder(borderColor)
            }

        fun buildPromptLeafMap(): LinkedHashMap<String, List<PromptLeaf>> {
            val config = runCatching { LlmSettingsLoader.loadConfig(project) }.getOrNull()
                ?: return linkedMapOf("Error" to listOf(PromptLeaf("Load failed", "Unable to load .ai-test.yaml")))
            val prompts = config.prompts
            return linkedMapOf(
                "Test Generation" to prompts.profiles.generation.items.entries.map { PromptLeaf(it.key, it.value) },
                "Commit Message" to prompts.profiles.commitMessage.items.entries.map { PromptLeaf(it.key, it.value) },
                "Branch Diff" to prompts.profiles.branchDiffSummary.items.entries.map { PromptLeaf(it.key, it.value) },
                "Pull Request" to listOf(PromptLeaf("default", prompts.pullRequest))
            )
        }

        fun refreshPromptTabs() {
            promptCategoryTabs.removeAll()
            val promptMap = buildPromptLeafMap()
            promptMap.forEach { (category, items) ->
                val list = JList(items.toTypedArray()).apply {
                    selectionMode = ListSelectionModel.SINGLE_SELECTION
                    font = commonFont
                    background = bgColor
                    foreground = fgColor
                    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    selectionBackground = Color(0x2A, 0x2A, 0x2A)
                    selectionForeground = fgColor
                }
                list.addListSelectionListener {
                    val selected = list.selectedValue ?: return@addListSelectionListener
                    promptArea.text = selected.content
                    promptArea.caretPosition = 0
                }
                val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
                    leftComponent = styledScrollPane(list)
                    rightComponent = styledScrollPane(promptArea)
                    resizeWeight = 0.35
                    border = BorderFactory.createEmptyBorder()
                    background = bgColor
                }
                promptCategoryTabs.addTab(category, split)
            }
        }

        refreshPromptTabs()

        val resultPanel = JPanel(BorderLayout()).apply {
            add(styledScrollPane(resultArea), BorderLayout.CENTER)
            background = bgColor
            foreground = fgColor
        }

        val promptPanel = JPanel(BorderLayout()).apply {
            add(promptCategoryTabs, BorderLayout.CENTER)
            background = bgColor
            foreground = fgColor
        }

        val rootTabs = JTabbedPane().apply {
            addTab("Latest Result", resultPanel)
            addTab("Prompt Manager", promptPanel)
            background = bgColor
            foreground = fgColor
        }

        fun render(snapshot: ContextBoxStateService.Snapshot) {
            resultArea.text = snapshot.latestResult
            resultArea.caretPosition = 0
            refreshPromptTabs()
        }

        render(stateService.snapshot())

        project.messageBus.connect(toolWindow.disposable).subscribe(
            ContextBoxStateService.TOPIC,
            ContextBoxListener { snapshot ->
                render(snapshot)
            }
        )

        val content = ContentFactory.getInstance().createContent(rootTabs, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
