package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.Organizer;

import java.util.Date;

/**
 * Fragment for creating a new event.
 */
public class CreateEventFragment extends Fragment {

    private EditText etName, etLocation, etDescription;
    private Button btnConfirm, btnCancel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        etName = view.findViewById(R.id.et_event_name);
        etLocation = view.findViewById(R.id.et_event_location);
        etDescription = view.findViewById(R.id.et_event_description);
        btnConfirm = view.findViewById(R.id.btn_confirm);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // Setup cancel button to show custom confirmation dialog
        btnCancel.setOnClickListener(v -> showCancelConfirmationDialog());

        // Setup confirm button to save the event
        btnConfirm.setOnClickListener(v -> handleCreateEvent());
    }

    /**
     * Records fields, creates an Event object, and saves it to the database.
     */
    private void handleCreateEvent() {
        String name = etName.getText().toString().trim();
        String locationStr = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // Fetch current user to use as Organizer
        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            Organizer organizer = new Organizer(user.getDeviceId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());

            // For now, using dummy dates as date pickers are not yet implemented
            Date dummyDate = new Date();

            Event.create(name, description, organizer, null, null, dummyDate, dummyDate, dummyDate, dummyDate)
                    .addOnSuccessListener(event -> {
                        event.save().addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                            try {
                                getParentFragmentManager().popBackStack();
                            } catch (IllegalStateException e) {
                                Log.e("CreateEventFragment", "Error while popping back stack", e);
                            }
                        }).addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to save event", Toast.LENGTH_SHORT).show();
                        });
                    }).addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to create event object", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    /**
     * Displays a custom confirmation dialog asking the user if they are sure they want to cancel.
     */
    private void showCancelConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancel_confirmation, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(dialogView)
                .create();

        ImageView closeIcon = dialogView.findViewById(R.id.iv_close);
        Button backButton = dialogView.findViewById(R.id.btn_dialog_back);
        Button yesButton = dialogView.findViewById(R.id.btn_dialog_yes);

        closeIcon.setOnClickListener(v -> dialog.dismiss());
        backButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                getParentFragmentManager().popBackStack();
            } catch (IllegalStateException e) {
                Log.e("CreateEventFragment", "Error while popping back stack", e);
            }
        });

        dialog.show();
    }
}
