package com.example.thevms.ui.Event;

import android.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Factory class responsible for inflating and binding data to event card views.
 */
public class EventViewFactory {

    /**
     * Inflates an event card layout and populates it with data from the provided Event object.
     *
     * @param parent  The parent view group used for layout inflation context.
     * @param event   The event model containing the data to display.
     * @param isAdmin Whether the current user has administrative privileges.
     * @return A View instance representing the populated event card.
     */
    public static View createEventCard(ViewGroup parent, Event event, boolean isAdmin) {
        View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);

        TextView nameView = card.findViewById(R.id.event_name);
        TextView statusView = card.findViewById(R.id.event_status_info);
        TextView timeView = card.findViewById(R.id.event_time_info);
        TextView locationView = card.findViewById(R.id.event_location_info);
        Button removeBtn = card.findViewById(R.id.btn_remove_event);

        if (nameView != null) {
            nameView.setText(event.getName());
        }

        if (statusView != null) {
            int count = (event.getEntrantList() != null) ? event.getEntrantList().size() : 0;
            statusView.setText(String.format(Locale.getDefault(), "☆ %d people joined", count));
        }

        if (timeView != null) {
            if (event.getEventStartTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                timeView.setText(String.format("Starts %s", sdf.format(event.getEventStartTime())));
            } else {
                timeView.setText("Time TBD");
            }
        }

        if (locationView != null) {
            // TODO: Replace with event.getLocation() if available in your model
            locationView.setText("📍 Nearby");
        }

        if (isAdmin && removeBtn != null) {
            removeBtn.setVisibility(View.VISIBLE);
            removeBtn.setOnClickListener(v -> showDeleteConfirmation(card, event, parent));
        }

        return card;
    }

    private static void showDeleteConfirmation(View cardView, Event event, ViewGroup parent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cardView.getContext());
        View dialogView = LayoutInflater.from(cardView.getContext()).inflate(R.layout.dialog_delete_event, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView title = dialogView.findViewById(R.id.tv_dialog_title);
        TextView message = dialogView.findViewById(R.id.tv_dialog_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnDelete = dialogView.findViewById(R.id.btn_dialog_delete);
        ImageView ivClose = dialogView.findViewById(R.id.iv_close);

        String formattedTitle = cardView.getContext().getString(R.string.delete_event_question_formatted, event.getName());
        title.setText(Html.fromHtml(formattedTitle, Html.FROM_HTML_MODE_LEGACY));

        message.setText(R.string.delete_event_consequence);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        ivClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            deleteEventFromDb(cardView, event, parent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void deleteEventFromDb(View cardView, Event event, ViewGroup parent) {
        DatabaseHandler dbHandler = new DatabaseHandler();
        dbHandler.deleteEvent(String.valueOf(event.getEventId()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(cardView.getContext(), "Event removed", Toast.LENGTH_SHORT).show();
                    parent.removeView(cardView);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventViewFactory", "Error deleting event", e);
                    Toast.makeText(cardView.getContext(), "Failed to remove event", Toast.LENGTH_LONG).show();
                });
    }
}
