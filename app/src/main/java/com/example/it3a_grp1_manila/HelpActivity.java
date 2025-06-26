package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HelpActivity extends AppCompatActivity {

    private ImageView backButton;

    // FAQ views
    private TextView titleText1, answer1;
    private TextView titleText2, answer2;
    private TextView titleText3, answer3;
    private BottomNavigationView bottomNav;

    private String username; // to pass between screens

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Get username from intent
        username = getIntent().getStringExtra("username");

        // Back button
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // FAQ
        titleText1 = findViewById(R.id.titleText1);
        answer1 = findViewById(R.id.answer1);
        titleText2 = findViewById(R.id.titleText2);
        answer2 = findViewById(R.id.answer2);
        titleText3 = findViewById(R.id.titleText3);
        answer3 = findViewById(R.id.answer3);

        titleText1.setOnClickListener(v -> toggleVisibility(answer1));
        titleText2.setOnClickListener(v -> toggleVisibility(answer2));
        titleText3.setOnClickListener(v -> toggleVisibility(answer3));

        // Bottom navigation setup
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.settings); // highlight settings tab

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent navIntent = null;

            if (itemId == R.id.home) {
                navIntent = new Intent(HelpActivity.this, HomeActivity.class);
            } else if (itemId == R.id.notification) {
                navIntent = new Intent(HelpActivity.this, NotificationActivity.class);
            } else if (itemId == R.id.appointment) {
                navIntent = new Intent(HelpActivity.this, AppointmentActivity.class);
            } else if (itemId == R.id.contactus) {
                navIntent = new Intent(HelpActivity.this, ContactUsActivity.class);
            } else if (itemId == R.id.settings) {
                navIntent = new Intent(HelpActivity.this, SettingsActivity.class);
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

    // Toggle view visibility helper
    private void toggleVisibility(View view) {
        view.setVisibility(view.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }
}
