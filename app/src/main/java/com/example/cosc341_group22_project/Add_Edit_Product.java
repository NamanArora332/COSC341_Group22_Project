package com.example.cosc341_group22_project;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;




import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;





public class Add_Edit_Product extends AppCompatActivity {
    private EditText productNameEditText, productPriceEditText, productQuantityEditText, productCategoryEditText, productDiscountEditText,productImageUrlEditText;
    private Button saveProductButton, uploadPhotoButton;

    private FirebaseFirestore db;
    private String productId, storeId, storeName; // Null if adding a new product

    private ImageView productImageView;
    private StorageReference storageReference;
    private Uri imageUri;


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
        productImageView = findViewById(R.id.productImageView);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        storageReference = FirebaseStorage.getInstance().getReference("product_photos");
        productImageUrlEditText = findViewById(R.id.productImageUrlEditText);



// Set up Upload Button
        uploadPhotoButton.setOnClickListener(v -> openGallery());


        // Initialize Back Button
        Button backButton = findViewById(R.id.backButton);

// Set Click Listener for Back Button
        backButton.setOnClickListener(v -> {
            // Finish the current activity and return to the previous one
            finish();
        });

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



//    private void openGallery() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, 101);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
//            photoUri = data.getData();
//            productImageView.setImageURI(photoUri); // Display selected image
//        }
//    }
    private void uploadImageToStorage(OnCompleteListener<Uri> onCompleteListener) {
        if (imageUri != null) {
            String fileName = "products/" + System.currentTimeMillis() + ".jpg";
            Log.d("FirebaseUpload", "Uploading to: " + fileName);
            FirebaseStorage.getInstance().getReference(fileName)
                    .putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl()
                            .addOnCompleteListener(onCompleteListener))
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
        }
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            productImageView.setImageURI(imageUri);
        }
    }

    private void loadProductData() {
        db.collection("Product").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        productNameEditText.setText(documentSnapshot.getString("name"));
                        productPriceEditText.setText(String.valueOf(documentSnapshot.getDouble("price")));
                        productQuantityEditText.setText(String.valueOf(documentSnapshot.getLong("quantity")));
                        productCategoryEditText.setText(documentSnapshot.getString("category"));
                        productDiscountEditText.setText(String.valueOf(documentSnapshot.getLong("discount")));
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        productImageUrlEditText.setText(photoUrl); // Set URL in the EditText
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_placeholder) // Add a placeholder
                                    .error(R.drawable.ic_placeholder) // Handle error
                                    .into(productImageView); // Load image into ImageView
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load product data", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveProduct() {
        String name = productNameEditText.getText().toString().trim();
        double price = Double.parseDouble(productPriceEditText.getText().toString().trim());
        int quantity = Integer.parseInt(productQuantityEditText.getText().toString().trim());
        String category = productCategoryEditText.getText().toString().trim();
        int discount = Integer.parseInt(productDiscountEditText.getText().toString().trim());
        String imageUrl = productImageUrlEditText.getText().toString().trim(); // Get URL from EditText

        if (name.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Save product with the URL
            saveProductToFirestore(name, price, quantity, category, discount, imageUrl);
        } else if (imageUri != null) {
            // If URL is empty, upload local image
            uploadImageToStorage(uriTask -> {
                if (uriTask.isSuccessful()) {
                    String uploadedImageUrl = uriTask.getResult().toString();
                    saveProductToFirestore(name, price, quantity, category, discount, uploadedImageUrl);
                } else {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveProductToFirestore(name, price, quantity, category, discount, null);
        }
    }



    private void saveProductToFirestore(String name, double price, int quantity, String category, int discount, String imageUrl) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("price", price);
        product.put("quantity", quantity);
        product.put("category", category);
        product.put("discount", discount);
        product.put("imageUrl", imageUrl);

        if (productId != null) {
            db.collection("Product").document(productId)
                    .update(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show());
        } else {
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