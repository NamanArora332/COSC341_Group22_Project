package com.example.cosc341_group22_project;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private SeekBar seekBar;
    private TextView priceTextView;
    private Button applyFilterButton;
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> allProducts; // List to hold all products

    private static final double MIN_PRICE = 0.99;  // Minimum price
    private static final double MAX_PRICE = 10.00; // Maximum price

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // Initialize Views
        categorySpinner = findViewById(R.id.categorySpinner);
        seekBar = findViewById(R.id.priceSeekBar);
        priceTextView = findViewById(R.id.priceTextView);
        applyFilterButton = findViewById(R.id.applyFilterButton);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);

        // Initialize RecyclerView
        productAdapter = new ProductAdapter(this, new ArrayList<>());
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);

        // Fetch products from Firestore
        getAllProducts();

        // Populate Spinner with categories
        String[] categories = {"Pantry", "Meat", "Dairy", "Beverages", "Frozen", "Produce", "Bakery"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Set up SeekBar with min, max and initial values
        seekBar.setMax(9900);  // We multiply by 100 to handle decimals with integer progress
        seekBar.setProgress(500);  // Default value to start with (50% of range)
        priceTextView.setText("Price: $" + formatPrice(seekBar.getProgress())); // Set initial price display

        // Update the price display as the SeekBar is moved
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert the SeekBar progress to a decimal value and display it
                priceTextView.setText("Price: $" + formatPrice(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Apply Filter button functionality
        applyFilterButton.setOnClickListener(v -> {
            String selectedCategory = categorySpinner.getSelectedItem().toString();
            double selectedPrice = getPriceFromSeekBar(seekBar.getProgress());
            applyFilters(selectedCategory, selectedPrice);
        });
    }

    // Fetching all products from Firestore
    private void getAllProducts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Products")  // Assuming your collection is named "Products"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        allProducts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Product product = document.toObject(Product.class);
                            allProducts.add(product);  // Add product to the list
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load products: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Apply filters to the products list
    private void applyFilters(String category, double maxPrice) {
        List<Product> filteredProducts = new ArrayList<>();

        // Filter products based on category and price (strictly less than the selected price)
        for (Product product : allProducts) {
            if (product.getCategory().equals(category) && product.getPrice() < maxPrice) {
                filteredProducts.add(product);
            }
        }

        if (filteredProducts.isEmpty()) {
            Toast.makeText(this, "No products found with the applied filters.", Toast.LENGTH_SHORT).show();
        } else {
            // Pass the filtered list to RecyclerView adapter
            productAdapter.updateProducts(filteredProducts);
        }
    }

    // Convert SeekBar progress to price range between MIN_PRICE and MAX_PRICE
    private double getPriceFromSeekBar(int progress) {
        return MIN_PRICE + (progress / 100.0);
    }

    // Format price to display with two decimal places
    private String formatPrice(int progress) {
        double price = MIN_PRICE + (progress / 100.0);
        return String.format("%.2f", price);
    }
}
