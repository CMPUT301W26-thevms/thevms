package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.Event;
import com.example.thevms.ui.Event.EventAdapter;

import java.util.ArrayList;
import java.util.List;

public class EntrantEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private ProgressBar loadingBar;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_entrant_events, container, false);

        recyclerView = view.findViewById(R.id.rv_my_events);
        loadingBar = view.findViewById(R.id.pb_loading);
        emptyText = view.findViewById(R.id.tv_no_events);



        adapter = new EventAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadSignedUpEvents();

        return view;
    }

    private void loadSignedUpEvents() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);
        if (emptyText != null) emptyText.setVisibility(View.GONE);

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            entrant.getRegisteredEvents().addOnSuccessListener(events -> {
                if (loadingBar != null) loadingBar.setVisibility(View.GONE);
                if (events.isEmpty()) {
                    if (emptyText != null) {
                        emptyText.setText("You haven't signed up for any events yet.");
                        emptyText.setVisibility(View.VISIBLE);
                    }
                } else {
                    adapter.setEvents(events);
                }
            }).addOnFailureListener(e -> {
                if (loadingBar != null) loadingBar.setVisibility(View.GONE);
            });
        });
    }
}
