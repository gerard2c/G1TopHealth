package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SecuritySettingsActivity extends AppCompatActivity {

    private ImageView backButton;
    private BottomNavigationView bottomNav;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_securitysettings);

        backButton = findViewById(R.id.back_button);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Get the username from intent
        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        if (username == null) username = ""; // fallback

        // Back button to previous screen
        backButton.setOnClickListener(v -> finish());

        // Change Password section
        LinearLayout changePasswordSection = findViewById(R.id.change_password_section);
        if (changePasswordSection != null) {
            changePasswordSection.setOnClickListener(v -> {
                Intent intentChangePassword = new Intent(SecuritySettingsActivity.this, ChangePasswordActivity.class);
                intentChangePassword.putExtra("username", username);
                startActivity(intentChangePassword);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        bottomNav.setSelectedItemId(R.id.settings); // Highlight settings tab

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent navIntent = null;

            if (itemId == R.id.home) {
                navIntent = new Intent(SecuritySettingsActivity.this, HomeActivity.class);
            } else if (itemId == R.id.notification) {
                navIntent = new Intent(SecuritySettingsActivity.this, NotificationActivity.class);
            } else if (itemId == R.id.appointment) {
                navIntent = new Intent(SecuritySettingsActivity.this, AppointmentActivity.class);
            } else if (itemId == R.id.contactus) {
                navIntent = new Intent(SecuritySettingsActivity.this, ContactUsActivity.class);
            } else if (itemId == R.id.settings) {
                navIntent = new Intent(SecuritySettingsActivity.this, SettingsActivity.class);
            }

            if (navIntent != null) {
                navIntent.putExtra("username", username);
                startActivity(navIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });
    }
}
