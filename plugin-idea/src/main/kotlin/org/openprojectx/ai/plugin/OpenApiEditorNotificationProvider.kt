package org.openprojectx.ai.plugin

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.JBUI

class OpenApiEditorNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

    private val key = Key.create<EditorNotificationPanel>("aitestgen.openapi.panel")

    override fun getKey(): Key<EditorNotificationPanel> = key

    override fun createNotificationPanel(
        file: VirtualFile,
        fileEditor: FileEditor,
        project: Project
    ): EditorNotificationPanel? {
        val doc = FileDocumentManager.getInstance().getDocument(file) ?: return null
        val contractText = doc.text ?: return null

        if (!OpenApiHeuristics.looksLikeOpenApi(file, contractText)) return null

        val stateService = OpenApiNotificationStateService.getInstance(project)
        val state = stateService.getState(file.path)

        return EditorNotificationPanel(fileEditor).apply {
            border = JBUI.Borders.empty(10, 0)

            when (state) {
                GenerationUiState.Idle -> {
                    text = "OpenAPI contract detected"
                    icon(OpenProjectXIcons.GenerateTests)
                }

                GenerationUiState.Generating -> {
                    text = "Generating tests with AI..."
                    icon(AnimatedIcon.Default())
                }

                GenerationUiState.Done -> {
                    text = "Tests generated successfully"
                    icon(OpenProjectXIcons.GenerateTests)
                }
            }

            createActionLabel("Generate Tests By AI") {
                stateService.setState(file.path, GenerationUiState.Generating)
                EditorNotifications.getInstance(project).updateNotifications(file)

                GenerateTestsDialog.open(project, file, contractText)
            }
        }
    }
}