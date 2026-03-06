package com.example.thevms.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.thevms.R;
import com.example.thevms.model.Entrant;
import com.example.thevms.model.UserRole;
import com.example.thevms.ui.Admin.AdminActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * The main activity of the application that manages fragment navigation and the admin drawer.
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Apply insets to the content container instead of the root DrawerLayout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Hide "Add" and "Admin" buttons by default until role is verified
        Menu menu = bottomNav.getMenu();
        MenuItem addItem = menu.findItem(R.id.nav_add);
        if (addItem != null) {
            addItem.setVisible(false);
        }
        MenuItem adminItem = menu.findItem(R.id.nav_admin_settings);
        if (adminItem != null) {
            adminItem.setVisible(false);
        }

        // Check user role and update UI
        checkUserRoleAndAdjustUI(bottomNav);

        // Initialize Admin Panel
        AdminActivity.init(this);

        // Set default fragment
        if (savedInstanceState == null) {
            navigateToFragment(new SearchFragment(), R.id.nav_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new SearchFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (id == R.id.nav_add) {
                selectedFragment = new CreateEventFragment();
            } else if (id == R.id.nav_admin_settings) {
                openAdminDrawer();
                return false; // Gear only opens the drawer, doesn't change selection by itself
            }

            if (selectedFragment != null) {
                navigateToFragment(selectedFragment, id);
                return true;
            }
            return false;
        });
    }

    /**
     * Navigates to the specified fragment and updates the bottom navigation selection.
     *
     * @param fragment   The fragment to display.
     * @param menuItemId The ID of the bottom navigation menu item to select.
     */
    public void navigateToFragment(Fragment fragment, int menuItemId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        if (bottomNav != null) {
            MenuItem item = bottomNav.getMenu().findItem(menuItemId);
            if (item != null) {
                item.setChecked(true);
            }
        }
    }

    /**
     * Opens the administrative side navigation drawer.
     */
    public void openAdminDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /**
     * Checks the user's role from the database and shows/hides role-specific buttons.
     */
    private void checkUserRoleAndAdjustUI(BottomNavigationView bottomNav) {
        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Entrant.getOrCreate(deviceId).addOnSuccessListener(entrant -> {
            UserRole role = entrant.getRole();
            if (role == UserRole.ORGANIZER || role == UserRole.ADMIN) {
                MenuItem addItem = bottomNav.getMenu().findItem(R.id.nav_add);
                if (addItem != null) {
                    addItem.setVisible(true);
                }
            }
            if (role == UserRole.ADMIN) {
                MenuItem adminItem = bottomNav.getMenu().findItem(R.id.nav_admin_settings);
                if (adminItem != null) {
                    adminItem.setVisible(true);
                }
            }
        });
    }
}
