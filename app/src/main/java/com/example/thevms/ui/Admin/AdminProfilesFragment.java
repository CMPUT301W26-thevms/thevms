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

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminProfilesFragment extends Fragment {

    private static final String ARG_FILTER_ORGANIZERS = "filter_organizers";

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProfileAdapter adapter;
    private final List<Entrant> profiles = new ArrayList<>();
    private DatabaseHandler dbHandler;
    private boolean filterOrganizersOnly = false;

    public static AdminProfilesFragment newInstance(boolean filterOrganizersOnly) {
        AdminProfilesFragment fragment = new AdminProfilesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FILTER_ORGANIZERS, filterOrganizersOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filterOrganizersOnly = getArguments().getBoolean(ARG_FILTER_ORGANIZERS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profiles, container, false);

        // Set the title Dynamically
        TextView titleView = view.findViewById(R.id.admin_profiles_title_text);
        if (titleView != null && filterOrganizersOnly) {
            titleView.setText(R.string.admin_organizers_title);
        }

        recyclerView = view.findViewById(R.id.profiles_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        if (emptyStateText != null) {
            emptyStateText.setText(filterOrganizersOnly ?
                    R.string.admin_organizers_empty : R.string.admin_profiles_empty);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProfileAdapter(profiles);
        recyclerView.setAdapter(adapter);

        dbHandler = new DatabaseHandler();
        loadProfiles();

        return view;
    }

    private void loadProfiles() {
        dbHandler.getAllUsers().addOnSuccessListener(queryDocumentSnapshots -> {
            profiles.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Entrant entrant = Entrant.fromMap(doc.getId(), doc.getData());
                if (entrant != null) {
                    if (filterOrganizersOnly) {
                        UserRole role = entrant.getRole();
                        if (role == UserRole.ORGANIZER) {
                            profiles.add(entrant);
                        }
                    } else {
                        profiles.add(entrant);
                    }
                }
            }
            updateUI();
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.admin_profiles_load_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (profiles.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private static class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {
        private final List<Entrant> profiles;

        public ProfileAdapter(List<Entrant> profiles) {
            this.profiles = profiles;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_profile, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Entrant entrant = profiles.get(position);
            String fullName = entrant.getFirstName() + " " + entrant.getLastName();

            holder.nameText.setText(fullName.trim().isEmpty() ?
                    holder.itemView.getContext().getString(R.string.admin_profiles_no_name) : fullName);

            holder.roleText.setText(entrant.getRole().name());

            holder.emailText.setText(entrant.getEmail() != null ?
                    entrant.getEmail() : holder.itemView.getContext().getString(R.string.admin_profiles_no_email));

            holder.phoneText.setText(entrant.getPhoneNumber() != null ?
                    entrant.getPhoneNumber() : holder.itemView.getContext().getString(R.string.admin_profiles_no_phone));

            // Delete button functionality - placeholder
            holder.deleteButton.setOnClickListener(v -> {
                // To be implemented: Delete user profile logic
            });
        }

        @Override
        public int getItemCount() {
            return profiles.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, roleText, emailText, phoneText;
            MaterialButton deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.profile_name);
                roleText = itemView.findViewById(R.id.profile_role);
                emailText = itemView.findViewById(R.id.profile_email);
                phoneText = itemView.findViewById(R.id.profile_phone);
                deleteButton = itemView.findViewById(R.id.btn_delete_profile);
            }
        }
    }
}
