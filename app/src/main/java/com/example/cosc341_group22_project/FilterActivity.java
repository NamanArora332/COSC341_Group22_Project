package com.example.cosc341_group22_project;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Spinner categorySpinner;
    private EditText minPriceEditText, maxPriceEditText, discountEditText;
    private Button applyFilterButton;
    private RecyclerView resultsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> filteredProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        categorySpinner = findViewById(R.id.categorySpinner);
        minPriceEditText = findViewById(R.id.minPriceEditText);
        maxPriceEditText = findViewById(R.id.maxPriceEditText);
        discountEditText = findViewById(R.id.discountEditText);
        applyFilterButton = findViewById(R.id.applyFilterButton);
        resultsRecyclerView = findViewById(R.id.productsRecyclerView);

        // Set up RecyclerView
        filteredProducts = new ArrayList<>();
        productAdapter = new ProductAdapter(this, filteredProducts);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setAdapter(productAdapter);

        // Populate the spinner with categories
        List<String> categories = Arrays.asList("All", "Pantry", "Meat", "Dairy", "Beverages", "Frozen", "Produce", "Bakery");
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Set up apply filter button click listener
        applyFilterButton.setOnClickListener(v -> applyFilter());
    }

    private void applyFilter() {
        String category = categorySpinner.getSelectedItem().toString();
        String minPriceStr = minPriceEditText.getText().toString().trim();
        String maxPriceStr = maxPriceEditText.getText().toString().trim();
        String discountStr = discountEditText.getText().toString().trim();

        // Default values for filters if empty input
        double minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
        double maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);
        int discount = discountStr.isEmpty() ? 0 : Integer.parseInt(discountStr);

        // Build Firestore query
        Query query = db.collection("Product");

        // Apply category filter if selected (skip if "All")
        if (!category.equals("All")) {
            query = query.whereEqualTo("category", category);
        }

        // Apply price range filter if specified
        if (minPrice > 0) {
            query = query.whereGreaterThanOrEqualTo("price", minPrice);
        }
        if (maxPrice < Double.MAX_VALUE) {
            query = query.whereLessThanOrEqualTo("price", maxPrice);
        }

        // Apply discount filter if specified
        if (discount > 0) {
            query = query.whereGreaterThanOrEqualTo("discount", discount);
        }

        // Fetch filtered products from Firestore
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                filteredProducts.clear(); // Clear previous results
                task.getResult().forEach(document -> {
                    Product product = document.toObject(Product.class);
                    filteredProducts.add(product);
                });
                productAdapter.notifyDataSetChanged(); // Update the RecyclerView
            } else {
                Toast.makeText(FilterActivity.this, "Error fetching products: " + task.getException(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
