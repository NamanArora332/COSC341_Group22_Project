package com.example.cosc341_group22_project;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;



import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;





public class Add_Edit_Product extends AppCompatActivity {
    private EditText productNameEditText, productPriceEditText, productQuantityEditText, productCategoryEditText, productDiscountEditText;
    private Button saveProductButton;

    private FirebaseFirestore db;
    private String productId; // Null if adding a new product
    private String storeId, storeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_product);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        productNameEditText = findViewById(R.id.productNameEditText);
        productPriceEditText = findViewById(R.id.productPriceEditText);
        productQuantityEditText = findViewById(R.id.productQuantityEditText);
        productCategoryEditText = findViewById(R.id.productCategoryEditText);
        productDiscountEditText = findViewById(R.id.productDiscountEditText);
        saveProductButton = findViewById(R.id.saveProductButton);



        // Get Intent Data
        productId = getIntent().getStringExtra("productId");
        storeId = getIntent().getStringExtra("storeId");
        storeName = getIntent().getStringExtra("storeName");

        if (productId != null) {
            // Editing a product
            setTitle("Edit Product");
            loadProductData();
        } else {
            // Adding a new product
            setTitle("Add Product");
        }

        // Save Button Listener
        saveProductButton.setOnClickListener(v -> saveProduct());





    }
    private void loadProductData() {
        // Load product details from Firestore
        db.collection("Product").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        productNameEditText.setText(documentSnapshot.getString("name"));
                        productPriceEditText.setText(String.valueOf(documentSnapshot.getDouble("price")));
                        productQuantityEditText.setText(String.valueOf(documentSnapshot.getLong("quantity")));
                        productCategoryEditText.setText(documentSnapshot.getString("category"));
                        productDiscountEditText.setText(String.valueOf(documentSnapshot.getLong("discount")));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load product data", Toast.LENGTH_SHORT).show());
    }
    private void saveProduct() {
        String name = productNameEditText.getText().toString().trim();
        double price = Double.parseDouble(productPriceEditText.getText().toString().trim());
        int quantity = Integer.parseInt(productQuantityEditText.getText().toString().trim());
        String category = productCategoryEditText.getText().toString().trim();
        int discount = Integer.parseInt(productDiscountEditText.getText().toString().trim());

        if (name.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("price", price);
        product.put("quantity", quantity);
        product.put("category", category);
        product.put("discount", discount);
        product.put("storeId", storeId);
        product.put("storeName", storeName);

        if (productId != null) {
            // Update existing product
            db.collection("Product").document(productId)
                    .update(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show());
        } else {
            // Add new product
            db.collection("Product")
                    .add(product)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
        }
    }

}