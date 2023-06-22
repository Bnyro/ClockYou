package com.bnyro.clock.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bnyro.clock.R
import com.bnyro.clock.obj.WatchState
import com.bnyro.clock.util.NotificationHelper

class TimerService : ScheduleService() {
    override val notificationId = 2
    private val finishedNotificationId = 3

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        NotificationManagerCompat.from(this).cancel(finishedNotificationId)
        currentPosition = intent.getIntExtra(START_TIME_KEY, -1)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getNotification() = NotificationCompat.Builder(
        this,
        NotificationHelper.TIMER_CHANNEL
    )
        .setContentTitle(getText(R.string.timer))
        .setUsesChronometer(state == WatchState.RUNNING)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(true)
            } else {
                setContentText(DateUtils.formatElapsedTime((currentPosition / 1000).toLong()))
            }
        }
        .setWhen(System.currentTimeMillis() + currentPosition)
        .addAction(getAction(R.string.stop, ACTION_STOP, 4))
        .addAction(pauseResumeAction())
        .setSmallIcon(R.drawable.ic_notification)
        .build()

    override fun updateState() {
        if (state == WatchState.RUNNING) {
            currentPosition -= updateDelay
            invokeChangeListener()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                updateNotification()
            }
        }

        if (currentPosition <= 0) {
            state = WatchState.IDLE
            invokeChangeListener()
            showFinishedNotification()
            onDestroy()
        }
    }

    private fun showFinishedNotification() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = NotificationCompat.Builder(
                this,
                NotificationHelper.TIMER_FINISHED_CHANNEL
            )
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.timer_finished))
                .build()

            NotificationManagerCompat.from(this)
                .notify(finishedNotificationId, notification)
        }
    }

    companion object {
        const val START_TIME_KEY = "start_time"
    }
}
