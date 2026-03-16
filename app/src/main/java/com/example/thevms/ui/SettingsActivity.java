package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

/**
 * Activity for managing user settings and profile updates.
 * Allows users to edit their personal information and toggle notification preferences.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText firstNameEdit, lastNameEdit, emailEdit, phoneEdit;
    private MaterialSwitch notificationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View root = findViewById(R.id.settings_title).getParent().getParent() instanceof View ? (View) findViewById(R.id.settings_title).getParent().getParent() : findViewById(android.R.id.content);
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
        notificationSwitch = findViewById(R.id.switch_notifications);

        ImageView backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Button confirmButton = findViewById(R.id.btn_confirm);
        confirmButton.setOnClickListener(v -> {
            if (validateInputs()) {
                showConfirmDialog(deviceId);
            }
        });

        Button deleteAccountButton = findViewById(R.id.btn_delete_account);
        deleteAccountButton.setOnClickListener(v -> {
            // Logic for deleting account will go here
        });

        loadUserSettings(deviceId);
    }

    /**
     * Validates the user input fields for correctness.
     *
     * @return True if all inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        String firstName = Objects.requireNonNull(firstNameEdit.getText()).toString().trim();
        String lastName = Objects.requireNonNull(lastNameEdit.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEdit.getText()).toString().trim();
        String phone = Objects.requireNonNull(phoneEdit.getText()).toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(firstName)) {
            firstNameEdit.setError("First name is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(lastName)) {
            lastNameEdit.setError("Last name is required");
            isValid = false;
        }
        if (TextUtils.isEmpty(email)) {
            emailEdit.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEdit.setError("Invalid email address");
            isValid = false;
        }

        if (!TextUtils.isEmpty(phone) && !Patterns.PHONE.matcher(phone).matches()) {
            phoneEdit.setError("Invalid phone number");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Shows a confirmation dialog before saving profile changes.
     *
     * @param deviceId The unique ID of the device.
     */
    private void showConfirmDialog(String deviceId) {
        new AlertDialog.Builder(this)
                .setTitle("Save Changes")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes", (dialog, which) -> handleUpdate(deviceId))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Handles the process of updating the user profile in the database.
     *
     * @param deviceId The unique ID of the device.
     */
    private void handleUpdate(String deviceId) {
        String firstName = Objects.requireNonNull(firstNameEdit.getText()).toString().trim();
        String lastName = Objects.requireNonNull(lastNameEdit.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEdit.getText()).toString().trim();
        String phone = Objects.requireNonNull(phoneEdit.getText()).toString().trim();
        boolean notificationsEnabled = notificationSwitch.isChecked();

        // Handle optional phone field
        if (TextUtils.isEmpty(phone)) {
            phone = null;
        }

        // Update and save the user
        Entrant updatedEntrant = new Entrant(deviceId, email, firstName, lastName, phone, notificationsEnabled);
        updatedEntrant.save().addOnSuccessListener(aVoid -> {
            Toast.makeText(SettingsActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(SettingsActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Loads the current user settings from the database and populates the UI.
     *
     * @param deviceId The unique ID of the device.
     */
    private void loadUserSettings(String deviceId) {
        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            firstNameEdit.setText(entrant.getFirstName());
            lastNameEdit.setText(entrant.getLastName());
            emailEdit.setText(entrant.getEmail());
            phoneEdit.setText(entrant.getPhoneNumber());
            notificationSwitch.setChecked(entrant.isNotificationsEnabled());
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load settings", Toast.LENGTH_SHORT).show();
        });
    }
}
