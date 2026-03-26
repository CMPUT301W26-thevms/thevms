package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.example.thevms.ui.Event.OrganizerEventAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for organizers to view and manage their own created events.
 * Displays a list of events associated with the current device ID.
 */
public class MyEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrganizerEventAdapter adapter;
    private ProgressBar loadingBar;
    private TextView emptyText;
    private DatabaseHandler dbHandler;
    private Event eventToUpdate;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && eventToUpdate != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        byte[] photoBytes = compressImage(bitmap);
                        eventToUpdate.setPhoto(photoBytes);
                        eventToUpdate.save().addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Poster updated successfully", Toast.LENGTH_SHORT).show();
                            loadMyEvents();
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to update poster", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.rv_my_events);
        loadingBar = findViewById(R.id.pb_loading);
        emptyText = findViewById(R.id.tv_no_events);

        dbHandler = new DatabaseHandler();
        adapter = new OrganizerEventAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnEventCancelListener(this::showCancelConfirmationDialog);
        adapter.setOnEventUpdatePosterListener(event -> {
            this.eventToUpdate = event;
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        loadMyEvents();
    }

    /**
     * Shows a confirmation dialog before cancelling (deleting) an event.
     *
     * @param event The event to be cancelled.
     */
    private void showCancelConfirmationDialog(Event event) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_confirmation, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Optional: Make dialog background transparent to show the card corner radius
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnYes = dialogView.findViewById(R.id.btn_dialog_yes);
        MaterialButton btnBack = dialogView.findViewById(R.id.btn_dialog_back);
        View ivClose = dialogView.findViewById(R.id.iv_close);

        btnYes.setOnClickListener(v -> {
            dbHandler.deleteEvent(String.valueOf(event.getEventId()))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event cancelled", Toast.LENGTH_SHORT).show();
                        loadMyEvents(); // Refresh list
                    });
            dialog.dismiss();
        });

        btnBack.setOnClickListener(v -> dialog.dismiss());
        ivClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Fetches the list of events organized by the current user and updates the UI.
     */
    private void loadMyEvents() {
        loadingBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        dbHandler.getEventsByOrganizer(deviceId).addOnSuccessListener(queryDocumentSnapshots -> {
            List<Event> myEvents = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Event event = Event.fromDoc(doc);
                if (event != null) {
                    myEvents.add(event);
                }
            }

            loadingBar.setVisibility(View.GONE);
            if (myEvents.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
            } else {
                adapter.setEvents(myEvents);
            }
        }).addOnFailureListener(e -> {
            loadingBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
        });
    }

    private byte[] compressImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return baos.toByteArray();
    }
}
