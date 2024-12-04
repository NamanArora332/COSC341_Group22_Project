package com.example.cosc341_group22_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NotificationProductAdapter extends RecyclerView.Adapter<NotificationProductAdapter.ProductViewHolder> {

    private List<String> productList;
    private List<String> filteredList;
    private Set<String> selectedProducts;

    /**
     * Constructor for NotificationProductAdapter.
     *
     * @param productList      The full list of products.
     * @param selectedProducts The set of currently selected products.
     */
    public NotificationProductAdapter(List<String> productList, Set<String> selectedProducts) {
        this.productList = productList;
        this.filteredList = new ArrayList<>(productList);
        this.selectedProducts = selectedProducts;
    }

    /**
     * ViewHolder class for Product items.
     */
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView;
        CheckBox productCheckBox;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            productCheckBox = itemView.findViewById(R.id.productCheckBox);
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the product item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        // Get the product name based on the filtered list
        String product = filteredList.get(position);
        holder.productNameTextView.setText(product);

        // Set the checkbox state based on selection
        holder.productCheckBox.setChecked(selectedProducts.contains(product));

        // Handle checkbox clicks
        holder.productCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedProducts.add(product);
            } else {
                selectedProducts.remove(product);
            }
        });

        // Handle item view clicks to toggle checkbox
        holder.itemView.setOnClickListener(v -> {
            holder.productCheckBox.setChecked(!holder.productCheckBox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * Filters the product list based on the search query.
     *
     * @param query The search query entered by the user.
     */
    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(productList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (String product : productList) {
                if (product.toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(product);
                }
            }
        }
        notifyDataSetChanged();
    }
}
