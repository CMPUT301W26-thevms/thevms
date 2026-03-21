package com.example.thevms.ui.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Notification;
import com.example.thevms.ui.NotificationAdapter;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for administrators to view a log of all notifications sent in the system.
 */
public class AdminLogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyLogsText;
    private DatabaseHandler dbHandler;
    private ListenerRegistration logsListener;

    public AdminLogsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_logs, container, false);

        recyclerView = view.findViewById(R.id.logs_recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyLogsText = view.findViewById(R.id.empty_logs_text);

        dbHandler = new DatabaseHandler();

        setupRecyclerView();
        setupSwipeRefresh();

        return view;
    }

    private void setupRecyclerView() {
        // Initialize adapter in read-only mode (no buttons)
        adapter = new NotificationAdapter(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (logsListener != null) {
                logsListener.remove();
            }
            startListeningToLogs();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningToLogs();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (logsListener != null) {
            logsListener.remove();
        }
    }

    private void startListeningToLogs() {
        swipeRefreshLayout.setRefreshing(true);
        logsListener = dbHandler.listenToAllNotifications((value, error) -> {
            swipeRefreshLayout.setRefreshing(false);
            if (error != null) {
                Toast.makeText(getContext(), "Error loading logs: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (value != null) {
                List<Notification> logs = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    Notification notification = doc.toObject(Notification.class);
                    notification.setId(doc.getId());
                    logs.add(notification);
                }
                adapter.setNotifications(logs);
                updateEmptyState(logs.isEmpty());
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyLogsText.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
        } else {
            emptyLogsText.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
        }
    }
}
