package com.example.cosc341_group22_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreManager_Register extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText fullNameEditText, emailEditText, passwordEditText;
    private Spinner storeSpinner;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_store_manager_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        storeSpinner = findViewById(R.id.storeSpinner);
        registerButton = findViewById(R.id.registerButton);

        TextView goBackLogin = findViewById(R.id.instructionText);

        // Example list of stores
        List<String> stores = Arrays.asList("Walmart", "Costco", "Real Canadian Superstore", "SaveOnFoods");

        // Adapter for Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stores);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storeSpinner.setAdapter(adapter);

        registerButton.setOnClickListener(v -> checkAndRegisterStoreManager());

        goBackLogin.setOnClickListener(v -> {
            Intent intent = new Intent(StoreManager_Register.this, LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
            startActivity(intent);
            finish();
        });
    }

    private void checkAndRegisterStoreManager() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String storeName = storeSpinner.getSelectedItem().toString();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(StoreManager_Register.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the selected store already has a manager
        db.collection("Store").whereEqualTo("name", storeName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean storeHasManager = false;
                        String storeDocId = null;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            storeDocId = document.getId();
                            if (document.contains("managerId") && !document.getString("managerId").isEmpty()) {
                                storeHasManager = true;
                                break;
                            }
                        }

                        if (storeHasManager) {
                            Toast.makeText(StoreManager_Register.this, "This store already has a manager!", Toast.LENGTH_SHORT).show();
                        } else if (storeDocId != null) {
                            String storeDocId2 = storeDocId;
                            // Check if user is already registered
                            db.collection("User").whereEqualTo("email", email)
                                    .get()
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful() && !userTask.getResult().isEmpty()) {
                                            // User already registered
                                            Toast.makeText(StoreManager_Register.this, "User already registered. Please login.", Toast.LENGTH_SHORT).show();
                                            mAuth.signOut();
                                            Intent intent = new Intent(StoreManager_Register.this, LoginPage.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Proceed with user registration and store association
                                            registerStoreManager(fullName, email, password, storeDocId2);
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(StoreManager_Register.this, "Failed to check store: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerStoreManager(String fullName, String email, String password, String storeDocId) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            String userId = user.getUid();

                            // Add user to Users collection
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", fullName);
                            userData.put("email", email);
                            userData.put("role", "store_manager");

                            db.collection("User").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update the store document with the managerId
                                        DocumentReference storeRef = db.collection("Store").document(storeDocId);
                                        storeRef.update("managerId", userId)
                                                .addOnSuccessListener(storeUpdate -> {
                                                    Toast.makeText(StoreManager_Register.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                                    // Navigate to store manager dashboard or item list
                                                    Intent intent = new Intent(StoreManager_Register.this, Price__Comparision.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(StoreManager_Register.this, "Failed to update store: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(StoreManager_Register.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(StoreManager_Register.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
