package de.hbch.traewelling.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.jcloquell.androidsecurestorage.SecureStorage
import de.hbch.traewelling.R
import de.hbch.traewelling.api.getGson
import de.hbch.traewelling.api.models.notifications.Notification
import de.hbch.traewelling.shared.SharedValues
import de.hbch.traewelling.theme.PolylineColor
import org.unifiedpush.android.connector.MessagingReceiver

class PushNotificationReceiver : MessagingReceiver() {
    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        Log.d("PushReceiver", "Message received!")
        val json = String(message)
        if (json.isNotBlank()) {
            val notification = getGson().fromJson(json, Notification::class.java)
            notification?.let {
                pushNotification(context, it)
            }
        }
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        Log.d("PushReceiver", "Endpoint $endpoint on $instance received!")
        val secureStorage = SecureStorage(context)
        secureStorage.storeObject(SharedValues.SS_UP_ENDPOINT, endpoint)
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        Log.d("PushReceiver", "Registration with $instance failed!")
    }

    override fun onUnregistered(context: Context, instance: String) {
        Log.d("PushReceiver", "Unregistered from $instance")
    }

    private fun pushNotification(context: Context, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appHasPermission = context.packageManager.checkPermission(
                android.Manifest.permission.POST_NOTIFICATIONS,
                context.packageName
            )
            if (appHasPermission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notificationManager
            = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        if (notificationManager != null) {
            val title = notification.safeType.getHeadline(context, notification)
            val body = notification.safeType.getBody(context, notification)
            val icon = notification.safeType.icon
            val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getColor(R.color.material_dynamic_primary60)
            } else {
                PolylineColor.toArgb()
            }

            val intent = notification.safeType.getIntent(context, notification)

            var pushNotification = android.app.Notification.Builder(context, notification.safeType.channel.name)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setLargeIcon(Icon.createWithResource(context, icon).setTint(color))
                .setContentTitle(title)
                .setContentText(body)
                .setWhen(notification.safeCreatedAt.toInstant().toEpochMilli())
                .setShowWhen(true)
                .setCategory(android.app.Notification.CATEGORY_SOCIAL)
                .setAutoCancel(true)

            if (intent != null) {
                pushNotification = pushNotification.setContentIntent(
                    PendingIntent.getActivity(
                        context, 0, intent,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            notificationManager.notify(
                notification.safeCreatedAt.toEpochSecond().toInt(),
                pushNotification.build()
            )
        }
    }
}
