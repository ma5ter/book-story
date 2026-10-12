/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
 * analyzing the book text and stops itself once the extraction
 * finishes or fails.
 */
@AndroidEntryPoint
class CharacterExtractionService : Service() {

    @Inject
    lateinit var generateCharactersUseCase: GenerateCharactersUseCase

    @Inject
    lateinit var characterExtractionManager: CharacterExtractionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var extractionJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bookId = intent?.getIntExtra(EXTRA_BOOK_ID, -1) ?: -1
        val systemPrompt = intent?.getStringExtra(EXTRA_SYSTEM_PROMPT).orEmpty()

        // startForeground must always be called first, otherwise the system
        // crashes the app with "did not then call startForeground".
        startForegroundNotification()
        if (
            bookId == -1 ||
            systemPrompt.isBlank() ||
            characterExtractionManager.state.value.isRunning
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        characterExtractionManager.start(bookId)
        notifyProgress(progress = null)

        extractionJob = serviceScope.launch {
            val result = generateCharactersUseCase(
                bookId = bookId,
                systemPrompt = systemPrompt,
                onProgress = { progress ->
                    characterExtractionManager.updateProgress(bookId, progress)
                    notifyProgress(progress)
                }
            )
            characterExtractionManager.finish(bookId, result)
            ServiceCompat.stopForeground(
                this@CharacterExtractionService,
                ServiceCompat.STOP_FOREGROUND_REMOVE
            )
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /**
     * Called by the system when the dataSync foreground service reaches its
     * time limit (Android 15+). The default implementation crashes the app,
     * so the running extraction is cancelled and reported as failed instead.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        extractionJob?.cancel()
        val bookId = characterExtractionManager.state.value.bookId
        if (bookId != null) {
            characterExtractionManager.finish(
                bookId,
                Result.failure(IOException("Character extraction was stopped by the system."))
            )
        }
        ServiceCompat.stopForeground(
            this,
            ServiceCompat.STOP_FOREGROUND_REMOVE
        )
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Promotes the service to foreground with an indeterminate
     * progress notification.
     */
    private fun startForegroundNotification() {
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(progress = null),
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

    companion object {
        private const val CHANNEL_ID = "character_extraction"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_BOOK_ID = "book_id"
        const val EXTRA_SYSTEM_PROMPT = "system_prompt"

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
