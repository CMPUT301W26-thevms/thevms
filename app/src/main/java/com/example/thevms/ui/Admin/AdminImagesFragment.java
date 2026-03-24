package com.example.thevms.ui.Admin;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thevms.R;
import com.example.thevms.model.DatabaseHandler;
import com.example.thevms.model.Event;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for administrators to view and manage all images in the system.
 * Displays images from event posters and user profiles.
 */
public class AdminImagesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar loadingSpinner;
    private ImageAdapter adapter;
    private final List<AdminImageItem> imageItems = new ArrayList<>();
    private DatabaseHandler dbHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_images, container, false);

        recyclerView = view.findViewById(R.id.images_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        loadingSpinner = view.findViewById(R.id.loading_spinner);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ImageAdapter(imageItems, this::deleteImage);
        recyclerView.setAdapter(adapter);

        dbHandler = new DatabaseHandler();
        loadImages();

        return view;
    }

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

    private void loadImages() {
        setLoading(true);
        imageItems.clear();

        // Load images from Events
        dbHandler.getAllEvents().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Event event = Event.fromDoc(doc);
                if (event != null && event.getPhoto() != null) {
                    imageItems.add(new AdminImageItem(
                            event.getPhoto(),
                            "Event Poster",
                            event.getName(),
                            String.valueOf(event.getEventId()),
                            DatabaseHandler.COLLECTION_EVENTS
                    ));
                }
            }

            // Check for profile pictures in Users Note - Soon to support
            dbHandler.getAllUsers().addOnSuccessListener(userSnapshots -> {
                for (DocumentSnapshot doc : userSnapshots) {
                    if (doc.contains("photo")) {
                        byte[] photo = null;
                        Object photoObj = doc.get("photo");
                        if (photoObj instanceof com.google.firebase.firestore.Blob) {
                            photo = ((com.google.firebase.firestore.Blob) photoObj).toBytes();
                        }

                        if (photo != null) {
                            String firstName = doc.getString("firstName");
                            String lastName = doc.getString("lastName");
                            String name = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                            if (name.trim().isEmpty()) name = "User: " + doc.getId();

                            imageItems.add(new AdminImageItem(
                                    photo,
                                    "Profile Picture",
                                    name,
                                    doc.getId(),
                                    DatabaseHandler.COLLECTION_USERS
                            ));
                        }
                    }
                }
                setLoading(false);
            }).addOnFailureListener(e -> {
                setLoading(false);
                Log.e("AdminImagesFragment", "Error loading user images", e);
            });

        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(getContext(), "Failed to load event images", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (imageItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void deleteImage(AdminImageItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_image, null);
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

        if (title != null) title.setText("Delete Image?");
        if (message != null)
            message.setText("Are you sure you want to remove this image from " + item.sourceName + "? This action cannot be undone.");

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("photo", null);

                dbHandler.getDb().collection(item.collection)
                        .document(item.documentId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Image deleted", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadImages();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (ivClose != null) ivClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static class AdminImageItem {
        byte[] imageData;
        String type;
        String sourceName;
        String documentId;
        String collection;

        AdminImageItem(byte[] imageData, String type, String sourceName, String documentId, String collection) {
            this.imageData = imageData;
            this.type = type;
            this.sourceName = sourceName;
            this.documentId = documentId;
            this.collection = collection;
        }
    }

    private interface OnDeleteClickListener {
        void onDelete(AdminImageItem item);
    }

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final List<AdminImageItem> items;
        private final OnDeleteClickListener deleteListener;

        ImageAdapter(List<AdminImageItem> items, OnDeleteClickListener deleteListener) {
            this.items = items;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AdminImageItem item = items.get(position);

            if (item.imageData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(item.imageData, 0, item.imageData.length);
                holder.imageView.setImageBitmap(bitmap);
            }

            holder.typeText.setText(item.type);
            holder.sourceText.setText(item.sourceName);

            holder.deleteButton.setOnClickListener(v -> deleteListener.onDelete(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView typeText, sourceText;
            MaterialButton deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.admin_image_view);
                typeText = itemView.findViewById(R.id.admin_image_type);
                sourceText = itemView.findViewById(R.id.admin_image_source);
                deleteButton = itemView.findViewById(R.id.btn_delete_image);
            }
        }
    }
}
