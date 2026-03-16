package com.example.thevms.ui.Admin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

/**
 * Fragment for administrators to view and manage user profiles.
 * Can be configured to show only organizers or all users.
 */
public class AdminProfilesFragment extends Fragment {

    private static final String ARG_FILTER_ORGANIZERS = "filter_organizers";

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar loadingSpinner;
    private ProfileAdapter adapter;
    private final List<Entrant> profiles = new ArrayList<>();
    private DatabaseHandler dbHandler;
    private boolean filterOrganizersOnly = false;
    private String currentDeviceId;

    /**
     * Creates a new instance of AdminProfilesFragment.
     *
     * @param filterOrganizersOnly Whether to filter the list to show only organizers.
     * @return A new instance of AdminProfilesFragment.
     */
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
        
        if (getContext() != null) {
            @SuppressLint("HardwareIds")
            String id = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            this.currentDeviceId = id;
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
        loadingSpinner = view.findViewById(R.id.loading_spinner);

        if (emptyStateText != null) {
            emptyStateText.setText(filterOrganizersOnly ?
                    R.string.admin_organizers_empty : R.string.admin_profiles_empty);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter handles deletion and communicates loading state back to fragment
        adapter = new ProfileAdapter(
                profiles, 
                currentDeviceId,
                () -> setLoading(true), 
                () -> {
                    setLoading(false);
                    loadProfiles();
                },
                () -> setLoading(false)
        );
        recyclerView.setAdapter(adapter);

        dbHandler = new DatabaseHandler();
        loadProfiles();

        return view;
    }

    /**
     * Updates the loading state of the UI.
     *
     * @param isLoading True if data is being loaded, false otherwise.
     */
    private void setLoading(boolean isLoading) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
        } else {
            updateUI();
        }
    }

    /**
     * Fetches user profiles from the database and updates the local list.
     */
    private void loadProfiles() {
        setLoading(true);
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
            setLoading(false);
        }).addOnFailureListener(e -> {
            setLoading(false);
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.admin_profiles_load_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the RecyclerView and empty state text based on the current list of profiles.
     */
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

    /**
     * Adapter for displaying user profiles in the RecyclerView.
     */
    private static class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {
        private final List<Entrant> profiles;
        private final String currentDeviceId;
        private final Runnable onActionStarted;
        private final Runnable onActionSuccess;
        private final Runnable onActionFailure;

        /**
         * Constructs a new ProfileAdapter.
         *
         * @param profiles        The list of profiles to display.
         * @param currentDeviceId The device ID of the current user (to prevent self-deletion).
         * @param onActionStarted Callback for when a profile action starts.
         * @param onActionSuccess Callback for when a profile action succeeds.
         * @param onActionFailure Callback for when a profile action fails.
         */
        public ProfileAdapter(List<Entrant> profiles, String currentDeviceId, Runnable onActionStarted, Runnable onActionSuccess, Runnable onActionFailure) {
            this.profiles = profiles;
            this.currentDeviceId = currentDeviceId;
            this.onActionStarted = onActionStarted;
            this.onActionSuccess = onActionSuccess;
            this.onActionFailure = onActionFailure;
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

            holder.deleteButton.setOnClickListener(v -> {
                // Prevent admins from deleting themselves
                if (entrant.getDeviceId().equals(currentDeviceId)) {
                    Toast.makeText(holder.itemView.getContext(), "You cannot delete your own profile", Toast.LENGTH_SHORT).show();
                } else {
                    showDeleteConfirmation(holder.itemView, entrant);
                }
            });
        }

        /**
         * Shows a confirmation dialog before deleting a user profile.
         *
         * @param view    The view context for the dialog.
         * @param entrant The entrant profile to delete.
         */
        private void showDeleteConfirmation(View view, Entrant entrant) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_delete_profile, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            TextView title = dialogView.findViewById(R.id.tv_dialog_title);
            TextView message = dialogView.findViewById(R.id.tv_dialog_message);
            MaterialButton btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
            MaterialButton btnDelete = dialogView.findViewById(R.id.btn_dialog_delete);
            ImageView ivClose = dialogView.findViewById(R.id.iv_close);

            String roleName = entrant.getRole().name().toLowerCase();
            String fullName = entrant.getFirstName() + " " + entrant.getLastName();
            
            String messageText;
            if (entrant.getRole() == UserRole.ORGANIZER) {
                messageText = view.getContext().getString(R.string.delete_profile_message_organizer, roleName, fullName);
            } else {
                messageText = view.getContext().getString(R.string.delete_profile_message_user, roleName, fullName);
            }
            
            message.setText(Html.fromHtml(messageText, Html.FROM_HTML_MODE_LEGACY));

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            ivClose.setOnClickListener(v -> dialog.dismiss());

            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                performDeletion(view, entrant);
            });

            dialog.show();
        }

        /**
         * Performs the actual deletion of the user profile and associated data from the database.
         *
         * @param view    The view context for toast messages.
         * @param entrant The entrant profile to delete.
         */
        private void performDeletion(View view, Entrant entrant) {
            onActionStarted.run();
            DatabaseHandler dbHandler = new DatabaseHandler();
            dbHandler.deleteUserAccountCompletely(entrant.getDeviceId())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(view.getContext(), "User data and organized events deleted", Toast.LENGTH_SHORT).show();
                        onActionSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(view.getContext(), "Failed to delete user data", Toast.LENGTH_SHORT).show();
                        onActionFailure.run();
                    });
        }

        @Override
        public int getItemCount() {
            return profiles.size();
        }

        /**
         * ViewHolder for profile items in the RecyclerView.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, roleText, emailText, phoneText;
            MaterialButton deleteButton;

            /**
             * Initializes the ViewHolder with the item view and finds subviews.
             *
             * @param itemView The view for a single profile item.
             */
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
