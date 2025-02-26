package com.yasinyilmaz.appqr2.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yasinyilmaz.appqr2.MainActivity
import com.yasinyilmaz.appqr2.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Bildirim mesajı alındığında gösterilecek
        remoteMessage.notification?.let {
            showNotification(it.body, "alarm_notification")
        }
    }

    private fun showNotification(messageBody: String?, channelId: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = 1

        // **Bildirim tıklandığında uygulamayı açan Intent**
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Yeni Bildirim")
            .setContentText(messageBody)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // **Android 8.0 ve üstü için kanal oluşturma**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = if (channelId == "alarm_notification") {
                val alarmSoundUri = Uri.parse("android.resource://$packageName/raw/alarm")

                NotificationChannel(
                    channelId,
                    "Alarm Notification",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(
                        alarmSoundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            } else {
                NotificationChannel(
                    channelId,
                    "Default",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            }

            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
