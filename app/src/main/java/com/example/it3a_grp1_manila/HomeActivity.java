package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private TextView titleHome;
    private Button bookButton, knowMore, discover, learnMore, book, discover1;
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private Toast backToast;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_1);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Initialize UI elements
        titleHome = findViewById(R.id.title_home);
        bookButton = findViewById(R.id.bookButton);
        knowMore = findViewById(R.id.knowMore);
        discover = findViewById(R.id.discover);
        learnMore = findViewById(R.id.learnMore);
        book = findViewById(R.id.book);
        discover1 = findViewById(R.id.discover1);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Get username from Intent, falling back to SharedPreferences
        Intent intent = getIntent();
        final String username = intent.getStringExtra("username") != null && !intent.getStringExtra("username").isEmpty()
                ? intent.getStringExtra("username")
                : prefs.getString("username", "User");

        // Set up click listeners for buttons
        bookButton.setOnClickListener(v -> {
            Intent appointmentIntent = new Intent(HomeActivity.this, AppointmentActivity.class);
            appointmentIntent.putExtra("username", username); // Add username extra
            startActivity(appointmentIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        knowMore.setOnClickListener(v -> {
            Intent knowMoreIntent = new Intent(HomeActivity.this, KnowMoreDermaActivity.class);
            startActivity(knowMoreIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        discover.setOnClickListener(v -> {
            Intent discoverIntent = new Intent(HomeActivity.this, ServicesActivity.class);
            startActivity(discoverIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        learnMore.setOnClickListener(v -> {
            Intent learnMoreIntent = new Intent(HomeActivity.this, LearnMoreHMOActivity.class);
            startActivity(learnMoreIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        book.setOnClickListener(v -> {
            Intent appointmentIntent = new Intent(HomeActivity.this, AppointmentActivity.class);
            appointmentIntent.putExtra("username", username); // Add username extra
            startActivity(appointmentIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        discover1.setOnClickListener(v -> {
            Intent discover1Intent = new Intent(HomeActivity.this, DiscoverMoreActivity.class);
            startActivity(discover1Intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Set up bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                // Already on HomeActivity
                return true;
            } else if (itemId == R.id.notification) {
                Intent notificationIntent = new Intent(HomeActivity.this, NotificationActivity.class);
                notificationIntent.putExtra("username", username);
                startActivity(notificationIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.appointment) {
                Intent appointmentIntent = new Intent(HomeActivity.this, AppointmentActivity.class);
                appointmentIntent.putExtra("username", username);
                startActivity(appointmentIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.contactus) {
                Intent contactUsIntent = new Intent(HomeActivity.this, ContactUsActivity.class);
                contactUsIntent.putExtra("username", username);
                startActivity(contactUsIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (itemId == R.id.settings) {
                Intent settingsIntent = new Intent(HomeActivity.this, SettingsActivity.class);
                settingsIntent.putExtra("username", username);
                startActivity(settingsIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });

        // Set default selection
        bottomNav.setSelectedItemId(R.id.home);
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