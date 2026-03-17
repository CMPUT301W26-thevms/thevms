package com.example.thevms.ui.Event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.firstNameTextView.setText(comment.getFirstName());
        holder.lastNameTextView.setText(comment.getLastName());
        holder.textTextView.setText(comment.getText());
        if (comment.getTimestamp() != null) {
            holder.timeTextView.setText(dateFormat.format(comment.getTimestamp()));
        } else {
            holder.timeTextView.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView firstNameTextView, lastNameTextView, textTextView, timeTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            firstNameTextView = itemView.findViewById(R.id.comment_first_name);
            lastNameTextView = itemView.findViewById(R.id.comment_last_name);
            textTextView = itemView.findViewById(R.id.comment_text);
            timeTextView = itemView.findViewById(R.id.comment_time);
        }
    }
}
