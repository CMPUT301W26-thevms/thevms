package com.example.thevms.ui;

import android.os.Bundle;
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
import com.example.thevms.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

/**
 * Fragment for the main search/event listing screen.
 */
public class SearchFragment extends Fragment {

    private LinearLayout eventListContainer;
    private TextView resultsCountText;
    private DatabaseHandler dbHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        eventListContainer = view.findViewById(R.id.event_list_container);
        resultsCountText = view.findViewById(R.id.results_count);
        dbHandler = new DatabaseHandler();
        
        loadEvents();
        
        return view;
    }

    private void loadEvents() {
        dbHandler.getAllEvents().addOnSuccessListener(queryDocumentSnapshots -> {
            eventListContainer.removeAllViews(); // Clear dummy/previous views
            
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        Event event = Event.fromMap(data);
                        if (event != null) {
                            View card = EventCardHelper.createEventCard(eventListContainer, event);
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
