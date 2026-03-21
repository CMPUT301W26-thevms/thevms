package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying search results of entrants that can be invited to an event.
 */
public class InviteEntrantAdapter extends RecyclerView.Adapter<InviteEntrantAdapter.ViewHolder> {

    private List<Entrant> entrants = new ArrayList<>();
    private OnInviteClickListener inviteClickListener;

    public interface OnInviteClickListener {
        void onInvite(Entrant entrant);
    }

    public void setEntrants(List<Entrant> entrants) {
        this.entrants = entrants;
        notifyDataSetChanged();
    }

    public void setOnInviteClickListener(OnInviteClickListener inviteClickListener) {
        this.inviteClickListener = inviteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invite_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);
        holder.nameText.setText(entrant.getFirstName() + " " + entrant.getLastName());
        
        String contactInfo = "";
        if (entrant.getEmail() != null) contactInfo += entrant.getEmail();
        if (entrant.getPhoneNumber() != null) {
            if (!contactInfo.isEmpty()) contactInfo += " | ";
            contactInfo += entrant.getPhoneNumber();
        }
        holder.contactText.setText(contactInfo);

        holder.inviteButton.setOnClickListener(v -> {
            if (inviteClickListener != null) {
                inviteClickListener.onInvite(entrant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, contactText;
        Button inviteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_entrant_name);
            contactText = itemView.findViewById(R.id.tv_entrant_contact);
            inviteButton = itemView.findViewById(R.id.btn_send_invite);
        }
    }
}
