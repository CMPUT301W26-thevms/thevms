package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;

/**
 * Fragment for displaying user profile information.
 */
public class ProfileFragment extends Fragment {

    private TextView nameText, emailText, phoneText;
    private LinearLayout settingsButton;
    private LinearLayout myEventsButton;
    private LinearLayout howItWorksButton;
    private Button deleteProfileButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize UI components
        nameText = view.findViewById(R.id.profile_name);
        emailText = view.findViewById(R.id.profile_email);
        phoneText = view.findViewById(R.id.profile_phone);
        settingsButton = view.findViewById(R.id.btn_settings);
        myEventsButton = view.findViewById(R.id.btn_my_events);
        howItWorksButton = view.findViewById(R.id.btn_how_it_works);
        deleteProfileButton = view.findViewById(R.id.btn_delete_profile);

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // Navigation to Guidelines Activity
        if (howItWorksButton != null) {
            howItWorksButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), GuidelinesActivity.class);
                startActivity(intent);
            });
        }

        // Navigation to Settings
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        // Navigation to My Events
        if (myEventsButton != null) {
            myEventsButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MyEventsActivity.class);
                startActivity(intent);
            });
        }

        // Delete Profile action
        if (deleteProfileButton != null) {
            deleteProfileButton.setOnClickListener(v -> showDeleteAccountConfirmation(deviceId));
        }

        return view;
    }

    /**
     * Shows a confirmation dialog before deleting the user account.
     *
     * @param deviceId The unique ID of the device.
     */
    private void showDeleteAccountConfirmation(String deviceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_event, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView title = dialogView.findViewById(R.id.tv_dialog_title);
        TextView message = dialogView.findViewById(R.id.tv_dialog_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnDelete = dialogView.findViewById(R.id.btn_dialog_delete);
        ImageView ivClose = dialogView.findViewById(R.id.iv_close);

        if (title != null) title.setText("Delete Profile");
        if (message != null) message.setText("Are you sure you want to delete your profile? This will remove you from all event waitlists and delete any events you have organized. This action cannot be undone.");

        if (btnDelete != null) {
            btnDelete.setText("Delete");
            btnDelete.setOnClickListener(v -> {
                handleDeleteAccount(deviceId);
                dialog.dismiss();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (ivClose != null) ivClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Handles the process of deleting the user account from the database.
     *
     * @param deviceId The unique ID of the device.
     */
    private void handleDeleteAccount(String deviceId) {
        Entrant.deleteAccount(deviceId).addOnSuccessListener(aVoid -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                
                // Redirect to SignupActivity
                Intent intent = new Intent(getActivity(), SignupActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to delete account: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data whenever the fragment becomes visible
        loadUserProfile();
    }

    /**
     * Fetches the user profile from the database based on the device ID and populates the UI.
     */
    private void loadUserProfile() {
        if (getContext() == null) return;

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            if (isAdded()) { // Check if fragment is still attached
                String fullName = entrant.getFirstName() + " " + entrant.getLastName();
                nameText.setText(fullName);
                emailText.setText(entrant.getEmail() != null ? entrant.getEmail() : "No email provided");
                phoneText.setText(entrant.getPhoneNumber() != null ? entrant.getPhoneNumber() : "No phone number provided");

                // Show "My Events" only for Organizers and Admins
                UserRole role = entrant.getRole();
                if (role == UserRole.ORGANIZER || role == UserRole.ADMIN) {
                    myEventsButton.setVisibility(View.VISIBLE);
                } else {
                    myEventsButton.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
