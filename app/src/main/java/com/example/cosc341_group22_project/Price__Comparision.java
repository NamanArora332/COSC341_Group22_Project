package com.example.cosc341_group22_project;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import android.view.View;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.*;
public class Price__Comparision extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button logoutButton;
    private Button storeItemsButton; // Button for store manager
    private String userRole = ""; // To store user role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_price_comparision);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button logout = findViewById(R.id.logoutButton);
        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();
        storeItemsButton = findViewById(R.id.storeItemsButton);
        storeItemsButton.setVisibility(View.GONE); // Initially hide the button

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Toast.makeText(Price__Comparision.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Price__Comparision.this, LoginPage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                startActivity(intent);
                finish();

            }
        });
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRole(currentUser.getUid());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Price__Comparision.this, LoginPage.class);
            startActivity(intent);
            finish();
        }

        storeItemsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Price__Comparision.this, Store_Items_List.class);
            startActivity(intent);
        });








    }
    private void fetchUserRole(String userId) {
        db.collection("User").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRole = documentSnapshot.getString("role");
                        if ("store_manager".equals(userRole)) {
                            storeItemsButton.setVisibility(View.VISIBLE); // Show button for store managers
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}