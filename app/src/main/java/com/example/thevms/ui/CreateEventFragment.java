package com.example.thevms.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;

/**
 * Fragment for creating a new event.
 */
public class CreateEventFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup cancel button to show custom confirmation dialog
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> showCancelConfirmationDialog());

        // Confirm button placeholder
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            // Logic for saving event will go here
        });
    }

    /**
     * Displays a custom confirmation dialog asking the user if they are sure they want to cancel.
     */
    private void showCancelConfirmationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancel_confirmation, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(dialogView)
                .create();

        ImageView closeIcon = dialogView.findViewById(R.id.iv_close);
        Button backButton = dialogView.findViewById(R.id.btn_dialog_back);
        Button yesButton = dialogView.findViewById(R.id.btn_dialog_yes);

        closeIcon.setOnClickListener(v -> dialog.dismiss());
        backButton.setOnClickListener(v -> dialog.dismiss());

        yesButton.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                getParentFragmentManager().popBackStack();
            } catch (IllegalStateException e) {
                Log.e("CreateEventFragment", "Error while popping back stack", e);
            }
        });

        dialog.show();
    }
}
