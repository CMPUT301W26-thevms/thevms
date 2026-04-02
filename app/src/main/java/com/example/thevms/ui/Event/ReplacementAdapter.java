package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;

import java.util.ArrayList;
import java.util.List;

public class ReplacementAdapter extends RecyclerView.Adapter<ReplacementAdapter.ReplacementViewHolder> {

    static class ReplacementItem {
        final Entrant entrant;
        boolean selected;

        ReplacementItem(Entrant entrant) {
            this.entrant = entrant;
        }
    }

    private final List<ReplacementItem> items = new ArrayList<>();
    private int maxSelectable = 0;

    void setItems(List<ReplacementItem> replacements, int maxSelectable) {
        items.clear();
        items.addAll(replacements);
        this.maxSelectable = Math.max(0, maxSelectable);
        notifyDataSetChanged();
    }

    List<ReplacementItem> getSelectedItems() {
        List<ReplacementItem> selected = new ArrayList<>();
        for (ReplacementItem item : items) {
            if (item.selected) {
                selected.add(item);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ReplacementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_replacement_entry, parent, false);
        return new ReplacementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplacementViewHolder holder, int position) {
        ReplacementItem item = items.get(position);
        String fullName = (item.entrant.getFirstName() != null ? item.entrant.getFirstName() : "") +
                " " + (item.entrant.getLastName() != null ? item.entrant.getLastName() : "");
        holder.nameText.setText(fullName.trim().isEmpty() ? "Unnamed Entrant" : fullName);
        holder.checkBox.setChecked(item.selected);
        holder.checkBox.setEnabled(maxSelectable > 0);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int selectedCount = getSelectedItems().size();
            if (isChecked && selectedCount >= maxSelectable) {
                buttonView.setChecked(false);
            } else {
                item.selected = isChecked;
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReplacementViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        CheckBox checkBox;

        ReplacementViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_replacement_name);
            checkBox = itemView.findViewById(R.id.cb_replacement_select);
        }
    }
}
