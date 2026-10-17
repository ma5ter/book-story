/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.use_case.attachment.DeleteImageAttachmentUseCase
import ua.acclorite.book_story.domain.use_case.attachment.GenerateImageForAttachmentUseCase
import ua.acclorite.book_story.domain.use_case.attachment.GenerateStyleCollageUseCase
import ua.acclorite.book_story.data.settings.SettingsManager
import javax.inject.Inject

/**
 * Foreground service running the AI image generation step by step.
 * Shows a persistent progress notification with the current step
 * ("Preparing context…", "Building style collage…", "Distilling scene…",
 * "Rendering illustration…").
 * On a failed step the notification shows the error together with a Retry
 * action, which restarts the service from the failed step; intermediate
 * results travel in the intent extras, so the retry also works after
 * the process death. A Cancel action drops the PENDING attachment of the
 * failed generation instead, so it is not retried.
 */
@AndroidEntryPoint
class ImageGenerationService : Service() {

    @Inject
    lateinit var generateImageForAttachmentUseCase: GenerateImageForAttachmentUseCase

    @Inject
    lateinit var deleteImageAttachmentUseCase: DeleteImageAttachmentUseCase

    @Inject
    lateinit var generateStyleCollageUseCase: GenerateStyleCollageUseCase

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var imageGenerationManager: ImageGenerationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var generationJob: Job? = null

    // Data of the current run, retained so a retry (including onTimeout)
    // can restart from the failed step.
    private var currentBookId: Int = -1
    private var currentAttachmentId: Int = -1
    private var currentSelectedText: String = ""
    private var currentStep: ImageGenerationStep = ImageGenerationStep.PREPARE
    private var preparedInput: String? = null
    private var sceneDescription: String? = null
    private var styleReferencePath: String? = null

    // Download progress dots (1..3, cycling) appended to the step label
    // in the progress notification.
    private val downloadDots = ProgressDots()
    private var dots: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bookId = intent?.getIntExtra(EXTRA_BOOK_ID, -1) ?: -1
        val attachmentId = intent?.getIntExtra(EXTRA_ATTACHMENT_ID, -1) ?: -1
        val selectedText = intent?.getStringExtra(EXTRA_SELECTED_TEXT).orEmpty()
        val startStep = intent?.getStringExtra(EXTRA_START_STEP)
            ?.let { name -> ImageGenerationStep.entries.firstOrNull { it.name == name } }
            ?: ImageGenerationStep.PREPARE
        val input = intent?.getStringExtra(EXTRA_PREPARED_INPUT)?.takeIf { it.isNotBlank() }
        val scene = intent?.getStringExtra(EXTRA_SCENE_DESCRIPTION)?.takeIf { it.isNotBlank() }
        val styleReference =
            intent?.getStringExtra(EXTRA_STYLE_REFERENCE)?.takeIf { it.isNotBlank() }
        val cancelRequested = intent?.getBooleanExtra(EXTRA_CANCEL, false) == true

        if (imageGenerationManager.state.value.isRunning) {
            // A second start while a generation is in progress: keep serving
            // the running one and re-post its current notification, which
            // also satisfies the startForeground contract.
            promoteToForeground(imageGenerationManager.state.value.step)
            return START_NOT_STICKY
        }

        if (cancelRequested && attachmentId != -1) {
            // A cancelled failed generation: satisfy the startForeground
            // contract, drop the PENDING attachment and stop.
            promoteToForeground(step = null)
            cancelGeneration(attachmentId, startId)
            return START_NOT_STICKY
        }

        val invalid = bookId == -1 ||
                attachmentId == -1 ||
                selectedText.isBlank() ||
                (startStep.ordinal > ImageGenerationStep.PREPARE.ordinal && input == null) ||
                (startStep.ordinal > ImageGenerationStep.DISTILL.ordinal && scene == null)
        if (invalid) {
            promoteToForeground(step = null)
            stopSelf()
            return START_NOT_STICKY
        }

        currentBookId = bookId
        currentAttachmentId = attachmentId
        currentSelectedText = selectedText
        currentStep = startStep
        preparedInput = if (startStep.ordinal > ImageGenerationStep.PREPARE.ordinal) input else null
        sceneDescription =
            if (startStep.ordinal <= ImageGenerationStep.DISTILL.ordinal) null else scene
        styleReferencePath = styleReference

        imageGenerationManager.start(bookId, attachmentId)
        promoteToForeground(startStep)

        generationJob = serviceScope.launch {
            runGeneration(startId, startStep)
        }
        return START_NOT_STICKY
    }

    /**
     * Called by the system when the dataSync foreground service reaches its
     * time limit (Android 15+). The default implementation crashes the app,
     * so the running generation is cancelled and reported as failed with
     * a retry action instead.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        generationJob?.cancel()
        if (imageGenerationManager.state.value.isRunning) {
            failGeneration(getString(R.string.image_stopped_by_system), startId)
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        generationJob?.cancel()
        serviceScope.cancel()
        // The run was interrupted without reporting, mark it as failed
        // so the manager's running state cannot leak.
        if (imageGenerationManager.state.value.isRunning) {
            imageGenerationManager.finish(
                ImageGenerationResult.Failure(
                    attachmentId = currentAttachmentId,
                    step = currentStep,
                    message = getString(R.string.image_stopped_by_system)
                )
            )
        }
        super.onDestroy()
    }

    /**
     * Runs the remaining pipeline steps one by one, updating the manager
     * and the progress notification per step. On success the result is
     * published and the service stops. On failure the error is shown in
     * the notification together with a retry action restarting the
     * failed step.
     */
    private suspend fun runGeneration(startId: Int, startStep: ImageGenerationStep) {
        try {
            for (step in ImageGenerationStep.entries.drop(startStep.ordinal)) {
                currentStep = step
                imageGenerationManager.updateStep(step)
                dots = ""
                notifyStep(step)

                when (step) {
                    ImageGenerationStep.PREPARE ->
                        preparedInput = generateImageForAttachmentUseCase
                            .prepare(currentAttachmentId, currentSelectedText)
                            .getOrThrow()

                    ImageGenerationStep.COLLAGE ->
                        // Optional style reference, skipped entirely when disabled
                        // in settings; null when the book has no illustrations yet,
                        // the pipeline continues without it.
                        if (settingsManager.imageGenerationStyleReferenceEnabled.lastValue) {
                            styleReferencePath = generateStyleCollageUseCase(currentBookId).getOrNull()
                        }

                    ImageGenerationStep.DISTILL ->
                        sceneDescription = generateImageForAttachmentUseCase
                            .distill(requireNotNull(preparedInput)) {
                                onDownloadProgress(step)
                            }
                            .getOrThrow()

                    ImageGenerationStep.GENERATE -> {
                        val (attachment, bitmap) = generateImageForAttachmentUseCase
                            .complete(
                                currentAttachmentId,
                                requireNotNull(sceneDescription),
                                styleReferencePath
                            ) {
                                onDownloadProgress(step)
                            }
                            .getOrThrow()
                        imageGenerationManager.finish(
                            ImageGenerationResult.Success(
                                attachmentId = currentAttachmentId,
                                attachment = attachment,
                                image = bitmap
                            )
                        )
                        ServiceCompat.stopForeground(
                            this,
                            ServiceCompat.STOP_FOREGROUND_REMOVE
                        )
                        stopSelf(startId)
                        return
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failGeneration(e.message ?: "Image generation failed.", startId)
        }
    }

    /**
     * Reports the failure of the current step, shows an error notification
     * with a retry action restarting the failed step, and detaches the
     * notification so it remains after the service stops.
     */
    private fun failGeneration(message: String, startId: Int) {
        val failedStep = currentStep
        imageGenerationManager.finish(
            ImageGenerationResult.Failure(currentAttachmentId, failedStep, message)
        )
        showErrorNotification(message, failedStep)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    /**
     * Handles the cancel action of the error notification: drops the
     * PENDING attachment row of the failed generation (so it is not retried
     * on the next load) and notifies the reader to remove its pending
     * placeholder. [onStartCommand] has already promoted the service to
     * foreground to satisfy the start contract.
     */
    private fun cancelGeneration(attachmentId: Int, startId: Int) {
        serviceScope.launch {
            deleteImageAttachmentUseCase(attachmentId)
            imageGenerationManager.cancel(attachmentId)
            ServiceCompat.stopForeground(
                this@ImageGenerationService,
                ServiceCompat.STOP_FOREGROUND_REMOVE
            )
            stopSelf(startId)
        }
    }

    /**
     * Promotes the service to foreground with a progress notification
     * of the given [step].
     */
    private fun promoteToForeground(step: ImageGenerationStep?) {
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildProgressNotification(step),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else 0
        )
    }

    /**
     * Updates the progress notification with the current step,
     * only when notifications are enabled for the app.
     */
    private fun notifyStep(step: ImageGenerationStep) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, buildProgressNotification(step))
        }
    }

    /**
     * Advances the download dots cycle and refreshes the progress
     * notification with the dots appended to the current step label.
     */
    private fun onDownloadProgress(step: ImageGenerationStep) {
        dots = downloadDots.next()
        notifyStep(step)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.image_generating),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun buildProgressNotification(step: ImageGenerationStep?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(getString(R.string.image_generating))
            .setContentText(stepLabel(step).removeSuffix("…") + dots)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun showErrorNotification(message: String, failedStep: ImageGenerationStep) {
        val manager = getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return

        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(getString(R.string.image_generation_failed))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setOngoing(false)
                .addAction(
                    android.R.drawable.ic_media_play,
                    getString(R.string.retry),
                    buildRetryPendingIntent(failedStep)
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.cancel),
                    buildCancelPendingIntent()
                )
                .build()
        )
    }

    /**
     * Builds a retry action restarting the service from [failedStep],
     * carrying the intermediate results of the already completed steps.
     */
    private fun buildRetryPendingIntent(failedStep: ImageGenerationStep): PendingIntent {
        val intent = buildIntent(
            context = this,
            bookId = currentBookId,
            attachmentId = currentAttachmentId,
            selectedText = currentSelectedText,
            startStep = failedStep,
            preparedInput = preparedInput?.takeIf {
                failedStep.ordinal > ImageGenerationStep.PREPARE.ordinal
            },
            sceneDescription = sceneDescription?.takeIf {
                failedStep.ordinal > ImageGenerationStep.DISTILL.ordinal
            },
            styleReferencePath = styleReferencePath
        )
        return PendingIntent.getForegroundService(
            this,
            currentAttachmentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Builds a cancel action starting the service only to drop the PENDING
     * attachment of the failed generation, so it is not retried, and to
     * remove the pending placeholder in the reader.
     */
    private fun buildCancelPendingIntent(): PendingIntent {
        val intent = Intent(this, ImageGenerationService::class.java)
            .putExtra(EXTRA_CANCEL, true)
            .putExtra(EXTRA_ATTACHMENT_ID, currentAttachmentId)
        return PendingIntent.getForegroundService(
            this,
            CANCEL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stepLabel(step: ImageGenerationStep?): String = when (step) {
        ImageGenerationStep.PREPARE -> getString(R.string.image_step_prepare)
        ImageGenerationStep.COLLAGE -> getString(R.string.image_step_collage)
        ImageGenerationStep.DISTILL -> getString(R.string.image_step_distill)
        ImageGenerationStep.GENERATE -> getString(R.string.image_step_generate)
        null -> getString(R.string.image_generating)
    }

    companion object {
        private const val CHANNEL_ID = "image_generation"
        private const val NOTIFICATION_ID = 1002

        /** Per-extra character cap; three capped extras stay well under
         *  the ~1 MB shared binder transaction limit. */
        private const val MAX_EXTRA_CHARS = 100_000

        const val EXTRA_BOOK_ID = "image_book_id"
        const val EXTRA_ATTACHMENT_ID = "image_attachment_id"
        const val EXTRA_SELECTED_TEXT = "image_selected_text"
        const val EXTRA_START_STEP = "image_start_step"
        const val EXTRA_CANCEL = "image_cancel"
        private const val EXTRA_PREPARED_INPUT = "image_prepared_input"
        private const val EXTRA_SCENE_DESCRIPTION = "image_scene_description"
        private const val EXTRA_STYLE_REFERENCE = "image_style_reference"

        /** Distinct request code of the cancel action's PendingIntent. */
        private const val CANCEL_REQUEST_CODE = 1003

        private fun buildIntent(
            context: Context,
            bookId: Int,
            attachmentId: Int,
            selectedText: String,
            startStep: ImageGenerationStep,
            preparedInput: String?,
            sceneDescription: String?,
            styleReferencePath: String?
        ): Intent {
            return Intent(context, ImageGenerationService::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_ATTACHMENT_ID, attachmentId)
                .putExtra(EXTRA_SELECTED_TEXT, selectedText.sanitizeExtra())
                .putExtra(EXTRA_START_STEP, startStep.name)
                .putExtra(EXTRA_PREPARED_INPUT, preparedInput?.sanitizeExtra())
                .putExtra(EXTRA_SCENE_DESCRIPTION, sceneDescription?.sanitizeExtra())
                .putExtra(EXTRA_STYLE_REFERENCE, styleReferencePath)
        }

        /**
         * Caps a string intent extra, so a large chapter context, characters
         * catalog or scene description cannot overflow the shared binder
         * transaction buffer (~1 MB) when the service is started or the
         * retry PendingIntent is created. The tail beyond the cap is dropped;
         * the cap is only reached in pathological configurations, so this
         * loses non-essential context rather than crashing the transaction.
         */
        private fun String.sanitizeExtra(): String =
            if (length <= MAX_EXTRA_CHARS) this else take(MAX_EXTRA_CHARS)

        /**
         * Starts the service as a foreground service generating an image
         * for the given attachment.
         */
        fun start(context: Context, bookId: Int, attachmentId: Int, selectedText: String) {
            ContextCompat.startForegroundService(
                context,
                buildIntent(
                    context = context,
                    bookId = bookId,
                    attachmentId = attachmentId,
                    selectedText = selectedText,
                    startStep = ImageGenerationStep.PREPARE,
                    preparedInput = null,
                sceneDescription = null,
                styleReferencePath = null
                )
            )
        }
    }
}
