package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;

import java.util.ArrayList;
import java.util.List;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

    private List<Entrant> attendees = new ArrayList<>();

    public void setAttendees(List<Entrant> attendees) {
        this.attendees = attendees;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant entrant = attendees.get(position);
        String fullName = entrant.getFirstName() + " " + entrant.getLastName();
        holder.nameText.setText(fullName);
    }

    @Override
    public int getItemCount() {
        return attendees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_attendee_name);
        }
    }
}
