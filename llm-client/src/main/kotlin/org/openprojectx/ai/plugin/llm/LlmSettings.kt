package org.openprojectx.ai.plugin.llm

data class LlmSettings(
    val provider: String,
    val model: String,
    val timeoutSeconds: Long = 60,
    val apiKey: String? = null,
    val endpoint: String? = null,
    val template: TemplateLlmConfig? = null
)

data class TemplateLlmConfig(
    val method: String = "POST",
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String,
    val responsePath: String
)