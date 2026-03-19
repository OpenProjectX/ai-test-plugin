package org.openprojectx.ai.plugin

import com.intellij.openapi.vfs.VirtualFile

object JavaHeuristics {
    fun looksLikeJavaSource(file: VirtualFile, text: String): Boolean {
        if (!file.name.lowercase().endsWith(".java")) return false

        val hasTypeDeclaration = Regex("""(?m)^\s*(public\s+)?(class|interface|record|enum)\s+\w+""")
            .containsMatchIn(text)
        if (!hasTypeDeclaration) return false

        val hasMethodSignature = Regex("""(?m)^\s*(public|protected|private)\s+[\w<>,\[\]\s]+\s+\w+\s*\(""")
            .containsMatchIn(text)

        return hasMethodSignature
    }
}
