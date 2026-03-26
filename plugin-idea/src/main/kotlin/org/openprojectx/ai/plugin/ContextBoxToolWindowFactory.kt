package org.openprojectx.ai.plugin

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.UIManager
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

class ContextBoxToolWindowFactory : ToolWindowFactory, DumbAware {

    data class PromptLeaf(
        val name: String,
        val content: String
    ) {
        override fun toString(): String = name
    }

    enum class PromptCategory {
        GENERATION,
        COMMIT,
        BRANCH_DIFF,
        PULL_REQUEST
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

        fun parseYamlMap(text: String): Map<String, String> {
            val parsed = Yaml().load<Any?>(text) as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val result = linkedMapOf<String, String>()
            parsed.forEach { (k, v) ->
                val key = k?.toString()?.trim().orEmpty()
                if (key.isNotBlank()) {
                    result[key] = v?.toString().orEmpty()
                }
            }
            return result
        }

        fun dumpYamlMap(value: Map<String, String>): String {
            val options = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                indent = 2
                isPrettyFlow = true
            }
            return Yaml(options).dump(value).trimEnd()
        }

        fun updateYamlProfile(yaml: String, name: String, value: String): String {
            val map = parseYamlMap(yaml).toMutableMap()
            map[name] = value
            return dumpYamlMap(map)
        }

        fun removeYamlProfile(yaml: String, name: String): String {
            val map = parseYamlMap(yaml).toMutableMap()
            map.remove(name)
            if (map.isEmpty()) {
                map[PromptProfileSet.DEFAULT_NAME] = ""
            }
            return dumpYamlMap(map)
        }

        fun savePromptValue(category: PromptCategory, name: String, value: String) {
            val model = LlmSettingsLoader.loadSettingsModel(project)
            val updated = when (category) {
                PromptCategory.GENERATION -> model.copy(
                    generationPromptProfilesYaml = updateYamlProfile(model.generationPromptProfilesYaml, name, value)
                )
                PromptCategory.COMMIT -> model.copy(
                    commitPromptProfilesYaml = updateYamlProfile(model.commitPromptProfilesYaml, name, value)
                )
                PromptCategory.BRANCH_DIFF -> model.copy(
                    branchDiffPromptProfilesYaml = updateYamlProfile(model.branchDiffPromptProfilesYaml, name, value)
                )
                PromptCategory.PULL_REQUEST -> model.copy(pullRequestPrompt = value)
            }
            LlmSettingsLoader.saveSettingsModel(project, updated)
        }

        fun addPrompt(category: PromptCategory, name: String) {
            val model = LlmSettingsLoader.loadSettingsModel(project)
            val updated = when (category) {
                PromptCategory.GENERATION -> model.copy(
                    generationPromptProfilesYaml = updateYamlProfile(model.generationPromptProfilesYaml, name, "")
                )
                PromptCategory.COMMIT -> model.copy(
                    commitPromptProfilesYaml = updateYamlProfile(model.commitPromptProfilesYaml, name, "")
                )
                PromptCategory.BRANCH_DIFF -> model.copy(
                    branchDiffPromptProfilesYaml = updateYamlProfile(model.branchDiffPromptProfilesYaml, name, "")
                )
                PromptCategory.PULL_REQUEST -> return
            }
            LlmSettingsLoader.saveSettingsModel(project, updated)
        }

        fun deletePrompt(category: PromptCategory, name: String) {
            val model = LlmSettingsLoader.loadSettingsModel(project)
            val updated = when (category) {
                PromptCategory.GENERATION -> model.copy(
                    generationPromptProfilesYaml = removeYamlProfile(model.generationPromptProfilesYaml, name)
                )
                PromptCategory.COMMIT -> model.copy(
                    commitPromptProfilesYaml = removeYamlProfile(model.commitPromptProfilesYaml, name)
                )
                PromptCategory.BRANCH_DIFF -> model.copy(
                    branchDiffPromptProfilesYaml = removeYamlProfile(model.branchDiffPromptProfilesYaml, name)
                )
                PromptCategory.PULL_REQUEST -> return
            }
            LlmSettingsLoader.saveSettingsModel(project, updated)
        }

        fun buildPromptLeafMap(): LinkedHashMap<String, Pair<PromptCategory, List<PromptLeaf>>> {
            val config = runCatching { LlmSettingsLoader.loadConfig(project) }.getOrNull()
                ?: return linkedMapOf("Error" to (PromptCategory.PULL_REQUEST to listOf(PromptLeaf("Load failed", "Unable to load .ai-test.yaml"))))
            val prompts = config.prompts
            return linkedMapOf(
                "Test Generation" to (PromptCategory.GENERATION to prompts.profiles.generation.items.entries.map { PromptLeaf(it.key, it.value) }),
                "Commit Message" to (PromptCategory.COMMIT to prompts.profiles.commitMessage.items.entries.map { PromptLeaf(it.key, it.value) }),
                "Branch Diff" to (PromptCategory.BRANCH_DIFF to prompts.profiles.branchDiffSummary.items.entries.map { PromptLeaf(it.key, it.value) }),
                "Pull Request" to (PromptCategory.PULL_REQUEST to listOf(PromptLeaf("default", prompts.pullRequest)))
            )
        }

        fun refreshPromptTabs() {
            promptCategoryTabs.removeAll()
            val promptMap = buildPromptLeafMap()
            promptMap.forEach { (categoryName, categoryAndItems) ->
                val (category, items) = categoryAndItems
                val promptArea = JTextArea().apply {
                    isEditable = true
                    lineWrap = true
                    wrapStyleWord = true
                    font = commonFont
                    background = bgColor
                    foreground = fgColor
                    caretColor = fgColor
                    border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    text = ""
                }
                val listModel = DefaultListModel<PromptLeaf>().apply {
                    items.forEach { addElement(it) }
                }
                val list = JList(listModel).apply {
                    selectionMode = ListSelectionModel.SINGLE_SELECTION
                    font = commonFont
                    background = bgColor
                    foreground = fgColor
                    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    selectionBackground = Color(0x2A, 0x2A, 0x2A)
                    selectionForeground = fgColor
                }

                val editorPanel = JPanel(BorderLayout()).apply {
                    add(styledScrollPane(promptArea), BorderLayout.CENTER)
                    isVisible = false
                }

                list.addListSelectionListener {
                    val selected = list.selectedValue
                    if (selected == null) {
                        promptArea.text = ""
                        editorPanel.isVisible = false
                    } else {
                        promptArea.text = selected.content
                        promptArea.caretPosition = 0
                        editorPanel.isVisible = true
                    }
                }

                val addButton = JButton("Add").apply {
                    addActionListener {
                        if (category == PromptCategory.PULL_REQUEST) return@addActionListener
                        val name = Messages.showInputDialog(
                            project,
                            "Enter prompt name for $categoryName",
                            "Add Prompt",
                            null
                        )?.trim().orEmpty()
                        if (name.isBlank()) return@addActionListener
                        addPrompt(category, name)
                        refreshPromptTabs()
                    }
                }
                val deleteButton = JButton("Delete").apply {
                    addActionListener {
                        if (category == PromptCategory.PULL_REQUEST) return@addActionListener
                        val selected = list.selectedValue ?: return@addActionListener
                        deletePrompt(category, selected.name)
                        refreshPromptTabs()
                    }
                }
                val saveButton = JButton("Save").apply {
                    addActionListener {
                        val selected = list.selectedValue ?: return@addActionListener
                        savePromptValue(category, selected.name, promptArea.text)
                        refreshPromptTabs()
                    }
                }

                val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                    background = bgColor
                    add(addButton)
                    add(deleteButton)
                    add(saveButton)
                }

                val split = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
                    topComponent = styledScrollPane(list)
                    bottomComponent = editorPanel
                    resizeWeight = 0.45
                    border = BorderFactory.createEmptyBorder()
                    background = bgColor
                }

                val categoryPanel = JPanel(BorderLayout(0, 8)).apply {
                    background = bgColor
                    add(buttonRow, BorderLayout.NORTH)
                    add(split, BorderLayout.CENTER)
                }

                promptCategoryTabs.addTab(categoryName, categoryPanel)
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
