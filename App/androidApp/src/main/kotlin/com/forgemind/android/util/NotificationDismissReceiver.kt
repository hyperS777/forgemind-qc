package com.forgemind.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.forgemind.android.model.AcknowledgeRequest
import com.forgemind.android.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Keeps a phone-level dismissal in sync with ForgeMind's pending-alert list. */
class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val androidNotificationId = intent.getIntExtra(EXTRA_ANDROID_NOTIFICATION_ID, -1)
        val backendNotificationId = intent.getStringExtra(EXTRA_BACKEND_NOTIFICATION_ID)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (androidNotificationId != -1) {
                    NotificationHelper.cancel(appContext, androidNotificationId)
                }
                if (!backendNotificationId.isNullOrBlank()) {
                    RetrofitClient.initialize(appContext)
                    RetrofitClient.api.acknowledgeNotification(
                        AcknowledgeRequest(backendNotificationId)
                    )
                }
            } catch (_: Exception) {
                // A later in-app dismissal can retry if the phone is offline.
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.forgemind.android.DISMISS_ALERT"
        const val EXTRA_ANDROID_NOTIFICATION_ID = "android_notification_id"
        const val EXTRA_BACKEND_NOTIFICATION_ID = "backend_notification_id"
    }
}
