package com.lyihub.archiveassistant.domain

import org.json.JSONObject

class LocalLlmSmartSummarizer(private val engine: LocalLlmEngine) : SmartSummarizer {
    override suspend fun summarize(
        request: SmartSummarizeRequest,
        topics: List<Topic>,
        existingItems: List<KnowledgeItem>,
    ): SmartSummarizeResult {
        val normalizedInput = request.rawText.trim()
        if (normalizedInput.isEmpty()) {
            return SmartSummarizeResult.Failure("请输入要智能总结的内容")
        }

        if (topics.isEmpty()) {
            return SmartSummarizeResult.Failure("没有可用主题，无法智能总结")
        }

        val materialContext = MaterialContextSelector.select(normalizedInput, topics, existingItems)
        val output = engine.generate(promptFor(request, normalizedInput, materialContext), maxTokens = 768)
            .getOrElse { return SmartSummarizeResult.Failure("本地模型生成失败，请稍后重试") }
            .trim()
        if (output.isEmpty()) {
            return SmartSummarizeResult.Failure("本地模型没有返回有效结果")
        }

        return runCatching {
            parseResult(output, topics)
        }.getOrElse {
            SmartSummarizeResult.Failure("本地模型返回格式无效，请重试")
        }
    }

    private fun parseResult(output: String, topics: List<Topic>): SmartSummarizeResult.Success {
        val json = JSONObject(extractJsonObject(output))
        val topicId = json.requiredString("topicId")
        val contentType = enumValue<ContentType>(json.requiredString("contentType"))
        val tag = json.requiredString("tag")
        val title = json.requiredString("title")
        val summary = json.requiredString("summary")
        val documentFormat = enumValue<DocumentFormat>(json.requiredString("documentFormat"))
        val sourceUrl = json.optString("sourceUrl").trim()

        require(topics.any { it.id == topicId })
        require(contentType in ALLOWED_CONTENT_TYPES)

        return SmartSummarizeResult.Success.fromAiJson(
            topicId = topicId,
            contentType = contentType,
            tag = tag,
            title = title,
            summary = summary,
            documentFormat = documentFormat,
            sourceUrl = sourceUrl,
        )
    }

    private fun promptFor(
        request: SmartSummarizeRequest,
        normalizedInput: String,
        materialContext: MaterialContext,
    ): String {
        val fetchedBlock = request.fetchedWebContext?.let { ctx ->
            """
已获取的网页内容：
原始 URL：${ctx.originalUrl}
网页标题：${ctx.title}
网页描述：${ctx.description}
网页正文：${ctx.bodyText}

注意：禁止只根据 URL 猜测标题或摘要。
当 contentType 为 WEB_ARTICLE 时，title 必须使用上述网页标题（来自已获取的网页元数据/内容），不要发明摘要式的标题。
summary 必须基于上述网页正文/描述生成。
sourceUrl 必须等于原始 URL。

""".trimIndent()
        }.orEmpty()

        return """
        你是一个本地归档助手。请只基于用户原始输入和给定素材上下文进行智能总结。
        你必须从现有主题中选择且只能选择一个 topicId，topicId 必须是下列已有 ID 之一，禁止创建新主题或返回主题名称。

        用户原始输入：$normalizedInput
        来源 URL（如有）：${request.sourceUrl.orEmpty()}
        来源标题（如有）：${request.sourceTitle.orEmpty()}

        ${fetchedBlock}现有主题：
        ${materialContext.topicOptions.joinToString("\n") { "- id=${it.id}; title=${it.title}" }}

        相关已归档素材（仅作参考，不要复制为原文）：
        ${materialContext.selectedSnippets.joinToString("\n") { "- itemId=${it.itemId}; title=${it.title}; snippet=${it.snippet}" }.ifBlank { "（无）" }}

        请推断 contentType、documentFormat 和 sourceUrl（能确定时填写；不能确定时 sourceUrl 返回空字符串）。
        contentType 只能是：${ALLOWED_CONTENT_TYPES.joinToString(", ") { it.name }}。
        documentFormat 只能是：${DocumentFormat.values().joinToString(", ") { it.name }}；只有模型明确判断未知文档格式时才返回 UNKNOWN。
        tag、title、summary 必须简洁，title 不超过 28 个中文字符，summary 不超过 96 个中文字符。

        只返回严格 JSON 对象，不要 Markdown，不要解释，不要额外字段：
        {"topicId":"现有主题ID","contentType":"WEB_ARTICLE","tag":"简短标签","title":"简洁标题","summary":"一句话摘要","sourceUrl":"来源URL或空字符串","documentFormat":"PDF"}
    """.trimIndent()
    }

    private fun JSONObject.requiredString(name: String): String {
        require(has(name) && !isNull(name))
        return getString(name).trim().also {
            require(it.isNotEmpty())
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValueOf(value)

    private fun extractJsonObject(output: String): String {
        val trimmed = output.trim()
        val unfenced = if (trimmed.startsWith("```")) {
            trimmed
                .removePrefix("```")
                .removePrefix("json")
                .removePrefix("JSON")
                .trim()
                .removeSuffix("```")
                .trim()
        } else {
            trimmed
        }

        val start = unfenced.indexOf('{')
        require(start >= 0)

        var depth = 0
        var inString = false
        var isEscaped = false
        for (index in start until unfenced.length) {
            val char = unfenced[index]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            when {
                char == '\\' && inString -> isEscaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth += 1
                !inString && char == '}' -> {
                    depth -= 1
                    if (depth == 0) return unfenced.substring(start, index + 1)
                }
            }
        }

        error("No complete JSON object found")
    }

    private companion object {
        val ALLOWED_CONTENT_TYPES = setOf(
            ContentType.WEB_ARTICLE,
            ContentType.IMAGE_SCREENSHOT,
            ContentType.DOCUMENT,
        )
    }
}
