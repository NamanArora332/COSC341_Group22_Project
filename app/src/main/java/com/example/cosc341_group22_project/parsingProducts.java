package com.example.cosc341_group22_project;
import android.content.Context;
import android.content.res.AssetManager;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class parsingProducts {

    public static ArrayList<String> readProductIds(Context context) {
        ArrayList<String> productIds = new ArrayList<>();
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("walmartOffers.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                productIds.add(line.trim());
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return productIds;
    }

    private static final String RAPIDAPI_KEY = "YOUR_RAPIDAPI_KEY";

    public static Product getProductDetails(String productId) {
        OkHttpClient client = new OkHttpClient();

        // API URL with the product ID
        String url = "https://walmart-data.p.rapidapi.com/product-details.php?url=https%3A%2F%2Fwww.walmart.ca%2Fen%2Fip%2F" + productId;

        // Create the HTTP request
        Request request = new Request.Builder()
                .url(url)
                .header("x-rapidapi-key", RAPIDAPI_KEY)
                .header("x-rapidapi-host", "walmart-data.p.rapidapi.com")
                .build();

        // Send the request and handle the response
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            Gson gson = new Gson();
            String jsonResponse = response.body().string();
            return gson.fromJson(jsonResponse, Product.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}