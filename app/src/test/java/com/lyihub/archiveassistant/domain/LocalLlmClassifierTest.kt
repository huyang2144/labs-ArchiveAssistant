package com.lyihub.archiveassistant.domain

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLlmClassifierTest {
    private val topics = listOf(
        Topic(
            id = "topic-1",
            title = "Topic1",
            iconName = "folder",
            iconColor = "#111111",
            updatedAtEpochMillis = 1L,
        ),
        Topic(
            id = "topic-2",
            title = "Topic2",
            iconName = "folder",
            iconColor = "#222222",
            updatedAtEpochMillis = 2L,
        ),
    )

    @Test
    fun classifySuccess() = runTest {
        val classifier = classifierReturning("""{"topic": "Topic1", "confidence": 0.9, "reason": "match"}""")

        val result = classifier.classify("content to classify", topics)

        assertTrue(result is ClassificationResult.Classified)
        val payload = (result as ClassificationResult.Classified).payload
        assertEquals("topic-1", payload.topicId)
        assertEquals("match", payload.summary)
        assertEquals("content to classify", payload.rawInput)
    }

    @Test
    fun malformedJsonReturnsUnknown() = runTest {
        val classifier = classifierReturning("not json text")

        val result = classifier.classify("content to classify", topics)

        assertTrue(result is ClassificationResult.Unknown)
    }

    @Test
    fun emptyOutputReturnsUnknown() = runTest {
        val classifier = classifierReturning("")

        val result = classifier.classify("content to classify", topics)

        assertTrue(result is ClassificationResult.Unknown)
    }

    @Test
    fun engineFailureReturnsUnknown() = runTest {
        val engine = initializedEngine("unused").apply {
            generateFailure = IllegalStateException("generation failed")
        }
        val classifier = LocalLlmClassifier(engine)

        val result = classifier.classify("content to classify", topics)

        assertTrue(result is ClassificationResult.Unknown)
    }

    @Test
    fun emptyTopicsListReturnsUnknown() = runTest {
        val classifier = classifierReturning("""{"topic": "Topic1", "confidence": 0.9, "reason": "match"}""")

        val result = classifier.classify("content to classify", emptyList())

        assertTrue(result is ClassificationResult.Unknown)
    }

    @Test
    fun veryLongRawInput_doesNotCrashAndPreservesInput() = runTest {
        val rawInput = "Long input ".repeat(1_100)
        val classifier = classifierReturning("""{"topic": "Topic1", "confidence": 0.9, "reason": "long match"}""")

        val result = classifier.classify(rawInput, topics)

        assertTrue(result is ClassificationResult.Classified)
        assertEquals(rawInput.trim(), (result as ClassificationResult.Classified).payload.rawInput)
    }

    @Test
    fun confidenceOutOfRange_stillParsesMatchingTopic() = runTest {
        val classifier = classifierReturning("""{"topic": "Topic1", "confidence": 1.5, "reason": "match"}""")

        val result = classifier.classify("content to classify", topics)

        assertTrue(result is ClassificationResult.Classified)
        assertEquals("topic-1", (result as ClassificationResult.Classified).payload.topicId)
    }

    @Test
    fun inferenceTimeout() = runTest {
        val engine = initializedEngine("""{"topic": "Topic1", "confidence": 0.9, "reason": "slow"}""").apply {
            delayMillis = 121_000L
        }
        val classifier = LocalLlmClassifier(engine)

        val result = withTimeoutOrNull(120_000L) {
            classifier.classify("content to classify", topics)
        }

        assertEquals(null, result)
    }

    @Test
    fun topicNotFoundReturnsUnknown() = runTest {
        val classifier = classifierReturning("""{"topic": "Missing", "confidence": 0.9, "reason": "match"}""")

        val result = classifier.classify("content to classify", topics)

        assertTrue(result is ClassificationResult.Unknown)
    }

    private suspend fun classifierReturning(output: String): LocalLlmClassifier = LocalLlmClassifier(initializedEngine(output))

    private suspend fun initializedEngine(output: String): FakeLocalLlmEngine = FakeLocalLlmEngine(returnText = output).also { engine ->
        engine.initialize("/tmp/model.litertlm", InferenceBackend.CPU).getOrThrow()
    }
}
