package com.example.cosc341_group22_project;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;








public class Store_Items_List extends AppCompatActivity {
    private TextView storeTitle;
    private RecyclerView productsRecyclerView;
    private Button addProductButton, deleteAllProductsButton, backButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String managerId;
    private String storeId;
    private String storeName;

    private List<Product> productList;
    private ProductAdapter productAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_store_items_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        storeTitle = findViewById(R.id.storeTitle);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);
        addProductButton = findViewById(R.id.addProductButton);
        deleteAllProductsButton = findViewById(R.id.deleteAllProductsButton);
        backButton = findViewById(R.id.backButton);

        // Setup RecyclerView
        productList = new ArrayList<>();
        productAdapter = new ProductAdapter(this, productList);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);



        // Get current manager ID
        managerId = mAuth.getCurrentUser().getUid();

        // Load store and products
        loadStoreAndProducts();

        // Add product button listener
        addProductButton.setOnClickListener(v -> {
            Intent intent = new Intent(Store_Items_List.this, Add_Edit_Product.class);
            intent.putExtra("storeId", storeId);
            intent.putExtra("storeName", storeName);
            startActivity(intent);
        });

        // Delete all products button listener
        deleteAllProductsButton.setOnClickListener(v -> deleteAllProducts());

        // Back button listener
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Store_Items_List.this, Price__Comparision.class);
            startActivity(intent);
            finish();
        });
    }
    private void loadStoreAndProducts() {
        // Fetch the store managed by the current user
        db.collection("Store").whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            storeId = document.getId();
                            storeName = document.getString("name");
                            storeTitle.setText(storeName); // Set store title
                            loadProducts(storeId); // Load products for this store
                            break;
                        }
                    } else {
                        Toast.makeText(this, "No store found for this manager.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load store: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void loadProducts(String storeId) {
        db.collection("Product").whereEqualTo("storeId", storeId)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to listen for changes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        productList.clear(); // Clear the existing list
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Product product = document.toObject(Product.class);
                            product.setId(document.getId()); // Store the document ID
                            productList.add(product);
                        }
                        productAdapter.notifyDataSetChanged(); // Notify the adapter
                    }
                });
    }



    private void deleteAllProducts() {
        // Delete all products for the current store
        if (storeId == null) return;

        db.collection("Product").whereEqualTo("storeId", storeId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("Product").document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Product deleted successfully.", Toast.LENGTH_SHORT).show();
                                        loadProducts(storeId); // Reload products
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete product: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this, "No products to delete.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


