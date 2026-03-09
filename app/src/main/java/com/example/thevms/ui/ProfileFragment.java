package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        // Navigation to Settings
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        // Navigation to My Events
        myEventsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyEventsActivity.class);
            startActivity(intent);
        });

        return view;
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
