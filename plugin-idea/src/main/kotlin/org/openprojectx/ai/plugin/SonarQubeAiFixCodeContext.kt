package org.openprojectx.ai.plugin

internal object SonarQubeAiFixCodeContext {
    private val methodStartPattern = Regex("""\b[\w<>\[\]?]+\s+\w+\s*\([^)]*\)\s*(?::\s*[\w<>\[\]?]+)?\s*\{?""")
    private val kotlinFunctionPattern = Regex("""\bfun\s+\w+\s*\([^)]*\)\s*(?::\s*[\w<>\[\]?]+)?\s*\{?""")
    private val classStartPattern = Regex("""\b(class|interface|enum|object)\s+\w+""")
    private val excludedStarts = setOf("if", "for", "while", "switch", "catch", "class", "interface", "enum", "object")

    data class Target(
        val startLine: Int,
        val endLine: Int,
        val code: String,
        val containingClassCode: String
    ) {
        val description: String = "lines $startLine-$endLine"
    }

    fun findTarget(sourceCode: String, issueLine: Int?): Target? {
        if (issueLine == null || issueLine <= 0) return null
        val lines = sourceCode.lines()
        val issueIndex = (issueLine - 1).coerceIn(0, lines.lastIndex)
        val startIndex = findMethodStart(lines, issueIndex) ?: return null
        val endIndex = findBlockEnd(lines, startIndex) ?: return null
        val classStartIndex = findClassStart(lines, startIndex)
        val classEndIndex = classStartIndex?.let { findBlockEnd(lines, it) }
        val containingClassCode = if (classStartIndex != null && classEndIndex != null) {
            lines.subList(classStartIndex, classEndIndex + 1).joinToString("\n")
        } else {
            sourceCode
        }
        return Target(
            startLine = startIndex + 1,
            endLine = endIndex + 1,
            code = lines.subList(startIndex, endIndex + 1).joinToString("\n"),
            containingClassCode = containingClassCode
        )
    }

    fun mergeFixedTarget(sourceCode: String, target: Target?, fixedTargetCode: String): String {
        if (target == null) return fixedTargetCode.trimEnd()
        val lines = sourceCode.lines().toMutableList()
        val startIndex = (target.startLine - 1).coerceAtLeast(0)
        val endIndex = (target.endLine - 1).coerceAtMost(lines.lastIndex)
        val replacement = fixedTargetCode.trimEnd().lines()
        repeat(endIndex - startIndex + 1) {
            lines.removeAt(startIndex)
        }
        lines.addAll(startIndex, replacement)
        return lines.joinToString("\n")
    }

    private fun findMethodStart(lines: List<String>, issueIndex: Int): Int? {
        for (index in issueIndex downTo 0) {
            val trimmed = lines[index].trim()
            if (trimmed.isBlank() || trimmed.startsWith("//") || trimmed.startsWith("*")) continue
            val keyword = trimmed.substringBefore(' ').substringBefore('(')
            if (keyword in excludedStarts) continue
            if (kotlinFunctionPattern.containsMatchIn(trimmed) || methodStartPattern.containsMatchIn(trimmed)) {
                return index
            }
        }
        return null
    }

    private fun findClassStart(lines: List<String>, methodStartIndex: Int): Int? {
        for (index in methodStartIndex downTo 0) {
            val trimmed = lines[index].trim()
            if (classStartPattern.containsMatchIn(trimmed)) return index
        }
        return null
    }

    private fun findBlockEnd(lines: List<String>, startIndex: Int): Int? {
        var depth = 0
        var seenOpeningBrace = false
        for (index in startIndex until lines.size) {
            for (char in lines[index]) {
                when (char) {
                    '{' -> {
                        depth++
                        seenOpeningBrace = true
                    }
                    '}' -> if (seenOpeningBrace) depth--
                }
            }
            if (seenOpeningBrace && depth == 0) return index
        }
        return if (seenOpeningBrace) lines.lastIndex else startIndex
    }
}
