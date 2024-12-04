
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
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

/**
 * Worker class responsible for sending scheduled notifications.
 */
public class NotificationWorker extends Worker {

    private static final String CHANNEL_ID = "default_channel";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve current user
        if (mAuth.getCurrentUser() == null) {
            // User not authenticated; do not proceed
            Log.w("NotificationWorker", "User not authenticated.");
            return Result.success();
        }

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userDocRef = db.collection("User").document(userId);

        try {
            // Fetch user settings from Firestore
            DocumentSnapshot userDoc = userDocRef.get().getResult();

            if (userDoc.exists()) {
                Map<String, Object> notificationSettings = (Map<String, Object>) userDoc.get("notification_settings");
                if (notificationSettings != null) {
                    List<String> selectedProducts = (List<String>) notificationSettings.get("selectedProducts");
                    String frequency = (String) notificationSettings.get("frequency");
                    String timeOfDay = (String) notificationSettings.get("timeOfDay");

                    if (selectedProducts == null || selectedProducts.isEmpty()) {
                        // No products selected; do not send notification
                        Log.i("NotificationWorker", "No products selected for notifications.");
                        return Result.success();
                    }

                    // TODO: Implement logic to determine "new products" based on your app's data
                    // For simplicity, we'll send a generic notification

                    // Construct the notification message
                    String message = "There are new products available from your selected list. Check them out now!";

                    sendNotification("New Products Available!", message);
                } else {
                    // No notification settings found; do not send notification
                    Log.i("NotificationWorker", "No notification settings found for user.");
                }
            } else {
                // User document does not exist; do not send notification
                Log.w("NotificationWorker", "User document does not exist.");
            }
        } catch (Exception e) {
            Log.e("NotificationWorker", "Error fetching user settings", e);
            return Result.retry(); // Retry if there's an exception
        }

        return Result.success();
    }

    /**
     * Sends a notification with the given title and message.
     *
     * @param title   The notification title.
     * @param message The notification message.
     */
    private void sendNotification(String title, String message) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for heads-up notification
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on lockscreen
                .setAutoCancel(true);

        // Add an intent to open the app when the notification is tapped
        Intent intent = new Intent(getApplicationContext(), Price__Comparision.class); // Replace with your target activity
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE for Android 12+
        );
        builder.setContentIntent(pendingIntent);

        // Show the notification
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.i("NotificationWorker", "Notification sent.");
        }
    }
}
