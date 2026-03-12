package org.openprojectx.ai.plugin.llm

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.ktor.client.HttpClient
import io.ktor.client.request.header
//import io.ktor.client.request.method
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import kotlinx.serialization.builtins.serializer

class TemplateLlmProvider(
    private val http: HttpClient,
    private val settings: LlmSettings
) : LlmProvider {

    private val handlebars = Handlebars()
    private val json = Json

    override suspend fun generateCode(prompt: String): String {
        val config = settings.template
            ?: error("Template config is required for provider='template'")

        val variables = mapOf(
            "model" to settings.model,
            "apiKey" to (settings.apiKey ?: ""),
            "prompt" to prompt,
            "promptJson" to json.encodeToString(String.serializer(), prompt)
        )

        val renderedUrl = render(config.url, variables)
        val renderedHeaders = config.headers.mapValues { (_, value) -> render(value, variables) }
        val renderedBody = render(config.body, variables)

        val responseText = http.request {
            url(renderedUrl)
            method = HttpMethod.parse(config.method.uppercase())

            renderedHeaders.forEach { (name, value) ->
                header(name, value)
            }

            if (renderedHeaders.keys.none { it.equals("Content-Type", ignoreCase = true) }) {
                contentType(ContentType.Application.Json)
            }

            setBody(renderedBody)
        }.bodyAsText()

        return try {
            JsonPath.read<String>(responseText, config.responsePath)
        } catch (e: PathNotFoundException) {
            error("LLM responsePath '${config.responsePath}' did not match response: ${e.message}")
        }
    }

    private fun render(templateText: String, variables: Map<String, Any>): String {
        val template: Template = handlebars.compileInline(templateText)
        return template.apply(variables)
    }
}