package com.example.cosc341_group22_project;

import android.content.Intent;
import android.os.Bundle;
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
    private Button applyFilterButton, backToComparisonButton;
    private RecyclerView resultsRecyclerView;
    private SearchResultsAdapter searchResultsAdapter;
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
        backToComparisonButton = findViewById(R.id.backToComparisonButton);
        resultsRecyclerView = findViewById(R.id.productsRecyclerView);

        // Set up RecyclerView
        filteredProducts = new ArrayList<>();
        searchResultsAdapter = new SearchResultsAdapter(this, filteredProducts);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setAdapter(searchResultsAdapter);

        // Populate the spinner with categories
        List<String> categories = Arrays.asList("All", "Pantry", "Meat", "Dairy", "Beverages", "Frozen", "Produce", "Bakery");
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Set up apply filter button click listener
        applyFilterButton.setOnClickListener(v -> applyFilter());

        // Back button click listener
        backToComparisonButton.setOnClickListener(v -> {
            Intent intent = new Intent(FilterActivity.this, Price__Comparision.class);
            startActivity(intent);
            finish();
        });
    }

    private void applyFilter() {
        // Retrieve filter values
        String category = categorySpinner.getSelectedItem().toString();
        String minPriceStr = minPriceEditText.getText().toString().trim();
        String maxPriceStr = maxPriceEditText.getText().toString().trim();
        String discountStr = discountEditText.getText().toString().trim();

        // Parse filter values
        double minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
        double maxPrice = maxPriceStr.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceStr);
        int discount = discountStr.isEmpty() ? 0 : Integer.parseInt(discountStr);

        // Create Firestore query
        Query query;

        if ("All".equalsIgnoreCase(category)) {
            // Fetch all products
            query = db.collection("Product");
        } else {
            // Fetch products for the selected category
            query = db.collection("Product").whereEqualTo("category", category);
        }

        // Execute the query
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                filteredProducts.clear();
                task.getResult().forEach(document -> {
                    Product product = document.toObject(Product.class);

                    // Apply client-side filtering for price and discount
                    if (product.getPrice() >= minPrice &&
                            product.getPrice() <= maxPrice &&
                            product.getDiscount() >= discount) {
                        filteredProducts.add(product);
                    }
                });
                searchResultsAdapter.notifyDataSetChanged(); // Update RecyclerView
            } else {
                Toast.makeText(FilterActivity.this, "Error fetching products: " + task.getException(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
