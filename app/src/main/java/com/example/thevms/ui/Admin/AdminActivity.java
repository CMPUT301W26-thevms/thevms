package com.example.thevms.ui.Admin;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.thevms.R;
import com.example.thevms.ui.MainActivity;
import com.example.thevms.ui.SearchFragment;

/**
 * Controller class for handling Admin Panel logic and navigation.
 */
public class AdminActivity {

    private final Activity activity;
    private final DrawerLayout drawerLayout;

    public AdminActivity(Activity activity) {
        this.activity = activity;
        this.drawerLayout = activity.findViewById(R.id.drawer_layout);
        setupClickListeners();
    }

    /**
     * Initializes the Admin Panel logic for the given activity.
     *
     * @param activity The main activity hosting the drawer.
     */
    public static void init(Activity activity) {
        new AdminActivity(activity);
    }

    private void setupClickListeners() {
        View panel = activity.findViewById(R.id.admin_panel_root);
        if (panel == null) return;

        panel.findViewById(R.id.admin_manage_event).setOnClickListener(v -> handleAction("Manage Event"));
        panel.findViewById(R.id.admin_manage_profiles).setOnClickListener(v -> handleAction("Manage Profiles"));
        panel.findViewById(R.id.admin_manage_images).setOnClickListener(v -> handleAction("Manage Images"));
        panel.findViewById(R.id.admin_view_logs).setOnClickListener(v -> handleAction("View Logs"));
        panel.findViewById(R.id.admin_manage_organizers).setOnClickListener(v -> handleAction("Manage Organizers"));
    }

    private void handleAction(String actionName) {
        Log.d("AdminActivity", "Action clicked: " + actionName);

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;

            if ("Manage Event".equals(actionName)) {
                mainActivity.navigateToFragment(new AdminEventsFragment(), R.id.nav_admin_settings);
            } else if ("Manage Profiles".equals(actionName)) {
                mainActivity.navigateToFragment(AdminProfilesFragment.newInstance(false), R.id.nav_admin_settings);
            } else if ("Manage Organizers".equals(actionName)) {
                mainActivity.navigateToFragment(AdminProfilesFragment.newInstance(true), R.id.nav_admin_settings);
            } else {
                Toast.makeText(activity, actionName + " coming soon", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
