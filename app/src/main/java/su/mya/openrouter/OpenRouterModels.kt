package su.mya.openrouter

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * OpenRouter provider routing preferences. All fields are optional;
 * omitted ones fall back to OpenRouter's default load balancing.
 */
@Serializable
data class ProviderRouting(
    val sort: ProviderSort? = null,
    @SerialName("preferred_max_latency") val preferredMaxLatency: PreferredMaxLatency? = null,
    @SerialName("max_price") val maxPrice: MaxPrice? = null
)

/**
 * OpenRouter provider sorting strategy. Serialized either as a bare
 * string (`"sort": "price"`) or as an object with a `by` field
 * (`"sort": {"by": "throughput"}`), both forms accepted by the
 * `provider.sort` parameter.
 */
@Serializable(with = ProviderSortSerializer::class)
sealed interface ProviderSort {

    /** Bare string form, e.g. `ProviderSort.OfString("price")`. */
    data class OfString(val value: String) : ProviderSort

    /** Object form, e.g. `ProviderSort.OfObject(by = "throughput")`. */
    data class OfObject(val by: String) : ProviderSort
}

/**
 * Serializes [ProviderSort] variants into their respective JSON forms:
 * a bare string or an object with a `by` field.
 */
object ProviderSortSerializer : KSerializer<ProviderSort> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ProviderSort")

    override fun serialize(encoder: Encoder, value: ProviderSort) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("ProviderSort can only be serialized as JSON.")
        when (value) {
            is ProviderSort.OfString -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is ProviderSort.OfObject -> jsonEncoder.encodeJsonElement(
                buildJsonObject { put("by", value.by) }
            )
        }
    }

    override fun deserialize(decoder: Decoder): ProviderSort {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("ProviderSort can only be deserialized from JSON.")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> ProviderSort.OfString(element.content)
            is JsonObject -> ProviderSort.OfObject(
                (element["by"] as? JsonPrimitive)?.content ?: ""
            )
            else -> error("Unsupported provider sort element: $element.")
        }
    }
}

/**
 * Preferred maximum latency percentile cutoff (seconds). Providers that
 * miss the threshold are only deprioritized, never excluded.
 */
@Serializable
data class PreferredMaxLatency(
    val p90: Int? = null
)

/**
 * Hard price ceiling per million tokens: the request fails when no
 * provider fits within the limit.
 */
@Serializable
data class MaxPrice(
    val prompt: Double? = null,
    val completion: Double? = null
)

/**
 * A reference image for image-to-image generation, sent under
 * `input_references` as a base64 data URL.
 */
@Serializable
data class ImageReference(
    val type: String,
    @SerialName("image_url") val imageUrl: ImageUrl
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class ImageGenerationRequest(
    val model: String,
    val prompt: String,
    @SerialName("input_references") val inputReferences: List<ImageReference>? = null,
    val provider: ProviderRouting
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
    val stream: Boolean = false,
    val provider: ProviderRouting
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
