package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ContactUsActivity extends AppCompatActivity {
    private static final String TAG = "ContactUsActivity";
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private Toast backToast;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactus);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Initialize UI elements
        bottomNav = findViewById(R.id.bottom_navigation);

        // Get username from Intent, falling back to SharedPreferences
        Intent intent = getIntent();
        final String username = intent.getStringExtra("username") != null && !intent.getStringExtra("username").isEmpty()
                ? intent.getStringExtra("username")
                : prefs.getString("username", "User");


        // Set up bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                Intent homeIntent = new Intent(ContactUsActivity.this, HomeActivity.class);
                homeIntent.putExtra("username", username);
                startActivity(homeIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.notification) {
                Intent notificationIntent = new Intent(ContactUsActivity.this, NotificationActivity.class);
                notificationIntent.putExtra("username", username);
                startActivity(notificationIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.appointment) {
                Intent appointmentIntent = new Intent(ContactUsActivity.this, AppointmentActivity.class);
                appointmentIntent.putExtra("username", username);
                startActivity(appointmentIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.contactus) {
                // Already on ContactUsActivity
                return true;
            } else if (itemId == R.id.settings) {
                Intent settingsIntent = new Intent(ContactUsActivity.this, SettingsActivity.class);
                settingsIntent.putExtra("username", username);
                startActivity(settingsIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });

        // Set default selection
        bottomNav.setSelectedItemId(R.id.contactus);
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            if (backToast != null) {
                backToast.cancel();
            }
            super.onBackPressed(); // Exit app
            finish();
        } else {
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}