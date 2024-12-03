package com.example.cosc341_group22_project;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private Context context;
    private FirebaseFirestore db;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        // Bind data to the views
        holder.productName.setText(product.getName());
        holder.productQuantity.setText("Quantity: " + product.getQuantity());
        holder.productPrice.setText("Current Price: $" + product.getPrice());

        // Set up Edit button functionality
        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, Add_Edit_Product.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("storeId", product.getStoreId());
            intent.putExtra("storeName", product.getStoreName());
            context.startActivity(intent);
        });

        // Set up Remove button functionality
        holder.removeButton.setOnClickListener(v -> {
            // Handle Remove button click
            db.collection("Products").document(product.getId()) // Ensure the collection name is "Products"
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, product.getName() + " removed successfully", Toast.LENGTH_SHORT).show();
                        // Refresh product list
                        productList.remove(position);  // Remove from list
                        notifyItemRemoved(position);   // Notify adapter of removal
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // Update the product list and notify adapter
    public void updateProducts(List<Product> newProducts) {
        productList.clear();           // Clear existing products
        productList.addAll(newProducts); // Add filtered products
        notifyDataSetChanged();        // Notify adapter that the data has changed
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productQuantity, productPrice;
        Button editButton, removeButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productQuantity = itemView.findViewById(R.id.productQuantity);
            productPrice = itemView.findViewById(R.id.productPrice);
            editButton = itemView.findViewById(R.id.editButton);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}
