package com.lyihub.archiveassistant.data

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.lyihub.archiveassistant.domain.BenchResult
import com.lyihub.archiveassistant.domain.InferenceBackend
import com.lyihub.archiveassistant.domain.LocalLlmEngine
import com.lyihub.archiveassistant.domain.LocalModelState
import com.lyihub.archiveassistant.domain.LocalModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalApi::class)
class LiteRtLmEngineAdapter(private val context: Context) : LocalLlmEngine {
    private val stateFlow = MutableStateFlow(LocalModelState())
    private var engine: Engine? = null
    private var activeBackend: InferenceBackend = InferenceBackend.UNKNOWN
    private var activeModelPath: String? = null

    override val state: Flow<LocalModelState> = stateFlow

    override suspend fun initialize(modelPath: String, backend: InferenceBackend): Result<InferenceBackend> =
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(INITIALIZE_TIMEOUT_MS) {
                    stateFlow.value = LocalModelState(
                        status = LocalModelStatus.INITIALIZING,
                        activeBackend = InferenceBackend.UNKNOWN,
                        modelPath = modelPath,
                    )

                    val attempts = backendAttempts(backend)
                    var lastError: Throwable? = null
                    for (attempt in attempts) {
                        val candidate = createEngineBackend(attempt)
                        val initialized = runCatching {
                            Engine(EngineConfig(modelPath = modelPath, backend = candidate)).also { it.initialize() }
                        }
                        initialized.onSuccess { readyEngine ->
                            engine?.close()
                            engine = readyEngine
                            activeBackend = attempt
                            activeModelPath = modelPath
                            stateFlow.value = LocalModelState(
                                status = LocalModelStatus.READY,
                                activeBackend = attempt,
                                modelPath = modelPath,
                            )
                            return@withTimeout attempt
                        }.onFailure { error ->
                            lastError = error
                        }
                    }

                    val error = lastError ?: IllegalStateException("No inference backend available")
                    stateFlow.value = LocalModelState(
                        status = LocalModelStatus.ERROR,
                        activeBackend = InferenceBackend.UNKNOWN,
                        errorMessage = error.message,
                        modelPath = modelPath,
                    )
                    throw error
                }
            }.onFailure { error ->
                stateFlow.value = LocalModelState(
                    status = LocalModelStatus.ERROR,
                    activeBackend = activeBackend,
                    errorMessage = error.message,
                    modelPath = modelPath,
                )
            }
        }

    override suspend fun generate(prompt: String, maxTokens: Int): Result<String> = withContext(Dispatchers.IO) {
        val currentEngine = engine
            ?: return@withContext Result.failure(IllegalStateException("Local LLM engine is not initialized"))

        runCatching {
            stateFlow.value = readyState(LocalModelStatus.INFERENCING)
            try {
                withTimeout(GENERATE_TIMEOUT_MS) {
                    currentEngine.createConversation().use { conversation ->
                        val response = conversation.sendMessage(prompt)
                        conversation.renderMessageIntoString(response).trim()
                    }
                }
            } finally {
                stateFlow.value = readyState(LocalModelStatus.READY)
            }
        }.onFailure { error ->
            stateFlow.value = readyState(LocalModelStatus.READY, error.message)
        }
    }

    override suspend fun benchmark(promptTokens: Int, generateTokens: Int): Result<BenchResult> =
        withContext(Dispatchers.IO) {
            if (engine == null) {
                return@withContext Result.failure(IllegalStateException("Local LLM engine is not initialized"))
            }

            runCatching {
                val prompt = benchmarkPrompt(promptTokens)
                val prefillStart = System.nanoTime()
                val result = generate(prompt, maxTokens = generateTokens).getOrThrow()
                val totalNs = System.nanoTime() - prefillStart
                val prefillNs = totalNs.coerceAtLeast(1L)
                val estimatedDecodeNs = result.length.coerceAtLeast(1) * 1_000_000L

                BenchResult(
                    promptTokens = promptTokens,
                    generateTokens = generateTokens,
                    prefillTokensPerSecond = tokensPerSecond(promptTokens, prefillNs),
                    decodeTokensPerSecond = tokensPerSecond(generateTokens, estimatedDecodeNs),
                    totalTimeMs = totalNs / 1_000_000,
                    backend = activeBackend,
                )
            }
        }

    override suspend fun release(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            engine?.close()
            engine = null
            activeBackend = InferenceBackend.UNKNOWN
            activeModelPath = null
            stateFlow.value = LocalModelState(status = LocalModelStatus.NOT_DOWNLOADED)
        }
    }

    private fun backendAttempts(preferredBackend: InferenceBackend): List<InferenceBackend> {
        val fallbackOrder = listOf(InferenceBackend.NPU, InferenceBackend.GPU, InferenceBackend.CPU)
        return if (preferredBackend in fallbackOrder) {
            listOf(preferredBackend) + fallbackOrder.filterNot { it == preferredBackend }
        } else {
            fallbackOrder
        }
    }

    private fun createEngineBackend(backend: InferenceBackend): Backend = when (backend) {
        InferenceBackend.NPU -> Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
        InferenceBackend.GPU -> Backend.GPU()
        InferenceBackend.CPU -> Backend.CPU()
        InferenceBackend.UNKNOWN -> Backend.CPU()
    }

    private fun readyState(status: LocalModelStatus, errorMessage: String? = null): LocalModelState =
        LocalModelState(
            status = status,
            activeBackend = activeBackend,
            errorMessage = errorMessage,
            modelPath = activeModelPath,
        )

    private fun benchmarkPrompt(promptTokens: Int): String {
        val phrase = "The quick brown fox jumps over the lazy dog. "
        val repeats = (promptTokens / 9).coerceAtLeast(1)
        return buildString {
            repeat(repeats) { append(phrase) }
        }.trim()
    }

    private fun tokensPerSecond(tokens: Int, elapsedNs: Long): Float =
        (tokens.toDouble() * 1_000_000_000.0 / elapsedNs.toDouble()).toFloat()

    private companion object {
        const val INITIALIZE_TIMEOUT_MS = 60_000L
        const val GENERATE_TIMEOUT_MS = 120_000L
    }
}
