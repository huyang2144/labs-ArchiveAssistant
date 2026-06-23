package com.lyihub.archiveassistant.domain

import org.json.JSONObject

class LocalLlmClassifier(private val engine: LocalLlmEngine) {
    suspend fun classify(rawInput: String, topics: List<Topic>): ClassificationResult {
        val normalizedInput = rawInput.trim()
        if (normalizedInput.isEmpty()) {
            return ClassificationResult.BlankInput()
        }

        val output = engine.generate(promptFor(normalizedInput, topics)).getOrElse {
            return ClassificationResult.Unknown
        }.trim()
        if (output.isEmpty()) {
            return ClassificationResult.Unknown
        }

        return runCatching {
            val json = JSONObject(output)
            val topicName = json.optString("topic").trim()
            val matchedTopic = topics.firstOrNull { it.title == topicName }
                ?: return ClassificationResult.Unknown

            ClassificationResult.Classified(
                ClassificationPayload(
                    topicId = matchedTopic.id,
                    contentType = ContentType.WEB_ARTICLE,
                    tag = ContentType.WEB_ARTICLE.label,
                    title = normalizedInput.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(28)
                        ?: "提取内容",
                    summary = json.optString("reason").trim().ifBlank { normalizedInput.take(96) },
                    rawInput = normalizedInput,
                ),
            )
        }.getOrElse {
            ClassificationResult.Unknown
        }
    }

    private fun promptFor(rawInput: String, topics: List<Topic>): String = """
        你是一个归档助手。根据以下输入内容，从给定主题列表中选择最匹配的主题。
        输入内容：$rawInput
        可选主题：${topics.joinToString(", ") { it.title }}
        请只返回 JSON，格式：{"topic": "主题名", "confidence": 0.0-1.0, "reason": "简短理由"}
    """.trimIndent()
}
