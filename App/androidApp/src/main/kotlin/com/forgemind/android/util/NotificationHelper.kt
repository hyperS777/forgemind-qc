package com.forgemind.android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.forgemind.android.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "forgemind_alerts"
    private const val CHANNEL_NAME = "ForgeMind Alerts"

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Critical anomaly and system alerts"
            nm.createNotificationChannel(channel)
        }
    }

    fun notificationIdFor(backendNotificationId: String): Int = backendNotificationId.hashCode()

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun showAnomalyNotification(
        context: Context,
        title: String,
        text: String,
        backendNotificationId: String? = null,
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("NotificationHelper", "Permission POST_NOTIFICATIONS not granted")
                return
            }
        }
        ensureChannel(context)
        android.util.Log.i("NotificationHelper", "showAnomalyNotification: $title | $text")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = backendNotificationId?.let(::notificationIdFor)
            ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = NotificationDismissReceiver.ACTION_DISMISS
            putExtra(NotificationDismissReceiver.EXTRA_ANDROID_NOTIFICATION_ID, notificationId)
            putExtra(NotificationDismissReceiver.EXTRA_BACKEND_NOTIFICATION_ID, backendNotificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

}
