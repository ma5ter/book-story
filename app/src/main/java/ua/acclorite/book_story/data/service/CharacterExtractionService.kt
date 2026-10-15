/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.use_case.character.GenerateCharactersUseCase
import java.io.IOException
import javax.inject.Inject

/**
 * Foreground service running the long-running characters extraction.
 * Shows a persistent progress notification ("Processing ##%") while
 * analyzing the book text. On a failure the notification shows the error
 * together with a Retry action, which restarts the whole extraction
 * (the book and the analysis prompt are retained by the service, so the
 * retry also works after the process death), and a Cancel action, which
 * dismisses the error notification. The service stops itself once the
 * extraction finishes or fails.
 */
@AndroidEntryPoint
class CharacterExtractionService : Service() {

    @Inject
    lateinit var generateCharactersUseCase: GenerateCharactersUseCase

    @Inject
    lateinit var characterExtractionManager: CharacterExtractionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var extractionJob: Job? = null

    // Data of the current run, retained so a retry (including onTimeout)
    // can restart the extraction.
    private var currentBookId: Int = -1
    private var currentSystemPrompt: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bookId = intent?.getIntExtra(EXTRA_BOOK_ID, -1) ?: -1
        val systemPrompt = intent?.getStringExtra(EXTRA_SYSTEM_PROMPT).orEmpty()
        val cancelRequested = intent?.getBooleanExtra(EXTRA_CANCEL, false) == true

        if (characterExtractionManager.state.value.isRunning) {
            // A second start while an extraction is in progress: keep serving
            // the running one and re-post its current notification, which
            // also satisfies the startForeground contract.
            promoteToForeground(progress = characterExtractionManager.state.value.progress)
            return START_NOT_STICKY
        }

        if (cancelRequested && bookId != -1) {
            // A cancelled failed extraction: satisfy the startForeground
            // contract, dismiss the error notification and stop.
            promoteToForeground(progress = null)
            cancelExtraction(startId)
            return START_NOT_STICKY
        }

        // startForeground must always be called first, otherwise the system
        // crashes the app with "did not then call startForeground".
        if (bookId == -1 || systemPrompt.isBlank()) {
            promoteToForeground(progress = null)
            stopSelf()
            return START_NOT_STICKY
        }

        currentBookId = bookId
        currentSystemPrompt = systemPrompt

        characterExtractionManager.start(bookId)
        promoteToForeground(progress = null)

        extractionJob = serviceScope.launch {
            val result = generateCharactersUseCase(
                bookId = bookId,
                systemPrompt = systemPrompt,
                onProgress = { progress ->
                    characterExtractionManager.updateProgress(bookId, progress)
                    notifyProgress(progress)
                }
            )
            result.fold(
                onSuccess = {
                    characterExtractionManager.finish(bookId, result)
                    ServiceCompat.stopForeground(
                        this@CharacterExtractionService,
                        ServiceCompat.STOP_FOREGROUND_REMOVE
                    )
                    stopSelf(startId)
                },
                onFailure = {
                    failExtraction(
                        it.message ?: "Couldn't generate characters catalog.",
                        startId
                    )
                }
            )
        }
        return START_NOT_STICKY
    }

    /**
     * Called by the system when the dataSync foreground service reaches its
     * time limit (Android 15+). The default implementation crashes the app,
     * so the running extraction is cancelled and reported as failed with
     * a retry action instead.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        extractionJob?.cancel()
        if (characterExtractionManager.state.value.isRunning) {
            failExtraction(getString(R.string.characters_stopped_by_system), startId)
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        extractionJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Reports the failure of the extraction, shows an error notification
     * with retry and cancel actions, and detaches the notification so it
     * remains after the service stops.
     */
    private fun failExtraction(message: String, startId: Int) {
        characterExtractionManager.finish(
            currentBookId,
            Result.failure(IOException(message))
        )
        showErrorNotification(message)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    /**
     * Handles the cancel action of the error notification. Nothing is
     * persisted during the extraction (the catalog is written only on
     * success), so cancellation just dismisses the error notification.
     * [onStartCommand] has already promoted the service to foreground to
     * satisfy the start contract.
     */
    private fun cancelExtraction(startId: Int) {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    /**
     * Promotes the service to foreground with a progress notification
     * of the given [progress].
     */
    private fun promoteToForeground(progress: Int?) {
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(progress),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else 0
        )
    }

    /**
     * Updates the progress notification, only when notifications
     * are enabled for the app.
     */
    private fun notifyProgress(progress: Int?) {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, buildNotification(progress))
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.characters_generating),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    /**
     * Builds the progress notification: indeterminate while the progress
     * cannot be estimated, otherwise a determinate "Processing ##%" bar.
     */
    private fun buildNotification(progress: Int?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(getString(R.string.characters_generating))
            .setContentText(
                if (progress == null) getString(R.string.characters_indeterminate)
                else getString(R.string.characters_processing, progress)
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress ?: 0, progress == null)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun showErrorNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return

        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(getString(R.string.characters_failed))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setOngoing(false)
                .addAction(
                    android.R.drawable.ic_media_play,
                    getString(R.string.retry),
                    buildRetryPendingIntent()
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
     * Builds a retry action restarting the whole extraction with the
     * retained book and analysis prompt.
     */
    private fun buildRetryPendingIntent(): PendingIntent {
        val intent = Intent(this, CharacterExtractionService::class.java)
            .putExtra(EXTRA_BOOK_ID, currentBookId)
            .putExtra(EXTRA_SYSTEM_PROMPT, currentSystemPrompt)
        return PendingIntent.getForegroundService(
            this,
            currentBookId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Builds a cancel action starting the service only to dismiss the
     * error notification of the failed extraction.
     */
    private fun buildCancelPendingIntent(): PendingIntent {
        val intent = Intent(this, CharacterExtractionService::class.java)
            .putExtra(EXTRA_CANCEL, true)
            .putExtra(EXTRA_BOOK_ID, currentBookId)
        return PendingIntent.getForegroundService(
            this,
            CANCEL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "character_extraction"
        private const val NOTIFICATION_ID = 1001

        /** Distinct request code of the cancel action's PendingIntent. */
        private const val CANCEL_REQUEST_CODE = 1004

        const val EXTRA_BOOK_ID = "book_id"
        const val EXTRA_SYSTEM_PROMPT = "system_prompt"
        const val EXTRA_CANCEL = "characters_cancel"

        /**
         * Starts the service as a foreground service for the given book.
         */
        fun start(context: Context, bookId: Int, systemPrompt: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CharacterExtractionService::class.java)
                    .putExtra(EXTRA_BOOK_ID, bookId)
                    .putExtra(EXTRA_SYSTEM_PROMPT, systemPrompt)
            )
        }
    }
}
