package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrganizerEventAdapter adapter;
    private ProgressBar loadingBar;
    private TextView emptyText;
    private DatabaseHandler dbHandler;

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

        loadMyEvents();
    }

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
}
