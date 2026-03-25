package com.example.thevms.ui.Event;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.AttendeeItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    // Full unfiltered list — never modified after set
    private List<AttendeeItem> allAttendees = new ArrayList<>();
    // Currently displayed list — filtered by dropdown selection
    private List<AttendeeItem> filteredAttendees = new ArrayList<>();

    private String activeStatus = "waiting"; // default on load

    private OnCancelEntrantListener cancelListener;

    public interface OnCancelEntrantListener {
        void onCancel(AttendeeItem item);
    }

    public void setOnCancelEntrantListener(OnCancelEntrantListener listener) {
        this.cancelListener = listener;
    }

    /**
     * Called when Firestore data loads.
     * Stores the full list and applies the current active filter.
     */
    public void setAttendees(List<AttendeeItem> attendees) {
        this.allAttendees = attendees;
        applyFilter(activeStatus);
    }

    /**
     * Called when the organizer picks a status from the dropdown.
     * Filters the displayed list instantly.
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

    /**
     * Exports the currently filtered list as a CSV and opens the Android share sheet.
     * CSV columns: First Name, Last Name, Email, Status
     *
     * Called from OrganizerEventAdapter when tv_export_csv is tapped.
     *
     * @param context   Used for file creation and launching the share intent.
     * @param eventName Used to name the file e.g. "SummerFest_waiting.csv"
     */
    public void exportFilteredListAsCsv(Context context, String eventName) {
        if (filteredAttendees.isEmpty()) {
            Toast.makeText(context, "No entrants to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build CSV string
        StringBuilder csv = new StringBuilder();
        csv.append("First Name,Last Name,Email,Status\n");
        for (AttendeeItem item : filteredAttendees) {
            csv.append(escapeCsv(item.getEntrant().getFirstName())).append(",");
            csv.append(escapeCsv(item.getEntrant().getLastName())).append(",");
            csv.append(escapeCsv(item.getEntrant().getEmail())).append(",");
            csv.append(escapeCsv(item.getStatus())).append("\n");
        }

        // Write to a temp file in the app's cache directory
        // File name: "EventName_status.csv" with spaces replaced by underscores
        String safeEventName = eventName.replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = safeEventName + "_" + activeStatus + ".csv";
        File csvFile = new File(context.getCacheDir(), fileName);

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(csv.toString());
        } catch (IOException e) {
            Toast.makeText(context, "Failed to create CSV file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get a shareable URI via FileProvider (required for Android 7+)
        // Make sure your AndroidManifest.xml has a FileProvider with authority "${applicationId}.provider"
        Uri csvUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                csvFile
        );

        // Launch share sheet
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Entrant List - " + eventName + " (" + activeStatus + ")");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(shareIntent, "Export entrant list via...");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    /**
     * Wraps a CSV field in quotes and escapes any internal quotes.
     * Handles nulls safely.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        // If value contains a comma, newline, or quote — wrap in quotes and escape internal quotes
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

        // Set name
        String fullName = item.getEntrant().getFirstName() + " " + item.getEntrant().getLastName();
        holder.nameText.setText(fullName);

        // Set icon + tint based on status
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

        // Cancel button only visible for "selected" entrants (haven't decided yet)
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_attendee_name);
            statusIcon = itemView.findViewById(R.id.iv_status_icon);
            cancelBtn = itemView.findViewById(R.id.btn_cancel_entrant);
        }
    }
}
