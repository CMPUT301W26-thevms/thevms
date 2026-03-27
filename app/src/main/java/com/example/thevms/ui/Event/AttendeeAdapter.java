package com.example.thevms.ui.Event;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.AttendeeItem;
import com.example.thevms.model.DatabaseHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying a list of attendees (entrants) in a RecyclerView.
 * Provides filtering by status and CSV export functionality.
 */
public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    // Full unfiltered list — never modified after set
    private List<AttendeeItem> allAttendees = new ArrayList<>();
    // Currently displayed list — filtered by dropdown selection
    private List<AttendeeItem> filteredAttendees = new ArrayList<>();

    private String activeStatus = "waiting"; // default on load
    private String eventId;
    private String eventName;
    private String organizerId;
    private String organizerName;
    private DatabaseHandler dbHandler = new DatabaseHandler();

    private OnCancelEntrantListener cancelListener;

    /**
     * Interface for listening to cancellation events for an entrant.
     */
    public interface OnCancelEntrantListener {
        void onCancel(AttendeeItem item);
    }

    public void setOnCancelEntrantListener(OnCancelEntrantListener listener) {
        this.cancelListener = listener;
    }

    public void setEventContext(String eventId, String eventName, String organizerId, String organizerName) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
    }

    /**
     * Called when Firestore data loads.
     */
    public void setAttendees(List<AttendeeItem> attendees) {
        this.allAttendees = attendees;
        applyFilter(activeStatus);
    }

    /**
     * Called when the organizer picks a status from the dropdown.
     */
    public void filterByStatus(String status) {
        this.activeStatus = status;
        applyFilter(status);
    }

    private void applyFilter(String status) {
        filteredAttendees = new ArrayList<>();
        for (AttendeeItem item : allAttendees) {
            if (status.equals(item.getStatus())) {
                filteredAttendees.add(item);
            }
        }
        if (hasObservers()) notifyDataSetChanged();
    }

    public void exportFilteredListAsCsv(Context context, String eventName) {
        if (filteredAttendees.isEmpty()) {
            Toast.makeText(context, "No entrants to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("First Name,Last Name,Email,Status\n");
        for (AttendeeItem item : filteredAttendees) {
            csv.append(escapeCsv(item.getEntrant().getFirstName())).append(",");
            csv.append(escapeCsv(item.getEntrant().getLastName())).append(",");
            csv.append(escapeCsv(item.getEntrant().getEmail())).append(",");
            csv.append(escapeCsv(item.getStatus())).append("\n");
        }

        String safeEventName = eventName.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = safeEventName + "_" + activeStatus + ".csv";
        File csvFile = new File(context.getCacheDir(), fileName);

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(csv.toString());
        } catch (IOException e) {
            Toast.makeText(context, "Failed to create CSV file", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri csvUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                csvFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Entrant List - " + eventName + " (" + activeStatus + ")");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, "Export entrant list via...");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendeeItem item = filteredAttendees.get(position);

        String fullName = item.getEntrant().getFirstName() + " " + item.getEntrant().getLastName();
        holder.nameText.setText(fullName);

        int iconRes;
        int tintColor;

        switch (item.getStatus()) {
            case "selected":
                iconRes = R.drawable.ic_status_selected;
                tintColor = R.color.status_selected;
                break;
            case "accepted":
                iconRes = R.drawable.ic_status_accepted;
                tintColor = R.color.status_accepted;
                break;
            case "rejected":
                iconRes = R.drawable.ic_status_rejected;
                tintColor = R.color.status_rejected;
                break;
            case "cancelled":
                iconRes = R.drawable.ic_status_cancelled;
                tintColor = R.color.status_cancelled;
                break;
            case "waiting":
            default:
                iconRes = R.drawable.ic_status_waitlisted;
                tintColor = R.color.status_waitlisted;
                break;
        }

        holder.statusIcon.setImageResource(iconRes);
        holder.statusIcon.setImageTintList(
                ContextCompat.getColorStateList(holder.itemView.getContext(), tintColor)
        );

        // Show "Assign as Co-organizer" button for waiting or selected entrants
        if (eventId != null && ("waiting".equals(item.getStatus()) || "selected".equals(item.getStatus()))) {
            holder.assignCoOrganizerBtn.setVisibility(View.VISIBLE);
            holder.assignCoOrganizerBtn.setOnClickListener(v -> {
                String safeEventName = eventName != null ? eventName : "Event";
                String safeOrganizerId = organizerId != null ? organizerId : getDeviceId(holder.itemView.getContext());
                String safeOrganizerName = organizerName != null ? organizerName : "Organizer";
                String receiverName = buildDisplayName(item);

                if (safeOrganizerId == null) {
                    Toast.makeText(holder.itemView.getContext(), "Missing organizer info", Toast.LENGTH_SHORT).show();
                    return;
                }

                dbHandler.assignCoOrganizer(eventId, safeEventName, safeOrganizerId, safeOrganizerName,
                                item.getEntrant().getDeviceId(), receiverName)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(), "Assigned " + item.getEntrant().getFirstName() + " as co-organizer", Toast.LENGTH_SHORT).show();
                            // Item will be filtered out next refresh due to status change
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        } else {
            holder.assignCoOrganizerBtn.setVisibility(View.GONE);
        }

        if (item.isCancellable()) {
            holder.cancelBtn.setVisibility(View.VISIBLE);
            holder.cancelBtn.setOnClickListener(v -> {
                if (cancelListener != null) cancelListener.onCancel(item);
            });
        } else {
            holder.cancelBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return filteredAttendees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        ImageView statusIcon;
        Button cancelBtn;
        ImageButton assignCoOrganizerBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_attendee_name);
            statusIcon = itemView.findViewById(R.id.iv_status_icon);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_entrant);
            assignCoOrganizerBtn = itemView.findViewById(R.id.btn_assign_co_organizer);
        }
    }

    private String buildDisplayName(AttendeeItem item) {
        String first = item.getEntrant().getFirstName();
        String last = item.getEntrant().getLastName();
        if (first != null && last != null) {
            return first + " " + last;
        } else if (first != null) {
            return first;
        } else if (last != null) {
            return last;
        } else {
            return item.getEntrant().getEmail() != null ? item.getEntrant().getEmail() : "Entrant";
        }
    }

    @SuppressLint("HardwareIds")
    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
