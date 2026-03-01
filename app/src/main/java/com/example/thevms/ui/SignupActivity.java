package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText firstNameEdit, lastNameEdit, emailEdit, phoneEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        View root = findViewById(R.id.app_name).getParent().getParent() instanceof View ? (View) findViewById(R.id.app_name).getParent().getParent() : findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        firstNameEdit = findViewById(R.id.edit_first_name);
        lastNameEdit = findViewById(R.id.edit_last_name);
        emailEdit = findViewById(R.id.edit_email);
        phoneEdit = findViewById(R.id.edit_phone);
        Button signupButton = findViewById(R.id.btn_signup);

        // Get Device ID
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Check if user already exists
        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            if (entrant.getEmail() != null) {
                // User already has a profile, skip signup
                navigateToMain();
            }
        });

        signupButton.setOnClickListener(v -> {
            handleSignup(deviceId);
        });
    }

    private void handleSignup(String deviceId) {
        String firstName = Objects.requireNonNull(firstNameEdit.getText()).toString().trim();
        String lastName = Objects.requireNonNull(lastNameEdit.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEdit.getText()).toString().trim();
        String phone = Objects.requireNonNull(phoneEdit.getText()).toString().trim();

        // Basic presence validation
        if (TextUtils.isEmpty(firstName)) {
            firstNameEdit.setError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            lastNameEdit.setError("Last name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEdit.setError("Email is required");
            return;
        }

        // Email regex validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEdit.setError("Invalid email address");
            return;
        }

        // Phone validation (optional)
        if (!TextUtils.isEmpty(phone)) {
            if (!Patterns.PHONE.matcher(phone).matches()) {
                phoneEdit.setError("Invalid phone number");
                return;
            }
        } else {
            phone = null; // Ensure blank phone is stored as null
        }

        // Create and save the new user
        Entrant newEntrant = new Entrant(deviceId, email, firstName, lastName, phone);
        newEntrant.save().addOnSuccessListener(aVoid -> {
            Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        }).addOnFailureListener(e -> {
            Toast.makeText(SignupActivity.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
