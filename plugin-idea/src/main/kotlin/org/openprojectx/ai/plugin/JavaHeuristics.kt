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

    fun isTestJavaPath(file: VirtualFile): Boolean {
        val normalized = file.path.replace('\\', '/').lowercase()
        return normalized.contains("/src/test/java/")
    }

    fun deriveTestLocationForMainJava(file: VirtualFile, projectBasePath: String?): String? {
        val basePath = projectBasePath ?: return null
        val normalizedFilePath = file.path.replace('\\', '/')
        val normalizedBasePath = basePath.replace('\\', '/').removeSuffix("/")
        val prefix = "$normalizedBasePath/"
        if (!normalizedFilePath.startsWith(prefix)) return null

        val relative = normalizedFilePath.removePrefix(prefix)
        val marker = "/src/main/java/"
        val index = relative.indexOf(marker)
        if (index < 0) return null

        val modulePrefix = relative.substring(0, index).trim('/')
        return listOfNotNull(
            modulePrefix.takeIf { it.isNotBlank() },
            "src/test/java"
        ).joinToString("/")
    }

    fun derivePackageNameForJava(file: VirtualFile, projectBasePath: String?): String? {
        val basePath = projectBasePath ?: return null
        val normalizedFilePath = file.path.replace('\\', '/')
        val normalizedBasePath = basePath.replace('\\', '/').removeSuffix("/")
        val prefix = "$normalizedBasePath/"
        if (!normalizedFilePath.startsWith(prefix)) return null

        val relative = normalizedFilePath.removePrefix(prefix)
        val marker = "/src/main/java/"
        val index = relative.indexOf(marker)
        if (index < 0) return null

        val afterMain = relative.substring(index + marker.length)
        val packagePath = afterMain.substringBeforeLast('/', missingDelimiterValue = "")
        if (packagePath.isBlank()) return null

        return packagePath.replace('/', '.').trim('.').takeIf { it.isNotBlank() }
    }
}
