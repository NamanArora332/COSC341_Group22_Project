package com.example.cosc341_group22_project;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Activity for managing notification settings.
 */
public class NotificationSettingsActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final String CHANNEL_ID = "default_channel";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AutoCompleteTextView productSelection;
    private RadioGroup frequencyGroup, timeOfDayGroup;
    private Button saveButton;
    private ProgressBar loadingProgressBar;

    // Dynamic product list fetched from Firestore
    private List<String> allProducts = new ArrayList<>();

    private Set<String> selectedProducts = new HashSet<>();

    private NotificationProductAdapter adapter;

    // Listener for Notifications Collection
    private ListenerRegistration notificationsListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Redirect to login page
            Intent intent = new Intent(NotificationSettingsActivity.this, LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_notification_settings);

        // Initialize UI elements
        productSelection = findViewById(R.id.productSelection);
        frequencyGroup = findViewById(R.id.frequencyGroup);
        timeOfDayGroup = findViewById(R.id.timeOfDayGroup);
        saveButton = findViewById(R.id.saveButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar); // Ensure this exists in your layout

        // Set up the AutoCompleteTextView as a non-editable field to trigger multi-select dialog
        productSelection.setFocusable(false);
        productSelection.setOnClickListener(v -> {
            if (allProducts.isEmpty()) {
                Toast.makeText(this, "No products available to select.", Toast.LENGTH_SHORT).show();
            } else {
                showMultiSelectDialog();
            }
        });

        // Fetch products from Firestore
        fetchProducts();

        // Set save button click listener
        saveButton.setOnClickListener(v -> saveSettings());

        // Check and request notification permission
        checkAndRequestNotificationPermission();
    }

    /**
     * Checks and requests notification permission if necessary.
     */
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, show rationale if needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission Needed")
                            .setMessage("This app requires notification permissions to alert you about updates and offers.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                ActivityCompat.requestPermissions(NotificationSettingsActivity.this,
                                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                        REQUEST_NOTIFICATION_PERMISSION);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                Toast.makeText(this, "Notification permissions denied. You won't receive alerts.", Toast.LENGTH_SHORT).show();
                            })
                            .create()
                            .show();
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_NOTIFICATION_PERMISSION);
                }
            } else {
                // Permission already granted
                Toast.makeText(this, "Notification permissions granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handles the result of the notification permission request.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Notification permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permissions denied. You won't receive alerts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Fetches the list of products from Firestore.
     */
    private void fetchProducts() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        productSelection.setEnabled(false);
        saveButton.setEnabled(false);

        db.collection("Product")
                .get()
                .addOnCompleteListener(task -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String productName = document.getString("name");
                            if (productName != null) {
                                allProducts.add(productName);
                            }
                        }

                        if (allProducts.isEmpty()) {
                            Toast.makeText(this, "No products available at the moment.", Toast.LENGTH_SHORT).show();
                            saveButton.setEnabled(false);
                        } else {
                            productSelection.setEnabled(true);
                            saveButton.setEnabled(true);
                            // Initialize adapter if needed
                            // For example, if you have a RecyclerView outside the dialog
                        }

                        // Load saved settings after products are loaded
                        loadSavedSettings();
                    } else {
                        Toast.makeText(this, "Failed to load products: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load products: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Displays a searchable multi-select dialog for product selection.
     */
    private void showMultiSelectDialog() {
        // Inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_searchable_multi_select, null);

        // Initialize the search EditText and RecyclerView
        EditText searchEditText = dialogView.findViewById(R.id.searchEditText);
        RecyclerView productRecyclerView = dialogView.findViewById(R.id.productRecyclerView);

        // Set up the RecyclerView
        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationProductAdapter(allProducts, selectedProducts);
        productRecyclerView.setAdapter(adapter);

        // Implement search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter the adapter based on user input
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text changes
            }
        });

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Select Products");

        // Set the Positive button (OK)
        builder.setPositiveButton("OK", (dialog, which) -> {
            updateProductSelectionText();
        });

        // Set the Negative button (Cancel)
        builder.setNegativeButton("Cancel", null);

        // Set the Neutral button (Clear All)
        builder.setNeutralButton("Clear All", (dialog, which) -> {
            // Clear all selections
            selectedProducts.clear();
            updateProductSelectionText();
            adapter.filter(searchEditText.getText().toString());
            adapter.notifyDataSetChanged();
        });

        // Show the dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Updates the AutoCompleteTextView with selected products.
     */
    private void updateProductSelectionText() {
        if (selectedProducts.isEmpty()) {
            productSelection.setText("");
        } else {
            // For API levels >= 24, String.join is available
            // For lower APIs, you can use TextUtils.join
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                productSelection.setText(String.join(", ", selectedProducts));
            } else {
                productSelection.setText(android.text.TextUtils.join(", ", selectedProducts));
            }
        }
    }

    /**
     * Saves user settings to Firestore and schedules notifications.
     */
    private void saveSettings() {
        if (selectedProducts.isEmpty()) {
            Toast.makeText(this, "Please select at least one product.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected frequency
        int selectedFrequencyId = frequencyGroup.getCheckedRadioButtonId();
        RadioButton selectedFrequencyButton = findViewById(selectedFrequencyId);
        String frequency = selectedFrequencyButton.getText().toString().toLowerCase(); // daily, weekly, monthly

        // Get selected time of day
        int selectedTimeId = timeOfDayGroup.getCheckedRadioButtonId();
        RadioButton selectedTimeButton = findViewById(selectedTimeId);
        String timeOfDay = selectedTimeButton.getText().toString().toLowerCase(); // morning, noon, evening

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Prepare notification settings data
        Map<String, Object> notificationSettings = new HashMap<>();
        notificationSettings.put("selectedProducts", new ArrayList<>(selectedProducts)); // Convert Set to List
        notificationSettings.put("frequency", frequency);
        notificationSettings.put("timeOfDay", timeOfDay);

        // Save notification settings to Firestore under 'notification_settings' field
        db.collection("User")
                .document(userId)
                .set(Collections.singletonMap("notification_settings", notificationSettings), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Schedule notifications
                    scheduleNotification(frequency, timeOfDay);
                    Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads saved settings from Firestore.
     */
    private void loadSavedSettings() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Optionally, redirect to login/signup
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        DocumentReference userDocRef = db.collection("User").document(userId);

        userDocRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> notificationSettings = (Map<String, Object>) document.get("notification_settings");
                            if (notificationSettings != null) {
                                List<String> products = (List<String>) notificationSettings.get("selectedProducts");
                                String frequency = (String) notificationSettings.get("frequency");
                                String timeOfDay = (String) notificationSettings.get("timeOfDay");

                                // Update selectedProducts set
                                if (products != null) {
                                    selectedProducts = new HashSet<>(products);
                                    updateProductSelectionText();
                                }

                                // Set frequency radio button
                                if (frequency != null) {
                                    switch (frequency) {
                                        case "daily":
                                            frequencyGroup.check(R.id.radio_daily);
                                            break;
                                        case "weekly":
                                            frequencyGroup.check(R.id.radio_weekly);
                                            break;
                                        case "monthly":
                                            frequencyGroup.check(R.id.radio_monthly);
                                            break;
                                        default:
                                            frequencyGroup.check(R.id.radio_daily);
                                            break;
                                    }
                                }

                                // Set time of day radio button
                                if (timeOfDay != null) {
                                    switch (timeOfDay) {
                                        case "morning":
                                            timeOfDayGroup.check(R.id.radio_morning);
                                            break;
                                        case "noon":
                                            timeOfDayGroup.check(R.id.radio_noon);
                                            break;
                                        case "evening":
                                            timeOfDayGroup.check(R.id.radio_evening);
                                            break;
                                        default:
                                            timeOfDayGroup.check(R.id.radio_morning);
                                            break;
                                    }
                                }
                            } else {
                                // No notification settings found
                                Toast.makeText(this, "No notification settings found. Please configure your preferences.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Document does not exist; possibly first-time user
                            Toast.makeText(this, "No user data found. Please register again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Task failed
                        Toast.makeText(this, "Failed to load settings: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Schedules a periodic notification based on frequency and time of day.
     *
     * @param frequency  The frequency of the notification (daily, weekly, monthly).
     * @param timeOfDay  The preferred time of day (morning, noon, evening).
     */
    private void scheduleNotification(String frequency, String timeOfDay) {
        // Cancel any existing work
        WorkManager.getInstance(this).cancelUniqueWork("notification_work");

        // Determine the interval based on frequency
        long interval = 1; // default to daily
        TimeUnit timeUnit = TimeUnit.DAYS;

        switch (frequency) {
            case "daily":
                interval = 1;
                timeUnit = TimeUnit.DAYS;
                break;
            case "weekly":
                interval = 7;
                timeUnit = TimeUnit.DAYS;
                break;
            case "monthly":
                interval = 30;
                timeUnit = TimeUnit.DAYS;
                break;
        }

        // Calculate initial delay based on preferred time
        long initialDelay = getInitialDelay(timeOfDay);

        // Create a periodic work request
        PeriodicWorkRequest notificationWork = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                interval,
                timeUnit
        )
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .addTag("notification_work")
                .build();

        // Enqueue the work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "notification_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                notificationWork
        );
    }

    /**
     * Calculates the initial delay in minutes based on the preferred time of day.
     *
     * @param timeOfDay The preferred time of day.
     * @return The delay in minutes.
     */
    private long getInitialDelay(String timeOfDay) {
        // Map timeOfDay to specific times
        // Morning: 8:00 AM
        // Noon: 12:00 PM
        // Evening: 6:00 PM

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour;
        switch (timeOfDay) {
            case "morning":
                hour = 8;
                break;
            case "noon":
                hour = 12;
                break;
            case "evening":
                hour = 18;
                break;
            default:
                hour = 8;
                break;
        }
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        long currentTime = System.currentTimeMillis();
        long scheduledTime = calendar.getTimeInMillis();

        if (scheduledTime < currentTime) {
            // If the scheduled time is before current time, set for next day
            scheduledTime += TimeUnit.DAYS.toMillis(1);
        }

        long delay = (scheduledTime - currentTime) / 60000; // convert to minutes
        return delay;
    }

    /**
     * Sets up a real-time listener for the user's notifications.
     * This listens to the Notifications collection and displays notifications as they arrive.
     */
    private void setupNotificationsListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w("NotificationSettings", "User not authenticated.");
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("User").document(userId);

        notificationsListener = db.collection("Notifications")
                .whereEqualTo("user_id", userRef)
                .whereEqualTo("read", false)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("NotificationSettings", "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                handleNewNotification(dc.getDocument());
                            }
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupNotificationsListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }

    /**
     * Handles a newly added notification document.
     *
     * @param document The notification document.
     */
    private void handleNewNotification(DocumentSnapshot document) {
        String message = document.getString("message");
        com.google.firebase.Timestamp timestamp = document.getTimestamp("timestamp");
        String notificationId = document.getId();

        if (message != null && timestamp != null) {
            // Display the notification
            displayNotification("New Alert", message);

            // Mark the notification as read
            markNotificationAsRead(notificationId);
        }
    }

    /**
     * Displays a notification with the given title and message.
     *
     * @param title   The notification title.
     * @param message The notification message.
     */
    private void displayNotification(String title, String message) {
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
            Log.i("NotificationSettings", "Notification sent.");
        }
    }

    /**
     * Marks a specific notification as read in Firestore.
     *
     * @param notificationId The ID of the notification to mark as read.
     */
    private void markNotificationAsRead(String notificationId) {
        db.collection("Notifications").document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d("NotificationSettings", "Notification marked as read: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.w("NotificationSettings", "Error marking notification as read", e);
                });
    }
}
