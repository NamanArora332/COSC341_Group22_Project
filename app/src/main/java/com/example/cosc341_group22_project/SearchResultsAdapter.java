package com.example.cosc341_group22_project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder> {
    private List<Product> productList;
    private Context context;

    public SearchResultsAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_product, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productName.setText("Name: " + product.getName());
        holder.productStore.setText("Store: " + product.getStoreName());
        holder.productPrice.setText("Price: $" + product.getPrice());
        holder.productQuantity.setText("Quantity: " + product.getQuantity());
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productStore, productPrice, productQuantity;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productStore = itemView.findViewById(R.id.productStore);
            productPrice = itemView.findViewById(R.id.productPrice);
            productQuantity = itemView.findViewById(R.id.productQuantity);
        }
    }
}
