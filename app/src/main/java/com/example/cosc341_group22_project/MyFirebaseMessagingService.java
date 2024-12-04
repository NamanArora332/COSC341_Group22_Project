// File: MyFirebaseMessagingService.java
package com.example.cosc341_group22_project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "default_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Handle FCM messages here.
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();
            sendNotification(title, message);
        }
    }

    /**
     * Sends a notification with the given title and message.
     *
     * @param title   The notification title.
     * @param message The notification message.
     */
    private void sendNotification(String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create NotificationChannel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "Channel for default notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH; // High importance for prominence
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Visible on lockscreen
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for heads-up notification
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on lockscreen
                .setAutoCancel(true);

        // Add an intent to open the app when the notification is tapped
        Intent intent = new Intent(this, Price__Comparision.class); // Replace with your target activity
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE for Android 12+
        );
        builder.setContentIntent(pendingIntent);

        // Show the notification
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.i(TAG, "FCM Notification sent.");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Send the token to your backend for associating with the user
        Log.d(TAG, "Refreshed token: " + token);
        // TODO: Implement token update logic
    }
}
