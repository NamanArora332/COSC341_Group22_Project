package com.example.cosc341_group22_project;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Price__Comparision extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button logoutButton;
    private Button storeItemsButton; // Button for store manager
    private EditText searchEditText;
    private RecyclerView productsRecyclerView;

    private SearchResultsAdapter searchResultsAdapter;
    private List<Product> searchResultsList;

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

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        logoutButton = findViewById(R.id.logoutButton);
        storeItemsButton = findViewById(R.id.storeItemsButton);
        searchEditText = findViewById(R.id.searchEditText);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);

        // Initialize RecyclerView
        searchResultsList = new ArrayList<>();
        searchResultsAdapter = new SearchResultsAdapter(this, searchResultsList);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(searchResultsAdapter);

        // Initially hide the store manager button
        storeItemsButton.setVisibility(View.GONE);

        // Logout button functionality
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(Price__Comparision.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Price__Comparision.this, LoginPage.class);
            startActivity(intent);
            finish();
        });

        // Store Items Button functionality
        storeItemsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Price__Comparision.this, Store_Items_List.class);
            startActivity(intent);
        });

        // Fetch user role to check if they're a store manager
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserRole(currentUser.getUid());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Price__Comparision.this, LoginPage.class);
            startActivity(intent);
            finish();
        }

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(); // Convert query to lowercase
                if (!query.isEmpty()) {
                    searchProducts(query);
                } else {
                    searchResultsList.clear();
                    searchResultsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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

    private void searchProducts(String query) {
        // Query Firestore for products
        db.collection("Product")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        searchResultsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);

                            // Perform a case-insensitive "startsWith" check on the product name
                            if (product.getName().toLowerCase().startsWith(query)) {
                                searchResultsList.add(product);
                            }
                        }
                        searchResultsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to fetch products: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
