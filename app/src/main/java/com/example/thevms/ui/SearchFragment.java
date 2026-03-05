package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.Event.EventViewFactory;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

/**
 * Fragment for the main search/event listing screen.
 */
public class SearchFragment extends Fragment {

    private LinearLayout eventListContainer;
    private TextView resultsCountText;
    private DatabaseHandler dbHandler;

    private boolean isAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        eventListContainer = view.findViewById(R.id.event_list_container);
        resultsCountText = view.findViewById(R.id.results_count);
        dbHandler = new DatabaseHandler();

        checkUserRoleAndLoadEvents();

        return view;
    }

    /**
     * Checks the current user's role from the database and then loads the events.
     */
    private void checkUserRoleAndLoadEvents() {
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(user -> {
            this.isAdmin = (user.getRole() == UserRole.ADMIN);
            Log.d("SearchFragment", "User role verified. Is Admin: " + isAdmin);

            // Now that we know the role, load the events
            loadEvents();
        }).addOnFailureListener(e -> {
            Log.e("SearchFragment", "Error verifying user role", e);
            // Default to non-admin and try to load events anyway
            this.isAdmin = false;
            loadEvents();
        });
    }

    private void loadEvents() {
        dbHandler.getAllEvents().addOnSuccessListener(queryDocumentSnapshots -> {
            eventListContainer.removeAllViews();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        Event event = Event.fromMap(data);
                        if (event != null) {
                            // Pass the verified isAdmin status to the factory
                            View card = EventViewFactory.createEventCard(eventListContainer, event, isAdmin);
                            eventListContainer.addView(card);
                        }
                    }
                } catch (Exception e) {
                    Log.e("SearchFragment", "Error mapping event", e);
                }
            }
            resultsCountText.setText(queryDocumentSnapshots.size() + " results");
        }).addOnFailureListener(e -> {
            Log.e("SearchFragment", "Error loading events", e);
        });
    }
}
