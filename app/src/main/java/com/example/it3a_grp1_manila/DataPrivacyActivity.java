package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DataPrivacyActivity extends AppCompatActivity {

    private ImageView backButton;
    private BottomNavigationView bottomNav;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dataprivacy);

        // Get username from intent
        Intent intent = getIntent();
        if (intent != null) {
            username = intent.getStringExtra("username");
        }

        // Back button functionality
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.settings); // Highlight settings tab

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent navIntent = null;

            if (itemId == R.id.home) {
                navIntent = new Intent(DataPrivacyActivity.this, HomeActivity.class);
            } else if (itemId == R.id.notification) {
                navIntent = new Intent(DataPrivacyActivity.this, NotificationActivity.class);
            } else if (itemId == R.id.appointment) {
                navIntent = new Intent(DataPrivacyActivity.this, AppointmentActivity.class);
            } else if (itemId == R.id.contactus) {
                navIntent = new Intent(DataPrivacyActivity.this, ContactUsActivity.class);
            } else if (itemId == R.id.settings) {
                navIntent = new Intent(DataPrivacyActivity.this, SettingsActivity.class);
            }

            if (navIntent != null) {
                if (username != null) {
                    navIntent.putExtra("username", username);
                }
                startActivity(navIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });
    }
}
