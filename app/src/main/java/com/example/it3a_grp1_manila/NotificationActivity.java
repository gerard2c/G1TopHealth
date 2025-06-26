package com.example.it3a_grp1_manila;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;


public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "NotificationActivity";
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private Toast backToast;
    private SharedPreferences prefs;
    private LinearLayout notificationContainer;
    private DatabaseReference usersRef, notificationsRef, appointmentsRef, archivesRef;
    private String username, userId;
    private TextView titleNotification;
    private ImageView iconSettings;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize UI elements
        titleNotification = findViewById(R.id.title_notification);
        iconSettings = findViewById(R.id.icon_settings);
        notificationContainer = findViewById(R.id.notification_container);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        notificationsRef = database.getReference("notifications");
        appointmentsRef = database.getReference("appointments");
        archivesRef = database.getReference("archives");

        // Get username from Intent, falling back to SharedPreferences
        Intent intent = getIntent();
        username = intent.getStringExtra("username") != null && !intent.getStringExtra("username").isEmpty()
                ? intent.getStringExtra("username")
                : prefs.getString("username", "User");

        // Fetch userId and admin status based on username
        fetchUserIdAndAdminStatus();

        // Set up bottom navigation
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent navIntent;
            if (itemId == R.id.home) {
                navIntent = new Intent(NotificationActivity.this, HomeActivity.class);
            } else if (itemId == R.id.notification) {
                return true;
            } else if (itemId == R.id.appointment) {
                navIntent = new Intent(NotificationActivity.this, AppointmentActivity.class);
            } else if (itemId == R.id.contactus) {
                navIntent = new Intent(NotificationActivity.this, ContactUsActivity.class);
            } else if (itemId == R.id.settings) {
                navIntent = new Intent(NotificationActivity.this, SettingsActivity.class);
            } else {
                return false;
            }
            navIntent.putExtra("username", username);
            startActivity(navIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        });

        bottomNav.setSelectedItemId(R.id.notification);
    }

    private void fetchUserIdAndAdminStatus() {
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        userId = userSnapshot.getKey();
                        isAdmin = userSnapshot.child("isAdmin").getValue(Boolean.class) != null && userSnapshot.child("isAdmin").getValue(Boolean.class);
                        Log.d(TAG, "Fetched userId: " + userId + ", isAdmin: " + isAdmin);
                        loadNotificationsAndAppointments();
                        break;
                    }
                } else {
                    Log.e(TAG, "User not found for username: " + username);
                    Toast.makeText(NotificationActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching userId: " + error.getMessage());
                Toast.makeText(NotificationActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotificationsAndAppointments() {
        if (userId == null) {
            Log.w(TAG, "UserId not available yet, delaying load");
            return;
        }
        notificationContainer.removeAllViews();
        Log.d(TAG, "Loading notifications and appointments for userId: " + userId);

        // Load user notifications
        notificationsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notificationContainer.removeAllViews();
                        boolean hasNotifications = false;

                        if (snapshot.exists() && snapshot.hasChildren()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                String notificationId = notificationSnapshot.getKey();
                                String title = notificationSnapshot.child("title").getValue(String.class);
                                String message = notificationSnapshot.child("message").getValue(String.class);
                                String appointmentId = notificationSnapshot.child("appointmentId").getValue(String.class);
                                Long timestamp = notificationSnapshot.child("timestamp").getValue(Long.class);

                                if (title == null || message == null || timestamp == null) {
                                    Log.w(TAG, "Invalid notification data for ID: " + notificationId);
                                    continue;
                                }

                                // Check if notification should be archived
                                checkAndArchiveNotification(notificationSnapshot);

                                // Display notification
                                View notificationView = LayoutInflater.from(NotificationActivity.this)
                                        .inflate(R.layout.item_notification, notificationContainer, false);
                                TextView tvTitle = notificationView.findViewById(R.id.tv_notification_title);
                                TextView tvMessage = notificationView.findViewById(R.id.tv_notification_message);
                                tvTitle.setText(title);
                                tvMessage.setText(message);

                                notificationView.setOnClickListener(v -> showNotificationDetailsDialog(title, message, appointmentId, false, notificationId));
                                notificationContainer.addView(notificationView);
                                hasNotifications = true;
                            }
                        }

                        // Load admin appointments if applicable
                        if (isAdmin) {
                            loadAdminAppointments(hasNotifications);
                        } else if (!hasNotifications) {
                            View emptyView = LayoutInflater.from(NotificationActivity.this)
                                    .inflate(R.layout.item_empty_notification, notificationContainer, false);
                            TextView tvEmpty = emptyView.findViewById(R.id.tv_empty_notification);
                            tvEmpty.setText("Empty Notification");
                            notificationContainer.addView(emptyView);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading notifications: " + error.getMessage());
                        Toast.makeText(NotificationActivity.this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndArchiveNotification(DataSnapshot notificationSnapshot) {
        String notificationId = notificationSnapshot.getKey();
        String appointmentId = notificationSnapshot.child("appointmentId").getValue(String.class);
        Long timestamp = notificationSnapshot.child("timestamp").getValue(Long.class);

        if (timestamp == null || appointmentId == null) {
            Log.w(TAG, "Skipping archiving for notification " + notificationId + ": Missing timestamp or appointmentId");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long notificationAge = currentTime - timestamp;
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000; // 30 days
        long oneDayInMillis = 24L * 60 * 60 * 1000; // 1 day

        appointmentsRef.child(appointmentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean confirmed = snapshot.child("confirmed").getValue(Boolean.class);
                    boolean shouldArchive = (confirmed != null && confirmed && notificationAge >= thirtyDaysInMillis) ||
                            (confirmed == null || !confirmed && notificationAge >= oneDayInMillis);

                    if (shouldArchive) {
                        // Archive the notification
                        Map<String, Object> archiveData = new HashMap<>();
                        archiveData.put("userId", notificationSnapshot.child("userId").getValue(String.class));
                        archiveData.put("title", notificationSnapshot.child("title").getValue(String.class));
                        archiveData.put("message", notificationSnapshot.child("message").getValue(String.class));
                        archiveData.put("type", notificationSnapshot.child("type").getValue(String.class));
                        archiveData.put("appointmentId", appointmentId);
                        archiveData.put("timestamp", timestamp);
                        archiveData.put("confirmed", confirmed != null && confirmed);

                        archivesRef.child(notificationId).setValue(archiveData)
                                .addOnSuccessListener(aVoid -> {
                                    notificationsRef.child(notificationId).removeValue();
                                    Log.d(TAG, "Notification " + notificationId + " archived");
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to archive notification " + notificationId + ": " + e.getMessage()));
                    }
                } else {
                    // If appointment doesn't exist (e.g., deleted), archive as unconfirmed
                    Map<String, Object> archiveData = new HashMap<>();
                    archiveData.put("userId", notificationSnapshot.child("userId").getValue(String.class));
                    archiveData.put("title", notificationSnapshot.child("title").getValue(String.class));
                    archiveData.put("message", notificationSnapshot.child("message").getValue(String.class));
                    archiveData.put("type", notificationSnapshot.child("type").getValue(String.class));
                    archiveData.put("appointmentId", appointmentId);
                    archiveData.put("timestamp", timestamp);
                    archiveData.put("confirmed", false);

                    archivesRef.child(notificationId).setValue(archiveData)
                            .addOnSuccessListener(aVoid -> {
                                notificationsRef.child(notificationId).removeValue();
                                Log.d(TAG, "Notification " + notificationId + " archived (appointment deleted)");
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to archive notification " + notificationId + ": " + e.getMessage()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking appointment for notification " + notificationId + ": " + error.getMessage());
            }
        });
    }

    private void loadAdminAppointments(boolean hasUserNotifications) {
        appointmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasAppointments = false;
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    String appointmentId = appointmentSnapshot.getKey();
                    String username = appointmentSnapshot.child("username").getValue(String.class);
                    String checkupType = appointmentSnapshot.child("checkupType").getValue(String.class);
                    String time = appointmentSnapshot.child("time").getValue(String.class);
                    String doctor = appointmentSnapshot.child("doctor").getValue(String.class);
                    Boolean confirmed = appointmentSnapshot.child("confirmed").getValue(Boolean.class);

                    if (username != null && time != null) {
                        String message = "Booked: " + username + " - " + checkupType + " at " + time + (confirmed != null && confirmed ? " (Confirmed)" : " (Pending)");
                        View appointmentView = LayoutInflater.from(NotificationActivity.this)
                                .inflate(R.layout.item_notification, notificationContainer, false);
                        TextView tvTitle = appointmentView.findViewById(R.id.tv_notification_title);
                        TextView tvMessage = appointmentView.findViewById(R.id.tv_notification_message);
                        tvTitle.setText("New Appointment!");
                        tvMessage.setText(message);

                        final String idToPass = appointmentId;
                        appointmentView.setOnClickListener(v -> showNotificationDetailsDialog("New Appointment!", message, idToPass, true, null));

                        notificationContainer.addView(appointmentView);
                        hasAppointments = true;
                    }
                }
                if (!hasUserNotifications && !hasAppointments) {
                    notificationContainer.removeAllViews();
                    View emptyView = LayoutInflater.from(NotificationActivity.this)
                            .inflate(R.layout.item_empty_notification, notificationContainer, false);
                    TextView tvEmpty = emptyView.findViewById(R.id.tv_empty_notification);
                    tvEmpty.setText("Empty Notification");
                    notificationContainer.addView(emptyView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading appointments: " + error.getMessage());
                Toast.makeText(NotificationActivity.this, "Error loading appointments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNotificationDetailsDialog(String title, String message, String id, boolean isAppointment, String notificationId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification_details, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        Button btnCheck = dialogView.findViewById(R.id.btn_check);
        Button btnClose = dialogView.findViewById(R.id.btn_close);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);

        tvTitle.setText(title != null ? title : "No Title");
        tvMessage.setText(message != null ? message : "No Message");

        final AlertDialog dialog = builder.setView(dialogView).create();
        dialog.show();

        if (isAdmin && isAppointment) {
            btnCheck.setVisibility(View.VISIBLE);
            btnCheck.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationActivity.this, AppointmentActivity.class);
                intent.putExtra("username", username);
                intent.putExtra("appointmentId", id);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                dialog.dismiss();
            });
        } else {
            btnCheck.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(NotificationActivity.this)
                    .setTitle("Delete Confirmation")
                    .setMessage("Are you sure you want to delete this " + (isAppointment ? "appointment" : "notification") + "?")
                    .setPositiveButton("Yes", (dialog1, which) -> {
                        if (isAppointment) {
                            appointmentsRef.child(id).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(NotificationActivity.this, "Appointment deleted", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(NotificationActivity.this, "Failed to delete appointment", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to delete appointment: " + e.getMessage());
                                    });
                        } else {
                            notificationsRef.child(notificationId).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(NotificationActivity.this, "Notification deleted", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(NotificationActivity.this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to delete notification: " + e.getMessage());
                                    });
                        }
                    })
                    .setNegativeButton("No", (dialog1, which) -> dialog1.dismiss())
                    .setCancelable(true)
                            .
                    show();
        });
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            if (backToast != null) {
                backToast.cancel();
            }
            super.onBackPressed();
            finish();
        } else {
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}