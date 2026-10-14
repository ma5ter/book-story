package su.mya.openrouter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Base64
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.settings.SettingsManager
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * OkHttp-based client for the OpenRouter image generation and chat
 * completion APIs.
 */
@Singleton
class OpenRouterClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsManager: SettingsManager
) {

    /**
     * Sends the selected text together with the configured system prompt
     * to the configured image generation model via the /images endpoint
     * and returns the decoded image.
     */
    suspend fun generateImage(prompt: String): Result<CoverImage> {
        logI(TAG, "generateImage: started, selected text length: ${prompt.length}.")
        val model = settingsManager.imageGenerationModel.lastValue.trim()
        requireValidConfiguration(model, "Image model").fold(
            onSuccess = {},
            onFailure = { return Result.failure(it) }
        )

        val requestBody = json.encodeToString(
            ImageGenerationRequest.serializer(),
            ImageGenerationRequest(
                model = model,
                prompt = prompt,
                provider = ProviderRouting(sort = "price")
            )
        )
        val request = buildRequest(
            endpoint = "images",
            apiKey = settingsManager.imageGenerationApiKey.lastValue.trim(),
            requestBody = requestBody
        )
        logI(
            TAG,
            "generateImage: sending request, model: [$model], prompt length: ${prompt.length}."
        )

        return execute(request, "generateImage") { parseGeneratedImage(it) }
            .onSuccess { image ->
                logI(TAG, "generateImage: succeeded, image size: ${image.width}x${image.height}.")
            }
    }

    /**
     * Sends the given chat messages to the given text model via the
     * /chat/completions endpoint and returns the assistant's reply.
     */
    suspend fun chatCompletion(
        model: String,
        messages: List<ChatMessage>
    ): Result<ChatMessage> {
        logI(TAG, "chatCompletion: started, messages: ${messages.size}.")
        val resolvedModel = model.trim()
        requireValidConfiguration(resolvedModel, "Text model").fold(
            onSuccess = {},
            onFailure = { return Result.failure(it) }
        )

        val requestBody = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(
                model = resolvedModel,
                messages = messages,
                provider = ProviderRouting(sort = "price")
            )
        )
        val request = buildRequest(
            endpoint = "chat/completions",
            apiKey = settingsManager.imageGenerationApiKey.lastValue.trim(),
            requestBody = requestBody
        )
        logI(TAG, "chatCompletion: sending request, model: [$resolvedModel].")

        return execute(request, "chatCompletion") { parseChatCompletion(it) }
            .onSuccess { message ->
                logI(TAG, "chatCompletion: succeeded, role: ${message.role}.")
            }
    }

    /**
     * Validates configuration shared by all OpenRouter requests: API key,
     * model slug, and proxy setup. Returns a failure result describing the
     * first misconfigured setting, or success when everything is valid.
     */
    private fun requireValidConfiguration(model: String, modelLabel: String): Result<Unit> {
        if (settingsManager.imageGenerationApiKey.lastValue.trim().isBlank()) {
            logI(TAG, "requireValidConfiguration: failed validation, API key is not configured.")
            return Result.failure(
                IllegalStateException(
                    "API key is not configured. Set it under Settings - Reader - AI."
                )
            )
        }
        if (model.isBlank()) {
            logI(TAG, "requireValidConfiguration: failed validation, $modelLabel is not configured.")
            return Result.failure(
                IllegalStateException(
                    "$modelLabel is not configured. Set it under Settings - Reader - AI."
                )
            )
        }
        if (resolveClient() == null) {
            logI(TAG, "requireValidConfiguration: failed validation, proxy is misconfigured.")
            return Result.failure(
                IllegalStateException(
                    "Proxy is misconfigured. Provide a valid host and port (1-65535) " +
                            "under Settings -> Reader -> AI."
                )
            )
        }
        return Result.success(Unit)
    }

    /**
     * Builds a POST request to the given OpenRouter endpoint with a JSON
     * body, authentication, and JSON accept headers.
     */
    private fun buildRequest(endpoint: String, apiKey: String, requestBody: String): Request {
        return Request.Builder()
            .url("$OPENROUTER_BASE_URL/$endpoint")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
    }

    /**
     * Executes the given request on the resolved client, converts HTTP
     * failures to descriptive exceptions and parses the response body with
     * [parse]. Transport and parsing errors are wrapped into the result.
     */
    private suspend fun <T> execute(request: Request, logTag: String, parse: (String) -> T): Result<T> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val requestClient = resolveClient()
                if (requestClient == null) {
                    logI(TAG, "$logTag: failed validation, proxy is misconfigured.")
                    throw IllegalStateException(
                        "Proxy is misconfigured. Provide a valid host and port (1-65535) " +
                                "under Settings -> Reader -> AI."
                    )
                }
                val response = requestClient.newCall(request).await()
                response.use {
                    val responseBody = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        logI(TAG, "$logTag: HTTP failure, code: ${it.code}.")
                        throw buildHttpException(it.code, responseBody)
                    }
                    logI(
                        TAG,
                        "$logTag: HTTP success, code: ${it.code}, response length: ${responseBody.length}."
                    )
                    parse(responseBody)
                }
            }
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
            logI(
                TAG,
                "$logTag: failed with ${throwable::class.java.simpleName}: ${throwable.message}."
            )
        }
    }

    /**
     * Returns the OkHttp client for this request: the injected base client when
     * the proxy is disabled, otherwise a client derived from it (sharing the
     * base client's connection pool and dispatcher) configured with the
     * selected proxy. Returns null when the proxy is enabled but misconfigured.
     */
    private fun resolveClient(): OkHttpClient? {
        if (!settingsManager.imageGenerationProxyEnabled.lastValue) return okHttpClient

        val protocol = when (settingsManager.imageGenerationProxyProtocol.lastValue) {
            ImageProxyProtocol.HTTP -> Proxy.Type.HTTP
            ImageProxyProtocol.SOCKS5 -> Proxy.Type.SOCKS
        }
        val host = settingsManager.imageGenerationProxyHost.lastValue.trim()
        val port = settingsManager.imageGenerationProxyPort.lastValue.trim().toIntOrNull()
        if (host.isBlank() || port == null || port !in 1..65535) return null

        return okHttpClient.newBuilder()
            .proxy(Proxy(protocol, InetSocketAddress(host, port)))
            .build()
    }

    /**
     * Suspends until the OkHttp call completes and cancels it on coroutine cancellation.
     */
    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                }
            )
            continuation.invokeOnCancellation { runCatching { cancel() } }
        }
    }

    private fun parseGeneratedImage(responseBody: String): CoverImage {
        val response = json.decodeFromString(
            ImageGenerationResponse.serializer(),
            responseBody
        )

        response.error?.message?.let { message ->
            logI(TAG, "parseGeneratedImage: API error: $message.")
            throw IOException("OpenRouter error: $message")
        }

        val image = response.data?.firstOrNull()
        if (image == null) {
            logI(TAG, "parseGeneratedImage: incomplete response, no image was returned by the model.")
            throw IOException("Incomplete response: no image was returned by the model.")
        }

        return decodeImage(image.b64Json, image.mediaType)
    }

    private fun parseChatCompletion(responseBody: String): ChatMessage {
        val response = json.decodeFromString(
            ChatCompletionResponse.serializer(),
            responseBody
        )

        response.error?.message?.let { message ->
            logI(TAG, "parseChatCompletion: API error: $message.")
            throw IOException("OpenRouter error: $message")
        }

        val message = response.choices?.firstOrNull()?.message
        if (message == null) {
            logI(TAG, "parseChatCompletion: incomplete response, no message was returned by the model.")
            throw IOException("Incomplete response: no message was returned by the model.")
        }
        return message
    }

    /**
     * Decodes the returned image bytes. Vector output declared as
     * image/svg+xml is rasterized locally, raster output is decoded by
     * BitmapFactory with dimension validation.
     */
    private fun decodeImage(base64Data: String, mediaType: String?): CoverImage {
        if (base64Data.isBlank()) {
            logI(TAG, "decodeImage: image returned by the model is empty.")
            throw IOException("Image returned by the model is empty.")
        }

        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
        return if (mediaType?.trim().equals(SVG_MEDIA_TYPE, ignoreCase = true)) {
            decodeSvgImage(imageBytes)
        } else {
            decodeRasterImage(imageBytes)
        }
    }

    /**
     * Rasterizes SVG bytes into a bitmap, so that the larger side fits
     * [SVG_TARGET_SIZE] pixels. SVGs without intrinsic dimensions are sized
     * from their aspect ratio (viewBox), or rendered square as a fallback.
     */
    private fun decodeSvgImage(imageBytes: ByteArray): CoverImage {
        val svg = try {
            SVG.getFromString(String(imageBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            logI(TAG, "decodeSvgImage: couldn't parse SVG, ${e.message}.")
            throw IOException("Couldn't parse SVG image returned by the model.")
        }

        val aspect = svg.documentAspectRatio?.takeIf { it > 0f }
        val width = when {
            svg.documentWidth > 0f -> svg.documentWidth
            svg.documentHeight > 0f -> svg.documentHeight * (aspect ?: 1f)
            aspect != null -> SVG_TARGET_SIZE * aspect
            else -> SVG_TARGET_SIZE
        }
        val height = when {
            svg.documentHeight > 0f -> svg.documentHeight
            svg.documentWidth > 0f -> svg.documentWidth / (aspect ?: 1f)
            aspect != null -> SVG_TARGET_SIZE
            else -> SVG_TARGET_SIZE
        }

        val scale = minOf(SVG_TARGET_SIZE / width, SVG_TARGET_SIZE / height)
        val bitmap = Bitmap.createBitmap(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        Canvas(bitmap).apply {
            scale(scale, scale)
            svg.renderToCanvas(this)
        }

        logI(TAG, "decodeSvgImage: rendered ${bitmap.width}x${bitmap.height}.")
        return bitmap
    }

    /**
     * Decodes raster image bytes. Dimensions are first read without
     * allocating; oversized bitmaps are downsampled instead of being
     * allocated at full size.
     */
    private fun decodeRasterImage(imageBytes: ByteArray): CoverImage {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            logI(TAG, "decodeRasterImage: couldn't decode image, bytes: ${imageBytes.size}.")
            throw IOException("Couldn't decode image returned by the model.")
        }

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_RASTER_SIZE ||
            bounds.outHeight / sampleSize > MAX_RASTER_SIZE
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        if (bitmap == null) {
            logI(TAG, "decodeRasterImage: couldn't decode image, bytes: ${imageBytes.size}.")
            throw IOException("Couldn't decode image returned by the model.")
        }
        return bitmap
    }

    private fun buildHttpException(statusCode: Int, responseBody: String): IOException {
        val apiError = runCatching {
            json.decodeFromString(OpenRouterErrorEnvelope.serializer(), responseBody).error
        }.getOrNull()

        val message = apiError?.message?.takeIf { it.isNotBlank() }
            ?: when (statusCode) {
                400 -> "Invalid request parameters. Check the configured model and prompt."
                401 -> "Invalid credentials: API key is invalid or revoked."
                402 -> "Payment required: your account or API key has insufficient credits."
                403 -> "Forbidden: this API key doesn't have permission to use the requested model."
                404 -> "Not found: the configured model doesn't exist. Check it under Settings -> Reader -> AI."
                413 -> "Request payload too large. Try a shorter prompt."
                429 -> "Rate limit exceeded. Please wait a moment before retrying."
                500 -> "Internal server error. Please try again later."
                502 -> "The provider returned an error. Please try again or choose another model."
                524 -> "The request timed out. Please try again later."
                529 -> "The provider is temporarily overloaded. Please try again later."
                else -> "Request failed with status code $statusCode."
            }

        return IOException("OpenRouter (HTTP $statusCode): $message")
    }

    companion object {
        private const val TAG = "OpenRouterClient"

        private const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

        private const val SVG_MEDIA_TYPE = "image/svg+xml"

        /** Maximum rasterization target resolution for SVG output (larger side). */
        private const val SVG_TARGET_SIZE = 1600f

        /** Maximum dimension for raster images before downsampling. */
        private const val MAX_RASTER_SIZE = 1600

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
            isLenient = true
        }
    }
}
