package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ArchivesActivity extends AppCompatActivity {

    private static final String TAG = "ArchivesActivity";
    private ImageView backButton;
    private Spinner spinnerDateFilter, spinnerStatusFilter;
    private BottomNavigationView bottomNav;
    private LinearLayout archiveContainer;
    private String username, userId;
    private DatabaseReference usersRef, userArchivesRef, adminArchivesRef;
    private boolean isAdmin;

    private final String[] dateFilterOptions = {
            "All Dates",
            "Today",
            "Last 7 Days",
            "Last 30 Days"
    };

    private final String[] statusFilterOptions = {
            "All",
            "Confirmed",
            "Unconfirmed"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archives);

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        if (username == null) {
            Log.e(TAG, "Username not provided in intent");
            Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            usersRef = database.getReference("users");
            userArchivesRef = database.getReference("userArchives");
            adminArchivesRef = database.getReference("adminArchives");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing database", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton = findViewById(R.id.back_button);
        spinnerDateFilter = findViewById(R.id.spinner_date_filter);
        spinnerStatusFilter = findViewById(R.id.spinner_status_filter);
        archiveContainer = findViewById(R.id.archive_container);
        bottomNav = findViewById(R.id.bottom_navigation);

        if (backButton == null || spinnerDateFilter == null || spinnerStatusFilter == null || archiveContainer == null || bottomNav == null) {
            Log.e(TAG, "UI elements not found");
            Toast.makeText(this, "Error initializing UI", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(v -> finish());

        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, dateFilterOptions);
        dateAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerDateFilter.setAdapter(dateAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, statusFilterOptions);
        statusAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerStatusFilter.setAdapter(statusAdapter);

        fetchUserIdAndAdminStatus();

        bottomNav.setSelectedItemId(R.id.settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent navIntent = null;

            if (itemId == R.id.home) {
                navIntent = new Intent(ArchivesActivity.this, HomeActivity.class);
            } else if (itemId == R.id.notification) {
                navIntent = new Intent(ArchivesActivity.this, NotificationActivity.class);
            } else if (itemId == R.id.appointment) {
                navIntent = new Intent(ArchivesActivity.this, AppointmentActivity.class);
            } else if (itemId == R.id.contactus) {
                navIntent = new Intent(ArchivesActivity.this, ContactUsActivity.class);
            } else if (itemId == R.id.settings) {
                navIntent = new Intent(ArchivesActivity.this, SettingsActivity.class);
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

    private void fetchUserIdAndAdminStatus() {
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        userId = userSnapshot.getKey();
                        Boolean isAdminValue = userSnapshot.child("isAdmin").getValue(Boolean.class);
                        isAdmin = isAdminValue != null && isAdminValue;
                        Log.d(TAG, "Fetched userId: " + userId + ", isAdmin: " + isAdmin + ", Username: " + username);
                        if (username.equals("adminTopHealth") && !isAdmin) {
                            Log.e(TAG, "adminTopHealth is not set as admin");
                            Toast.makeText(ArchivesActivity.this, "Admin privileges not detected for adminTopHealth", Toast.LENGTH_LONG).show();
                        }
                        setupFilterListeners();
                        loadArchives();
                    }
                } else {
                    Log.e(TAG, "User not found for username: " + username);
                    Toast.makeText(ArchivesActivity.this, "User " + username + " not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching user data: " + error.getMessage(), error.toException());
                Toast.makeText(ArchivesActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupFilterListeners() {
        spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadArchives();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadArchives();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadArchives() {
        if (userId == null) {
            Log.w(TAG, "UserId not available yet, delaying load");
            return;
        }
        archiveContainer.removeAllViews();
        String selectedDateFilter = spinnerDateFilter.getSelectedItem().toString();
        String selectedStatusFilter = spinnerStatusFilter.getSelectedItem().toString();

        DatabaseReference archivesRef = isAdmin ? adminArchivesRef : userArchivesRef.child(userId);
        archivesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                archiveContainer.removeAllViews();
                boolean hasArchives = false;

                for (DataSnapshot archiveSnapshot : snapshot.getChildren()) {
                    String archiveId = archiveSnapshot.getKey();
                    String type = archiveSnapshot.child("type").getValue(String.class);
                    if (type == null || !type.equals("appointment")) {
                        Log.w(TAG, "Skipping non-appointment archive: " + archiveId);
                        continue;
                    }

                    String archiveUserId = archiveSnapshot.child("userId").getValue(String.class);
                    Long timestamp = archiveSnapshot.child("archiveTimestamp").getValue(Long.class);
                    final boolean isConfirmed = archiveSnapshot.child("confirmed").getValue(Boolean.class) != null
                            ? archiveSnapshot.child("confirmed").getValue(Boolean.class) : false;

                    if (!isAdmin && (archiveUserId == null || !archiveUserId.equals(userId))) {
                        Log.d(TAG, "Skipping archive for different user: " + archiveUserId);
                        continue;
                    }

                    if (!selectedStatusFilter.equals("All") &&
                            ((selectedStatusFilter.equals("Confirmed") && !isConfirmed) ||
                                    (selectedStatusFilter.equals("Unconfirmed") && isConfirmed))) {
                        Log.d(TAG, "Skipping archive due to status filter: " + isConfirmed);
                        continue;
                    }

                    if (timestamp == null || !isWithinDateFilter(timestamp, selectedDateFilter)) {
                        Log.d(TAG, "Skipping archive due to date filter: " + timestamp);
                        continue;
                    }

                    String username = archiveSnapshot.child("username").getValue(String.class);
                    String checkupType = archiveSnapshot.child("checkupType").getValue(String.class);
                    String time = archiveSnapshot.child("time").getValue(String.class);
                    String doctor = archiveSnapshot.child("doctor").getValue(String.class);
                    String provider = archiveSnapshot.child("provider").getValue(String.class);
                    String virtualNumber = archiveSnapshot.child("virtualNumber").getValue(String.class);

                    String title, message;
                    if (!isConfirmed && !isAdmin) {
                        title = "Unconfirmed Appointment";
                        message = "Your appointment at " + (time != null ? time : "unknown time") + " was unconfirmed.";
                    } else {
                        title = isConfirmed ? "Confirmed Appointment" : "Unconfirmed Appointment";
                        message = String.format(
                                "Patient: %s\nType: %s\nTime: %s\nDoctor: %s\nProvider: %s\nVirtual #: %s",
                                username != null ? username : "Unknown",
                                checkupType != null ? checkupType : "Unknown",
                                time != null ? time : "Unknown",
                                doctor != null ? doctor : "Unknown",
                                provider != null ? provider : "None",
                                virtualNumber != null ? virtualNumber : "Unknown"
                        );
                    }

                    View archiveView = LayoutInflater.from(ArchivesActivity.this)
                            .inflate(R.layout.item_notification, archiveContainer, false);
                    TextView tvTitle = archiveView.findViewById(R.id.tv_notification_title);
                    TextView tvMessage = archiveView.findViewById(R.id.tv_notification_message);
                    tvTitle.setText(title);
                    tvMessage.setText(message);

                    archiveView.setOnClickListener(v -> showArchiveDetailsDialog(
                            archiveId,
                            title,
                            message,
                            archiveUserId,
                            isConfirmed
                    ));

                    archiveContainer.addView(archiveView);
                    hasArchives = true;
                }

                if (!hasArchives) {
                    View emptyView = LayoutInflater.from(ArchivesActivity.this)
                            .inflate(R.layout.item_empty_notification, archiveContainer, false);
                    TextView tvEmpty = emptyView.findViewById(R.id.tv_empty_notification);
                    tvEmpty.setText("No Archived Items");
                    archiveContainer.addView(emptyView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading archives: " + error.getMessage(), error.toException());
                Toast.makeText(ArchivesActivity.this, "Error loading archives", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showArchiveDetailsDialog(String archiveId, String title, String message, String archiveUserId, boolean confirmed) {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_archive_details, null);
            TextView tvTitle = dialogView.findViewById(R.id.tv_archive_title);
            TextView tvDetails = dialogView.findViewById(R.id.tv_archive_details);
            MaterialButton btnDelete = dialogView.findViewById(R.id.btn_delete_archive);
            MaterialButton btnClose = dialogView.findViewById(R.id.btn_close_archive);

            if (tvTitle == null || tvDetails == null || btnDelete == null || btnClose == null) {
                Log.e(TAG, "One or more UI elements not found in dialog_archive");
                Toast.makeText(this, "Error loading dialog", Toast.LENGTH_SHORT).show();
                return;
            }

            tvTitle.setText(title);
            tvDetails.setText(message);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            btnClose.setOnClickListener(v -> dialog.dismiss());

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Archive")
                        .setMessage("Are you sure you want to delete this archive entry?")
                        .setPositiveButton("Yes", (d, which) -> deleteArchive(archiveId, archiveUserId, confirmed, dialog))
                        .setNegativeButton("No", null)
                        .setCancelable(true)
                        .show();
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showArchiveDetailsDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error displaying archive dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteArchive(String archiveId, String archiveUserId, boolean confirmed, AlertDialog parentDialog) {
        DatabaseReference archiveRef = isAdmin ? adminArchivesRef.child(archiveId) : userArchivesRef.child(userId).child(archiveId);
        archiveRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Archive deleted: " + archiveId);
                    Toast.makeText(ArchivesActivity.this, "Archive deleted", Toast.LENGTH_SHORT).show();
                    if (isAdmin && !confirmed && archiveUserId != null) {
                        // Also delete user archive for unconfirmed appointments
                        userArchivesRef.child(archiveUserId).child(archiveId).removeValue()
                                .addOnSuccessListener(aVoid2 -> Log.d(TAG, "User archive deleted for user: " + archiveUserId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete user archive: " + e.getMessage()));
                    }
                    parentDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete archive: " + e.getMessage(), e);
                    Toast.makeText(ArchivesActivity.this, "Failed to delete archive: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isWithinDateFilter(long timestamp, String filter) {
        long currentTime = System.currentTimeMillis();
        long oneDayInMillis = 24L * 60 * 60 * 1000;
        long sevenDaysInMillis = 7 * oneDayInMillis;
        long thirtyDaysInMillis = 30 * oneDayInMillis;

        switch (filter) {
            case "Today":
                return currentTime - timestamp <= oneDayInMillis;
            case "Last 7 Days":
                return currentTime - timestamp <= sevenDaysInMillis;
            case "Last 30 Days":
                return currentTime - timestamp <= thirtyDaysInMillis;
            case "All Dates":
            default:
                return true;
        }
    }
}
