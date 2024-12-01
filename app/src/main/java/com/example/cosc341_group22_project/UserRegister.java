package com.example.cosc341_group22_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

public class UserRegister extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText fullNameEditText, emailEditText, passwordEditText;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        // Set up register button listener
        registerButton.setOnClickListener(v -> checkIfUserExists());

        // Set up back-to-login listener
        TextView backToLogin = findViewById(R.id.instructionText);
        backToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(UserRegister.this, LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
            startActivity(intent);
        });
    }

    private void checkIfUserExists() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(UserRegister.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attempt to sign in with the provided email
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User already exists
                        Toast.makeText(UserRegister.this, "User already registered. Please login.", Toast.LENGTH_SHORT).show();
                        // Sign out the user
                        mAuth.signOut();
                        // Redirect to login page
                        Intent intent = new Intent(UserRegister.this, LoginPage.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                        startActivity(intent);
                        finish();
                    } else {
                        // If sign-in fails, register the user
                        if (task.getException() instanceof FirebaseAuthInvalidUserException || task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            registerUser(email, password);
                        } else {
                            Toast.makeText(UserRegister.this, "An error occurred: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Save user information in Firestore
                            saveUserToFirestore(user.getUid(), fullNameEditText.getText().toString().trim(), email);
                        }
                    } else {
                        Toast.makeText(UserRegister.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String email) {
        // Prepare user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", fullName);
        userData.put("email", email);
        userData.put("role", "user"); // Default role

        // Save to Firestore
        db.collection("User").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserRegister.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                    // Navigate to Price_Comparision page (user is logged in automatically)
                    Intent intent = new Intent(UserRegister.this, Price__Comparision.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserRegister.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
