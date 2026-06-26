package com.lyihub.archiveassistant.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockKnowledgeClassifierTest {
    private val classifier = MockKnowledgeClassifier()

    @Test
    fun classify_urlArticle_returnsWebArticleResult() {
        val result = classifier.classify("https://example.com/article about transformer architecture")

        val payload = result.assertClassified()
        assertEquals(ContentType.WEB_ARTICLE, payload.contentType)
        assertEquals(SampleKnowledgeData.DefaultTopicId, payload.topicId)
    }

    @Test
    fun classify_imageScreenshot_returnsImageResult() {
        val result = classifier.classify("UX screenshot image of a settings panel")

        val payload = result.assertClassified()
        assertEquals(ContentType.IMAGE_SCREENSHOT, payload.contentType)
        assertEquals(SixMinistryCatalog.LiRitesTopicId, payload.topicId)
    }

    @Test
    fun classify_pdfDocument_returnsDocumentResult() {
        val result = classifier.classify("PDF document report about attention mechanism")

        val payload = result.assertClassified()
        assertEquals(ContentType.DOCUMENT, payload.contentType)
        assertEquals(DocumentFormat.PDF, payload.documentFormat)
        assertEquals(SampleKnowledgeData.DefaultTopicId, payload.topicId)
    }

    @Test
    fun classify_markdownFile_returnsDocumentWithMarkdownFormat() {
        val result = classifier.classify("readme.md project documentation in markdown")

        val payload = result.assertClassified()
        assertEquals(ContentType.DOCUMENT, payload.contentType)
        assertEquals(DocumentFormat.MARKDOWN, payload.documentFormat)
    }

    @Test
    fun classify_docxFile_returnsDocumentWithDocxFormat() {
        val result = classifier.classify("report.docx word document")

        val payload = result.assertClassified()
        assertEquals(ContentType.DOCUMENT, payload.contentType)
        assertEquals(DocumentFormat.DOCX, payload.documentFormat)
    }

    @Test
    fun classify_txtFile_returnsDocumentWithTxtFormat() {
        val result = classifier.classify("notes.txt 纯文本")

        val payload = result.assertClassified()
        assertEquals(ContentType.DOCUMENT, payload.contentType)
        assertEquals(DocumentFormat.TXT, payload.documentFormat)
    }

    @Test
    fun classify_unknownDocument_returnsDocumentWithUnknownFormat() {
        val result = classifier.classify("document 文档 without specific format clue")

        val payload = result.assertClassified()
        assertEquals(ContentType.DOCUMENT, payload.contentType)
        assertEquals(DocumentFormat.UNKNOWN, payload.documentFormat)
    }

    @Test
    fun classify_plainText_returnsWebArticleFallback() {
        val result = classifier.classify("田野笔记里关于仪式交换的一段摘录")

        val payload = result.assertClassified()
        assertEquals(ContentType.WEB_ARTICLE, payload.contentType)
        assertEquals(SixMinistryCatalog.LiRitesTopicId, payload.topicId)
    }

    @Test
    fun classify_blankInput_returnsValidationResult() {
        val result = classifier.classify("   \n\t ")

        assertTrue(result is ClassificationResult.BlankInput)
        assertEquals("请输入要归档的内容", (result as ClassificationResult.BlankInput).message)
    }

    @Test
    fun sampleData_containsPrototypeLabelsAndDeterministicFixtures() {
        assertEquals(
            listOf("全部", "网页", "图像", "文档"),
            ContentType.entries.map { it.label },
        )
        assertEquals(
            listOf("吏部 · 人物组织", "户部 · 商业经济", "礼部 · 文化审美", "兵部 · 趋势战略", "刑部 · 事实核验", "工部 · 技术产品"),
            SampleKnowledgeData.topics.map { it.title },
        )
        assertTrue(SampleKnowledgeData.items.any { it.contentType == ContentType.WEB_ARTICLE })
        assertTrue(SampleKnowledgeData.items.any { it.contentType == ContentType.IMAGE_SCREENSHOT })
        assertTrue(SampleKnowledgeData.items.any { it.contentType == ContentType.DOCUMENT })
        assertTrue(SampleKnowledgeData.topics.all { it.id.isNotBlank() && it.updatedAtEpochMillis > 0L })
        assertTrue(SampleKnowledgeData.items.all { it.id.isNotBlank() && it.createdAtEpochMillis > 0L })
    }

    private fun ClassificationResult.assertClassified(): ClassificationPayload {
        assertTrue(this is ClassificationResult.Classified)
        return (this as ClassificationResult.Classified).payload
    }
}
