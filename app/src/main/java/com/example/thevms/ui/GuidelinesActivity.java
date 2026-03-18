package com.example.thevms.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.thevms.R;

/**
 * Activity for displaying application guidelines.
 */
public class GuidelinesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guidelines);

        View root = findViewById(R.id.tv_guidelines_title).getParent().getParent() instanceof View ? (View) findViewById(R.id.tv_guidelines_title).getParent().getParent() : findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button dismissButton = findViewById(R.id.btn_dismiss);
        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> finish());
        }
    }
}
