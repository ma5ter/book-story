package su.mya.openrouter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String
)

@Serializable
data class ImageGenerationResponse(
    val created: Long? = null,
    val data: List<GeneratedImage>? = null,
    val usage: ImageGenerationUsage? = null,
    val error: OpenRouterError? = null
)

@Serializable
data class GeneratedImage(
    @SerialName("b64_json") val b64Json: String,
    @SerialName("media_type") val mediaType: String? = null
)

@Serializable
data class ImageGenerationUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null,
    val cost: Double? = null
)

@Serializable
data class OpenRouterError(
    val message: String? = null,
    val code: JsonElement? = null,
    val metadata: JsonElement? = null
)

@Serializable
data class OpenRouterErrorEnvelope(
    val error: OpenRouterError? = null
)

/**
 * Proxy protocol used to route OpenRouter API requests.
 */
enum class ImageProxyProtocol {
    HTTP,
    SOCKS5
}
