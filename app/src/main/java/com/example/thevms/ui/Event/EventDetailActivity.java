package com.example.thevms.ui.Event;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Activity for displaying the full details of a single event.
 * Reuses the binding logic from {@link EventAdapter.EventViewHolder} to ensure consistent behavior.
 * Supports launching via deep links (e.g., from a scanned QR code).
 */
public class EventDetailActivity extends AppCompatActivity implements EventAdapter.LocationPermissionRequester {

    private Event event;
    private EventAdapter.EventViewHolder viewHolder;
    private boolean isAdmin = false;
    private boolean isExpanded = true;
    private boolean isCommentsExpanded = false;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        String eventId = getIntent().getStringExtra("EVENT_ID");
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        // Handle Deep Link if launched from QR code
        Uri data = getIntent().getData();
        if (eventId == null && data != null && "thevms".equals(data.getScheme())) {
            eventId = data.getQueryParameter("id");
        }

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Initialize the ViewHolder with the included event card view
        View cardView = findViewById(R.id.included_event_card);
        viewHolder = new EventAdapter.EventViewHolder(cardView);

        loadEvent(eventId);
    }

    private void loadEvent(String eventId) {
        FirebaseFirestore.getInstance().collection(DatabaseHandler.COLLECTION_EVENTS)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    event = Event.fromDoc(documentSnapshot);
                    if (event != null) {
                        updateUI();
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Updates the UI by delegating to the ViewHolder's bind method.
     * This ensures that all logic for joining, leaving, and displaying info is consistent with the list view.
     */
    private void updateUI() {
        if (event == null || viewHolder == null) return;

        viewHolder.bind(event, dateFormat, fullDateFormat, isAdmin, false, isExpanded, isCommentsExpanded,
                this, // LocationPermissionRequester
                () -> {
                    // On event deletion (admin mode)
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                },
                (expand) -> {
                    isExpanded = expand;
                    updateUI();
                },
                (expandComments) -> {
                    isCommentsExpanded = expandComments;
                    updateUI();
                }
        );
    }

    @Override
    public void requestLocationPermission(boolean requiredForJoin, @NonNull Runnable onGranted, @NonNull Runnable onDenied) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onGranted.run();
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 77);
            
            // In a production app, we would handle the result in onRequestPermissionsResult
            Toast.makeText(this, "Location permission is required to proceed", Toast.LENGTH_SHORT).show();
        }
    }
}
