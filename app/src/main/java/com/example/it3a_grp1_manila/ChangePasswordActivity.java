package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.*;
import androidx.appcompat.app.AppCompatActivity;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText newPasswordInput, confirmPasswordInput;
    private Button changePasswordButton;
    private DatabaseReference usersRef;
    private String username;
    private String userId;
    private ImageView backButton;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changepassword);

        // Firebase reference
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // UI components
        newPasswordInput = findViewById(R.id.et_new_password);
        confirmPasswordInput = findViewById(R.id.et_confirm_password);
        changePasswordButton = findViewById(R.id.btn_change_password);
        backButton = findViewById(R.id.back_button);

        // Back button functionality
        backButton.setOnClickListener(v -> {
            finish();
        });

        // Username from intent
        Intent intent = getIntent();
        username = intent.getStringExtra("username");

        // Retrieve userId using username
        usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            userId = userSnap.getKey();
                            break;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ChangePasswordActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
                    }
                });

        // Handle password change
        changePasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId != null) {
                usersRef.child(userId).child("password").setValue(newPassword);
                Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Bottom navigation setup
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.settings); // highlight settings tab

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent navIntent = null;

            if (itemId == R.id.home) {
                navIntent = new Intent(ChangePasswordActivity.this, HomeActivity.class);
            } else if (itemId == R.id.notification) {
                navIntent = new Intent(ChangePasswordActivity.this, NotificationActivity.class);
            } else if (itemId == R.id.appointment) {
                navIntent = new Intent(ChangePasswordActivity.this, AppointmentActivity.class);
            } else if (itemId == R.id.contactus) {
                navIntent = new Intent(ChangePasswordActivity.this, ContactUsActivity.class);
            } else if (itemId == R.id.settings) {
                navIntent = new Intent(ChangePasswordActivity.this, SettingsActivity.class);
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
