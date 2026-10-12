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
 * A single chat message. [role] is one of "system", "user", "assistant",
 * "developer", or "tool", as accepted by the /chat/completions endpoint.
 */
@Serializable
data class ChatMessage(
    val role: String,
    // Nullable so assistant messages that omit content (reasoning-only,
    // refusals, tool calls) can still be deserialized.
    val content: String? = null
)

/**
 * Chat completion request for the /chat/completions endpoint. With
 * [json] configured with encodeDefaults disabled, unset optional fields
 * and the default stream=false are not serialized.
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false
)

/**
 * Chat completion response for the /chat/completions endpoint. Reuses the
 * image endpoint usage shape — both endpoints share the same usage fields.
 */
@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val created: Long? = null,
    val choices: List<ChatChoice>? = null,
    val usage: ImageGenerationUsage? = null,
    val error: OpenRouterError? = null
)

@Serializable
data class ChatChoice(
    val index: Int? = null,
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

/**
 * Proxy protocol used to route OpenRouter API requests.
 */
enum class ImageProxyProtocol {
    HTTP,
    SOCKS5
}
