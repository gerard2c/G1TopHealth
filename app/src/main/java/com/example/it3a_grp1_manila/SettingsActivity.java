package com.example.it3a_grp1_manila;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import com.yalantis.ucrop.UCrop;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int UCROP_REQUEST_CODE = 69;

    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private Toast backToast;
    private LinearLayout logoutButton, securitySettingsSection, helpSupportSection, dataPrivacySection, archiveSection;
    private TextView userFullName, editProfile;
    private ProgressBar progressBar;
    private ImageView profileImage;
    private DatabaseReference usersRef;
    private SharedPreferences prefs;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize UI components
        bottomNav = findViewById(R.id.bottom_navigation);
        logoutButton = findViewById(R.id.logout_button);
        userFullName = findViewById(R.id.user_full_name);
        editProfile = findViewById(R.id.edit_profile);
        progressBar = findViewById(R.id.progress_bar);
        profileImage = findViewById(R.id.profile_image);
        securitySettingsSection = findViewById(R.id.security_settings_section);
        helpSupportSection = findViewById(R.id.help_support_section);
        dataPrivacySection = findViewById(R.id.data_privacy_section);
        archiveSection = findViewById(R.id.archives_section); // Add this ID to your XML layout

        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Get username
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = prefs.getString("username", null);
        }

        if (username == null) {
            Toast.makeText(this, "No username found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String finalUsername = username;
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Load user data
        loadUserProfile(finalUsername);

        // Profile image click
        profileImage.setOnClickListener(v -> openImagePicker());

        // Edit profile click
        editProfile.setOnClickListener(v -> {
            Intent intentEdit = new Intent(SettingsActivity.this, EditProfileActivity.class);
            intentEdit.putExtra("username", finalUsername);
            startActivity(intentEdit);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Logout
        logoutButton.setOnClickListener(v -> showLogoutDialog());

        // Navigation clicks
        if (securitySettingsSection != null) {
            securitySettingsSection.setOnClickListener(v -> {
                Intent i = new Intent(this, SecuritySettingsActivity.class);
                i.putExtra("username", finalUsername);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        if (helpSupportSection != null) {
            helpSupportSection.setOnClickListener(v -> {
                startActivity(new Intent(this, HelpActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        if (dataPrivacySection != null) {
            dataPrivacySection.setOnClickListener(v -> {
                startActivity(new Intent(this, DataPrivacyActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        if (archiveSection != null) {
            archiveSection.setOnClickListener(v -> {
                Intent i = new Intent(this, ArchivesActivity.class);
                i.putExtra("username", finalUsername);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        // Bottom navigation
        bottomNav.setSelectedItemId(R.id.settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent navIntent = null;
            if (id == R.id.home) navIntent = new Intent(this, HomeActivity.class);
            else if (id == R.id.notification) navIntent = new Intent(this, NotificationActivity.class);
            else if (id == R.id.appointment) navIntent = new Intent(this, AppointmentActivity.class);
            else if (id == R.id.contactus) navIntent = new Intent(this, ContactUsActivity.class);
            else if (id == R.id.settings) return true;

            if (navIntent != null) {
                navIntent.putExtra("username", finalUsername);
                startActivity(navIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void loadUserProfile(String username) {
        progressBar.setVisibility(View.VISIBLE);
        usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                userId = userSnap.getKey();
                                String firstName = userSnap.child("firstName").getValue(String.class);
                                String lastName = userSnap.child("lastName").getValue(String.class);
                                String profileBase64 = userSnap.child("profileImageBase64").getValue(String.class);

                                userFullName.setText(firstName + " " + lastName);

                                if (profileBase64 != null && !profileBase64.isEmpty()) {
                                    byte[] decoded = Base64.decode(profileBase64, Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                    profileImage.setImageBitmap(bitmap);
                                } else {
                                    profileImage.setImageResource(R.drawable.ic_profile_default);
                                }
                            }
                        } else {
                            userFullName.setText("User not found");
                            profileImage.setImageResource(R.drawable.ic_profile_default);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        userFullName.setText("Error");
                        profileImage.setImageResource(R.drawable.ic_profile_default);
                        Toast.makeText(SettingsActivity.this, "Database error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openImagePicker() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK);
        pickIntent.setType("image/*");
        startActivityForResult(pickIntent, PICK_IMAGE_REQUEST);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();
                    startActivity(new Intent(this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            Uri sourceUri = data.getData();
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg"));

            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512)
                    .start(this);
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null && userId != null) {
                try {
                    InputStream input = getContentResolver().openInputStream(resultUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        profileImage.setImageBitmap(bitmap);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                        usersRef.child(userId).child("profileImageBase64").setValue(base64Image);
                    }
                    if (input != null) input.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing cropped image", e);
                    Toast.makeText(this, "Error saving profile picture", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            if (backToast != null) backToast.cancel();
            super.onBackPressed();
        } else {
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
            backPressedTime = System.currentTimeMillis();
        }
    }
}
