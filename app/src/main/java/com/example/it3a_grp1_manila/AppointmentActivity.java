package com.example.it3a_grp1_manila;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Arrays;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import android.net.Uri;

public class AppointmentActivity extends AppCompatActivity {
    private static final String TAG = "AppointmentActivity";
    private static final String CHANNEL_ID = "AppointmentApp";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    private DatabaseReference usersRef, appointmentsRef, adminRef, timesRef, notificationsRef, userArchivesRef, adminArchivesRef;
    private String username, userId;
    private BottomNavigationView bottomNav;
    private boolean isAdmin;
    private Button addTimeSlotButton, viewAppointmentsButton, viewAnalyticsButton;
    private RecyclerView recyclerTimeSlots;
    private TimeSlotAdapter timeSlotAdapter;
    private List<TimeSlotData> timeSlots;
    private FirebaseAuth mAuth;
    private String selectedAppointmentId;
    private com.google.firebase.database.ChildEventListener adminNotificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                Log.d(TAG, "Requested POST_NOTIFICATIONS permission");
            }
        }

        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            usersRef = database.getReference("users");
            appointmentsRef = database.getReference("appointments");
            adminRef = database.getReference("admin");
            timesRef = adminRef.child("availableTimes");
            notificationsRef = database.getReference("notifications");
            userArchivesRef = database.getReference("userArchives");
            adminArchivesRef = database.getReference("adminArchives");
            mAuth = FirebaseAuth.getInstance();
            userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing database", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        selectedAppointmentId = intent.getStringExtra("appointmentId");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Username not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        createNotificationChannel();

        try {
            bottomNav = findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setOnItemSelectedListener(item -> {
                    int itemId = item.getItemId();
                    Intent navIntent;
                    if (itemId == R.id.home) {
                        navIntent = new Intent(AppointmentActivity.this, HomeActivity.class);
                    } else if (itemId == R.id.notification) {
                        navIntent = new Intent(AppointmentActivity.this, NotificationActivity.class);
                    } else if (itemId == R.id.appointment) {
                        return true;
                    } else if (itemId == R.id.contactus) {
                        navIntent = new Intent(AppointmentActivity.this, ContactUsActivity.class);
                    } else if (itemId == R.id.settings) {
                        navIntent = new Intent(AppointmentActivity.this, SettingsActivity.class);
                    } else {
                        return false;
                    }
                    navIntent.putExtra("username", username);
                    startActivity(navIntent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                });
                bottomNav.setSelectedItemId(R.id.appointment);
            } else {
                Log.e(TAG, "BottomNavigationView not found");
                Toast.makeText(this, "Error loading navigation", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation: " + e.getMessage(), e);
            Toast.makeText(this, "Error in navigation setup", Toast.LENGTH_SHORT).show();
        }

        checkAdminStatus();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Log.d(TAG, "Creating notification channel: " + CHANNEL_ID);
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Appointment Notifications", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage(), e);
            }
        }
    }

    private void checkAdminStatus() {
        try {
            Log.d(TAG, "Checking admin status for username: " + username);
            usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            userId = userSnapshot.getKey();
                            Boolean isAdminValue = userSnapshot.child("isAdmin").getValue(Boolean.class);
                            isAdmin = isAdminValue != null && isAdminValue;
                            Log.d(TAG, "UserId: " + userId + ", isAdmin: " + isAdmin);
                            initializeUI();
                            if (isAdmin) {
                                setupAdminNotificationListener();
                            }
                        }
                    } else {
                        Log.e(TAG, "User not found in database");
                        Toast.makeText(AppointmentActivity.this, "User not found in database", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error verifying user: " + error.getMessage(), error.toException());
                    Toast.makeText(AppointmentActivity.this, "Error verifying user", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in checkAdminStatus: " + e.getMessage(), e);
            Toast.makeText(this, "Error checking user status", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupAdminNotificationListener() {
        Log.d(TAG, "Setting up admin notification listener for adminTopHealth");
        if (adminNotificationListener != null) {
            appointmentsRef.removeEventListener(adminNotificationListener);
            Log.d(TAG, "Removed existing admin notification listener");
        }
        adminNotificationListener = new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                try {
                    String appointmentId = snapshot.getKey();
                    String username = snapshot.child("username").getValue(String.class);
                    String time = snapshot.child("time").getValue(String.class);
                    Boolean confirmed = snapshot.child("confirmed").getValue(Boolean.class);
                    Log.d(TAG, "New appointment: ID=" + appointmentId + ", User=" + username + ", Time=" + time);

                    if (username != null && time != null && confirmed != null && !confirmed) {
                        String userMessage = "New appointment booked by " + username + " at " + time;
                        sendLocalNotification("New Appointment", userMessage, appointmentId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing new appointment: " + e.getMessage(), e);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    String appointmentId = snapshot.getKey();
                    Boolean confirmed = snapshot.child("confirmed").getValue(Boolean.class);
                    Boolean notificationSent = snapshot.child("notificationSent").getValue(Boolean.class);
                    String time = snapshot.child("time").getValue(String.class);
                    String userId = snapshot.child("userId").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String checkupType = snapshot.child("checkupType").getValue(String.class);
                    String doctor = snapshot.child("doctor").getValue(String.class);
                    String provider = snapshot.child("provider").getValue(String.class);
                    String virtualNumber = snapshot.child("virtualNumber").getValue(String.class);

                    if (appointmentId == null || time == null || userId == null) {
                        Log.e(TAG, "Missing required fields for appointment: " + appointmentId);
                        return;
                    }

                    if (confirmed != null && confirmed && (notificationSent == null || !notificationSent)) {
                        notificationsRef.orderByChild("appointmentId").equalTo(appointmentId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        boolean hasConfirmation = false;
                                        for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                            String type = notificationSnapshot.child("type").getValue(String.class);
                                            if ("confirmation".equals(type)) {
                                                hasConfirmation = true;
                                                break;
                                            }
                                        }
                                        if (!hasConfirmation) {
                                            String message = "Your appointment at " + time + " has been confirmed.";
                                            sendLocalNotification("Appointment Confirmed", message, appointmentId);
                                            saveNotification(userId, "Appointment Confirmed", message, "confirmation", appointmentId);
                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("notificationSent", true);
                                            appointmentsRef.child(appointmentId).updateChildren(updates)
                                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update notificationSent: " + e.getMessage()));

                                            Map<String, Object> archiveData = new HashMap<>();
                                            archiveData.put("type", "appointment");
                                            archiveData.put("userId", userId);
                                            archiveData.put("appointmentId", appointmentId);
                                            archiveData.put("username", username != null ? username : "");
                                            archiveData.put("checkupType", checkupType != null ? checkupType : "");
                                            archiveData.put("time", time);
                                            archiveData.put("doctor", doctor != null ? doctor : "");
                                            archiveData.put("provider", provider != null ? provider : "");
                                            archiveData.put("virtualNumber", virtualNumber != null ? virtualNumber : "");
                                            archiveData.put("confirmed", true);
                                            archiveData.put("archiveTimestamp", ServerValue.TIMESTAMP);

                                            adminArchivesRef.child("appointment_" + appointmentId).setValue(archiveData)
                                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Confirmed appointment archived in adminArchives: " + appointmentId))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to archive confirmed appointment in adminArchives: " + e.getMessage()));
                                            userArchivesRef.child(userId).child("appointment_" + appointmentId).setValue(archiveData)
                                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Confirmed appointment archived in userArchives: " + appointmentId))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to archive confirmed appointment for user: " + e.getMessage()));
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error checking notifications: " + error.getMessage());
                                    }
                                });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing appointment change: " + e.getMessage(), e);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                try {
                    String appointmentId = snapshot.getKey();
                    if (appointmentId == null) {
                        Log.e(TAG, "Invalid appointmentId in onChildRemoved");
                        return;
                    }
                    Log.d(TAG, "Appointment removed: " + appointmentId);
                    String userId = snapshot.child("userId").getValue(String.class);
                    if (userId == null) {
                        Log.e(TAG, "Missing userId for appointment: " + appointmentId);
                        return;
                    }

                    DatabaseReference adminArchiveRef = adminArchivesRef.child("appointment_" + appointmentId);
                    adminArchiveRef.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            if (currentData.getValue() == null) {
                                String username = snapshot.child("username").getValue(String.class);
                                String checkupType = snapshot.child("checkupType").getValue(String.class);
                                String time = snapshot.child("time").getValue(String.class);
                                String doctor = snapshot.child("doctor").getValue(String.class);
                                String provider = snapshot.child("provider").getValue(String.class);
                                String virtualNumber = snapshot.child("virtualNumber").getValue(String.class);

                                Map<String, Object> archiveData = new HashMap<>();
                                archiveData.put("type", "appointment");
                                archiveData.put("userId", userId);
                                archiveData.put("appointmentId", appointmentId);
                                archiveData.put("username", username != null ? username : "");
                                archiveData.put("checkupType", checkupType != null ? checkupType : "");
                                archiveData.put("time", time != null ? time : "");
                                archiveData.put("doctor", doctor != null ? doctor : "");
                                archiveData.put("provider", provider != null ? provider : "");
                                archiveData.put("virtualNumber", virtualNumber != null ? virtualNumber : "");
                                archiveData.put("confirmed", false);
                                archiveData.put("archiveTimestamp", ServerValue.TIMESTAMP);

                                currentData.setValue(archiveData);
                                return Transaction.success(currentData);
                            }
                            return Transaction.success(currentData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                            if (error != null) {
                                Log.e(TAG, "Admin archive transaction failed for appointment " + appointmentId + ": " + error.getMessage());
                                Toast.makeText(AppointmentActivity.this, "Failed to archive deleted appointment", Toast.LENGTH_SHORT).show();
                            } else if (committed && currentData != null && currentData.getValue() != null) {
                                Log.d(TAG, "Appointment archived in adminArchives: " + appointmentId);
                                Map<String, Object> userArchiveData = new HashMap<>();
                                userArchiveData.put("type", "appointment");
                                userArchiveData.put("userId", userId);
                                userArchiveData.put("appointmentId", appointmentId);
                                userArchiveData.put("username", snapshot.child("username").getValue(String.class) != null ? snapshot.child("username").getValue(String.class) : "");
                                userArchiveData.put("checkupType", snapshot.child("checkupType").getValue(String.class) != null ? snapshot.child("checkupType").getValue(String.class) : "");
                                userArchiveData.put("time", snapshot.child("time").getValue(String.class) != null ? snapshot.child("time").getValue(String.class) : "");
                                userArchiveData.put("doctor", snapshot.child("doctor").getValue(String.class) != null ? snapshot.child("doctor").getValue(String.class) : "");
                                userArchiveData.put("provider", snapshot.child("provider").getValue(String.class) != null ? snapshot.child("provider").getValue(String.class) : "");
                                userArchiveData.put("virtualNumber", snapshot.child("virtualNumber").getValue(String.class) != null ? snapshot.child("virtualNumber").getValue(String.class) : "");
                                userArchiveData.put("confirmed", false);
                                userArchiveData.put("archiveTimestamp", ServerValue.TIMESTAMP);

                                userArchivesRef.child(userId).child("appointment_" + appointmentId).setValue(userArchiveData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Appointment archived in userArchives for user: " + userId);
                                            String time = snapshot.child("time").getValue(String.class);
                                            String message = "Your appointment at " + (time != null ? time : "unknown time") + " was unconfirmed.";
                                            saveNotification(userId, "Appointment Unconfirmed", message, "unconfirmed", appointmentId);
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to archive appointment for user: " + e.getMessage()));
                            } else {
                                Log.d(TAG, "Appointment already archived in adminArchives or not archived: " + appointmentId);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error handling deleted appointment: " + e.getMessage(), e);
                    Toast.makeText(AppointmentActivity.this, "Error archiving deleted appointment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Admin listener error: " + error.getMessage());
            }
        };
        appointmentsRef.addChildEventListener(adminNotificationListener);
        Log.d(TAG, "Admin notification listener attached");
    }

    private void initializeUI() {
        try {
            addTimeSlotButton = findViewById(R.id.btn_add_time_slot);
            viewAppointmentsButton = findViewById(R.id.btn_view_appointments);
            viewAnalyticsButton = findViewById(R.id.btn_view_analytics);
            recyclerTimeSlots = findViewById(R.id.recycler_time_slots);

            if (addTimeSlotButton == null || viewAppointmentsButton == null || recyclerTimeSlots == null || viewAnalyticsButton == null) {
                Log.e(TAG, "UI elements not found");
                Toast.makeText(this, "Error loading interface", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isAdmin) {
                findViewById(R.id.card_book_appointment).setVisibility(View.GONE);
                addTimeSlotButton.setVisibility(View.VISIBLE);
                viewAppointmentsButton.setVisibility(View.VISIBLE);
                viewAnalyticsButton.setVisibility(View.VISIBLE);
                recyclerTimeSlots.setVisibility(View.VISIBLE);

                timeSlots = new ArrayList<>();
                timeSlotAdapter = new TimeSlotAdapter(timeSlots);
                recyclerTimeSlots.setLayoutManager(new LinearLayoutManager(this));
                recyclerTimeSlots.setAdapter(timeSlotAdapter);

                loadTimeSlots();

                addTimeSlotButton.setOnClickListener(v -> showAddTimeSlotDialog());
                viewAppointmentsButton.setOnClickListener(v -> showBookedAppointmentsDialog());
                viewAnalyticsButton.setOnClickListener(v -> showAnalyticsDialog());
            } else {
                findViewById(R.id.card_book_appointment).setVisibility(View.VISIBLE);
                addTimeSlotButton.setVisibility(View.GONE);
                viewAppointmentsButton.setVisibility(View.GONE);
                viewAnalyticsButton.setVisibility(View.GONE);
                recyclerTimeSlots.setVisibility(View.GONE);
                View bookButton = findViewById(R.id.book_appointment_button);
                if (bookButton != null) {
                    bookButton.setOnClickListener(v -> showBookAppointmentDialog());
                }
            }
            if (isAdmin && selectedAppointmentId != null) {
                showBookedAppointmentsDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up interface", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAnalyticsDialog() {
        try {
            Log.d(TAG, "Showing analytics dialog");
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_analytics, null);
            PieChart chartConfirmationStatus = dialogView.findViewById(R.id.chart_confirmation_status);
            LineChart chartAppointmentTrend = dialogView.findViewById(R.id.chart_appointment_trend);
            BarChart chartDoctors = dialogView.findViewById(R.id.chart_doctors);
            BarChart chartCheckups = dialogView.findViewById(R.id.chart_checkups);
            BarChart chartTimes = dialogView.findViewById(R.id.chart_times);
            Button btnClose = dialogView.findViewById(R.id.btn_close_analytics);

            if (chartConfirmationStatus == null || chartAppointmentTrend == null || chartDoctors == null || chartCheckups == null || chartTimes == null || btnClose == null) {
                Log.e(TAG, "One or more UI elements not found in dialog_analytics");
                Toast.makeText(this, "Error loading analytics charts", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            btnClose.setOnClickListener(v -> dialog.dismiss());

            // Fetch data from Firebase for analytics
            appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Integer> doctorCounts = new HashMap<>();
                    Map<String, Integer> checkupTypeCounts = new HashMap<>();
                    Map<String, Integer> timeCounts = new HashMap<>();
                    int confirmedCount = 0, pendingCount = 0;
                    Map<String, Integer> dailyCounts = new HashMap<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Calendar calendar = Calendar.getInstance();

                    // Initialize daily counts for the last 7 days
                    List<String> dateLabels = new ArrayList<>();
                    for (int i = 6; i >= 0; i--) {
                        calendar.setTimeInMillis(System.currentTimeMillis());
                        calendar.add(Calendar.DAY_OF_YEAR, -i);
                        String date = dateFormat.format(calendar.getTime());
                        dailyCounts.put(date, 0);
                        dateLabels.add(date.substring(5)); // Show MM-dd
                    }

                    for (DataSnapshot apptSnapshot : snapshot.getChildren()) {
                        String doctor = apptSnapshot.child("doctor").getValue(String.class);
                        String checkupType = apptSnapshot.child("checkupType").getValue(String.class);
                        String time = apptSnapshot.child("time").getValue(String.class);
                        Boolean confirmed = apptSnapshot.child("confirmed").getValue(Boolean.class);
                        Long timestamp = apptSnapshot.child("timestamp").getValue(Long.class);

                        if (doctor != null) {
                            doctorCounts.put(doctor, doctorCounts.getOrDefault(doctor, 0) + 1);
                        }
                        if (checkupType != null) {
                            checkupTypeCounts.put(checkupType, checkupTypeCounts.getOrDefault(checkupType, 0) + 1);
                        }
                        if (time != null) {
                            timeCounts.put(time, timeCounts.getOrDefault(time, 0) + 1);
                        }
                        if (confirmed != null) {
                            if (confirmed) {
                                confirmedCount++;
                            } else {
                                pendingCount++;
                            }
                        }
                        if (timestamp != null) {
                            String date = dateFormat.format(new Date(timestamp));
                            if (dailyCounts.containsKey(date)) {
                                dailyCounts.put(date, dailyCounts.getOrDefault(date, 0) + 1);
                            }
                        }
                    }

                    // Populate Confirmation Status Pie Chart
                    List<PieEntry> statusEntries = new ArrayList<>();
                    if (confirmedCount > 0) {
                        statusEntries.add(new PieEntry(confirmedCount, "Confirmed"));
                    }
                    if (pendingCount > 0) {
                        statusEntries.add(new PieEntry(pendingCount, "Pending"));
                    }
                    if (!statusEntries.isEmpty()) {
                        PieDataSet statusDataSet = new PieDataSet(statusEntries, "Confirmation Status");
                        statusDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        PieData statusData = new PieData(statusDataSet);
                        chartConfirmationStatus.setData(statusData);
                        chartConfirmationStatus.getDescription().setEnabled(false);
                        chartConfirmationStatus.setDrawEntryLabels(true);
                        chartConfirmationStatus.setUsePercentValues(true);
                        chartConfirmationStatus.invalidate();
                    }

                    // Populate Appointment Trend Line Chart
                    List<Entry> trendEntries = new ArrayList<>();
                    int index = 0;
                    for (String date : dateLabels) {
                        String fullDate = dateFormat.format(calendar.getTime());
                        trendEntries.add(new Entry(index++, dailyCounts.getOrDefault(fullDate, 0)));
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    if (!trendEntries.isEmpty()) {
                        LineDataSet trendDataSet = new LineDataSet(trendEntries, "Appointments");
                        trendDataSet.setColor(ColorTemplate.MATERIAL_COLORS[0]);
                        trendDataSet.setCircleColor(ColorTemplate.MATERIAL_COLORS[0]);
                        trendDataSet.setLineWidth(2f);
                        trendDataSet.setCircleRadius(4f);
                        trendDataSet.setDrawValues(true);
                        LineData trendData = new LineData(trendDataSet);
                        chartAppointmentTrend.setData(trendData);
                        chartAppointmentTrend.getDescription().setEnabled(false);
                        chartAppointmentTrend.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dateLabels));
                        chartAppointmentTrend.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        chartAppointmentTrend.getXAxis().setLabelRotationAngle(45);
                        chartAppointmentTrend.invalidate();
                    }

                    // Populate Doctors Bar Chart
                    List<BarEntry> doctorEntries = new ArrayList<>();
                    List<String> doctorLabels = new ArrayList<>();
                    index = 0;
                    for (Map.Entry<String, Integer> entry : doctorCounts.entrySet()) {
                        doctorEntries.add(new BarEntry(index++, entry.getValue()));
                        doctorLabels.add(entry.getKey());
                    }
                    if (!doctorEntries.isEmpty()) {
                        BarDataSet doctorDataSet = new BarDataSet(doctorEntries, "Appointments by Doctor");
                        doctorDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        BarData doctorData = new BarData(doctorDataSet);
                        chartDoctors.setData(doctorData);
                        chartDoctors.getDescription().setEnabled(false);
                        chartDoctors.getXAxis().setValueFormatter(new IndexAxisValueFormatter(doctorLabels));
                        chartDoctors.getXAxis().setLabelRotationAngle(45);
                        chartDoctors.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        chartDoctors.invalidate();
                    }

                    // Populate Checkup Types Bar Chart
                    List<BarEntry> checkupEntries = new ArrayList<>();
                    List<String> checkupLabels = new ArrayList<>();
                    index = 0;
                    for (Map.Entry<String, Integer> entry : checkupTypeCounts.entrySet()) {
                        checkupEntries.add(new BarEntry(index++, entry.getValue()));
                        checkupLabels.add(entry.getKey());
                    }
                    if (!checkupEntries.isEmpty()) {
                        BarDataSet checkupDataSet = new BarDataSet(checkupEntries, "Appointments by Checkup Type");
                        checkupDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        BarData checkupData = new BarData(checkupDataSet);
                        chartCheckups.setData(checkupData);
                        chartCheckups.getDescription().setEnabled(false);
                        chartCheckups.getXAxis().setValueFormatter(new IndexAxisValueFormatter(checkupLabels));
                        chartCheckups.getXAxis().setLabelRotationAngle(45);
                        chartCheckups.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        chartCheckups.invalidate();
                    }

                    // Populate Time Slots Bar Chart
                    List<BarEntry> timeEntries = new ArrayList<>();
                    List<String> timeLabels = new ArrayList<>();
                    index = 0;
                    for (Map.Entry<String, Integer> entry : timeCounts.entrySet()) {
                        timeEntries.add(new BarEntry(index++, entry.getValue()));
                        timeLabels.add(entry.getKey());
                    }
                    if (!timeEntries.isEmpty()) {
                        BarDataSet timeDataSet = new BarDataSet(timeEntries, "Appointments by Time Slot");
                        timeDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        BarData timeData = new BarData(timeDataSet);
                        chartTimes.setData(timeData);
                        chartTimes.getDescription().setEnabled(false);
                        chartTimes.getXAxis().setValueFormatter(new IndexAxisValueFormatter(timeLabels));
                        chartTimes.getXAxis().setLabelRotationAngle(45);
                        chartTimes.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        chartTimes.invalidate();
                    }

                    if (statusEntries.isEmpty() && trendEntries.isEmpty() && doctorEntries.isEmpty() && checkupEntries.isEmpty() && timeEntries.isEmpty()) {
                        Toast.makeText(AppointmentActivity.this, "No appointment data available", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        return;
                    }

                    dialog.show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading analytics data: " + error.getMessage());
                    Toast.makeText(AppointmentActivity.this, "Error loading analytics data", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showAnalyticsDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error displaying analytics", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTimeSlots() {
        try {
            timesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    timeSlots.clear();
                    for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                        String time = timeSnapshot.child("time").getValue(String.class);
                        Object doctorValue = timeSnapshot.child("doctorAvailability").getValue();
                        String doctor = "None";
                        if (doctorValue instanceof ArrayList) {
                            ArrayList<?> doctorList = (ArrayList<?>) doctorValue;
                            doctor = String.join(", ", doctorList.toArray(new String[0]));
                        } else if (doctorValue instanceof String) {
                            doctor = (String) doctorValue;
                        }
                        if (time != null) {
                            timeSlots.add(new TimeSlotData(time, doctor));
                        }
                    }
                    timeSlotAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Time slots loaded: " + timeSlots.size());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading time slots: " + error.getMessage());
                    Toast.makeText(AppointmentActivity.this, "Error loading time slots", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading time slots: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading time slots", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddTimeSlotDialog() {
        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_time_slot, null);
            TimePicker timePicker = dialogView.findViewById(R.id.time_picker);
            if (timePicker == null) {
                Log.e(TAG, "TimePicker not found");
                Toast.makeText(this, "Error loading time picker", Toast.LENGTH_SHORT).show();
                return;
            }
            timePicker.setIs24HourView(false);

            new AlertDialog.Builder(this)
                    .setTitle("Add Time Slot")
                    .setView(dialogView)
                    .setPositiveButton("Add", (dialog, which) -> {
                        int hour = timePicker.getHour();
                        int minute = timePicker.getMinute();
                        String time = String.format("%02d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour < 12 ? "AM" : "PM");
                        addTimeSlot(time, null);
                    })
                    .setNeutralButton("Next", (dialog, which) -> {
                        int hour = timePicker.getHour();
                        int minute = timePicker.getMinute();
                        String time = String.format("%02d:%02d %s", hour % 12 == 0 ? 12 : hour % 12, minute, hour < 12 ? "AM" : "PM");
                        addTimeSlot(time, timeRefKey -> showAddDoctorsDialog(time, timeRefKey));
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing add time slot dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error adding time slot", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTimeSlot(String time, AddTimeSlotCallback callback) {
        DatabaseReference newTimeRef = timesRef.push();
        Map<String, Object> timeData = new HashMap<>();
        timeData.put("time", time);
        newTimeRef.setValue(timeData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AppointmentActivity.this, "Time slot added", Toast.LENGTH_SHORT).show();
                    loadTimeSlots();
                    if (callback != null) {
                        callback.onSuccess(newTimeRef.getKey());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AppointmentActivity.this, "Failed to add time slot", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to add time slot: " + e.getMessage());
                });
    }

    private interface AddTimeSlotCallback {
        void onSuccess(String timeRefKey);
    }

    private class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private final List<TimeSlotData> timeSlots;

        TimeSlotAdapter(List<TimeSlotData> timeSlots) {
            this.timeSlots = timeSlots != null ? timeSlots : new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
                return new ViewHolder(view);
            } catch (Exception e) {
                Log.e(TAG, "Error inflating item_time_slot: " + e.getMessage(), e);
                throw new RuntimeException("Failed to inflate time slot layout", e);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                TimeSlotData slot = timeSlots.get(position);
                holder.tvTimeValue.setText(slot.getTime());
                holder.tvDoctorValue.setText(slot.getDoctor());
                holder.btnDelete.setOnClickListener(v -> deleteTimeSlot(slot.getTime()));
            } catch (Exception e) {
                Log.e(TAG, "Error binding time slot at position " + position + ": " + e.getMessage(), e);
            }
        }

        @Override
        public int getItemCount() {
            return timeSlots.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTimeSlot, tvTimeValue, tvDoctorSlot, tvDoctorValue;
            Button btnEditDoctors, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                try {
                    tvTimeSlot = itemView.findViewById(R.id.tv_time_slot);
                    tvTimeValue = itemView.findViewById(R.id.tv_time_value);
                    tvDoctorSlot = itemView.findViewById(R.id.tv_doctor_slot);
                    tvDoctorValue = itemView.findViewById(R.id.tv_doctor_value);
                    btnEditDoctors = itemView.findViewById(R.id.btn_edit_doctors);
                    btnDelete = itemView.findViewById(R.id.btn_delete);
                    if (btnEditDoctors != null) {
                        btnEditDoctors.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing TimeSlot ViewHolder: " + e.getMessage(), e);
                }
            }
        }
    }

    private static class TimeSlotData {
        private final String time;
        private final String doctor;

        TimeSlotData(String time, String doctor) {
            this.time = time;
            this.doctor = doctor;
        }

        public String getTime() { return time != null ? time : ""; }
        public String getDoctor() { return doctor != null ? doctor : "None"; }
    }

    private void showAddDoctorsDialog(String time, String key) {
        try {
            Log.d(TAG, "Showing Add Doctors dialog for time: " + time);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_doctors, null);
            if (dialogView == null) {
                Log.e(TAG, "Dialog view inflation failed for dialog_add_doctors");
                Toast.makeText(this, "Error loading doctor assignment", Toast.LENGTH_SHORT).show();
                return;
            }
            ListView listView = dialogView.findViewById(R.id.list_doctors);
            if (listView == null) {
                Log.e(TAG, "ListView not found in dialog_add_doctors");
                Toast.makeText(this, "Error loading doctor assignment", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] doctors = {"J.A. Costales", "R.F. Manila", "P.K. Jao", "G. Escueta"};
            List<DoctorItem> doctorItems = new ArrayList<>();
            for (String doctor : doctors) {
                doctorItems.add(new DoctorItem(doctor, false));
            }

            DoctorAdapter adapter = new DoctorAdapter(this, R.layout.item_doctor, doctorItems);
            listView.setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Assign Doctors for " + time)
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialogInterface, which) -> {
                        try {
                            List<String> selectedDoctors = new ArrayList<>();
                            for (DoctorItem item : doctorItems) {
                                if (item.isChecked()) {
                                    selectedDoctors.add(item.getName());
                                }
                            }
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("doctorAvailability", selectedDoctors.isEmpty() ? null : selectedDoctors);
                            timesRef.child(key).updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AppointmentActivity.this, "Doctors assigned", Toast.LENGTH_SHORT).show();
                                        loadTimeSlots();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AppointmentActivity.this, "Failed to assign doctors", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to assign doctors: " + e.getMessage());
                                    });
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving doctor assignment: " + e.getMessage(), e);
                            Toast.makeText(AppointmentActivity.this, "Error assigning doctors", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                    .setCancelable(true)
                    .create();

            dialog.setOnDismissListener(dialogInterface -> loadTimeSlots());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showAddDoctorsDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading doctor assignment", Toast.LENGTH_SHORT).show();
        }
    }

    private static class DoctorItem {
        private final String name;
        private boolean isChecked;

        DoctorItem(String name, boolean isChecked) {
            this.name = name;
            this.isChecked = isChecked;
        }

        public String getName() { return name; }
        public boolean isChecked() { return isChecked; }
        public void setChecked(boolean checked) { this.isChecked = checked; }
    }

    private class DoctorAdapter extends ArrayAdapter<DoctorItem> {
        private final List<DoctorItem> doctorItems;

        DoctorAdapter(Context context, int resource, List<DoctorItem> doctorItems) {
            super(context, resource, doctorItems);
            this.doctorItems = doctorItems;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_doctor, parent, false);
            }

            DoctorItem item = doctorItems.get(position);
            CheckBox checkBox = view.findViewById(R.id.cb_doctor);
            if (checkBox != null) {
                checkBox.setText(item.getName());
                checkBox.setChecked(item.isChecked());
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.setChecked(isChecked));
            } else {
                Log.e(TAG, "CheckBox not found in item_doctor layout");
            }

            return view;
        }
    }

    private void deleteTimeSlot(String time) {
        try {
            Log.d(TAG, "Deleting time slot: " + time);
            timesRef.orderByChild("time").equalTo(time).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                        timeSnapshot.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AppointmentActivity.this, "Time slot deleted", Toast.LENGTH_SHORT).show();
                                    loadTimeSlots();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AppointmentActivity.this, "Error deleting time slot", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error deleting time slot: " + e.getMessage());
                                });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error deleting time slot: " + error.getMessage());
                    Toast.makeText(AppointmentActivity.this, "Error deleting time slot", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteTimeSlot: " + e.getMessage(), e);
            Toast.makeText(this, "Error deleting time slot", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBookedAppointmentsDialog() {
        try {
            Log.d(TAG, "Showing booked appointments");
            appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        new AlertDialog.Builder(AppointmentActivity.this)
                                .setTitle("Booked Appointments")
                                .setMessage("No Booked Appointments")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .setCancelable(true)
                                .show();
                        return;
                    }

                    List<AppointmentData> appointments = new ArrayList<>();
                    for (DataSnapshot apptSnapshot : snapshot.getChildren()) {
                        try {
                            String id = apptSnapshot.getKey();
                            if (id == null) continue;

                            String username = apptSnapshot.child("username").getValue(String.class);
                            String checkupType = apptSnapshot.child("checkupType").getValue(String.class);
                            String time = apptSnapshot.child("time").getValue(String.class);
                            String doctor = apptSnapshot.child("doctor").getValue(String.class);
                            String provider = apptSnapshot.child("provider").getValue(String.class);
                            String virtualNumber = apptSnapshot.child("virtualNumber").getValue(String.class);
                            Boolean confirmed = apptSnapshot.child("confirmed").getValue(Boolean.class);

                            appointments.add(new AppointmentData(id, username, checkupType, time, doctor, provider, virtualNumber, confirmed != null && confirmed));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing appointment: " + e.getMessage(), e);
                        }
                    }

                    if (appointments.isEmpty()) {
                        new AlertDialog.Builder(AppointmentActivity.this)
                                .setTitle("Booked Appointments")
                                .setMessage("No Booked Appointments")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .setCancelable(true)
                                .show();
                        return;
                    }

                    try {
                        View dialogView = LayoutInflater.from(AppointmentActivity.this).inflate(R.layout.dialog_appointments, null);
                        RecyclerView rvAppointments = dialogView.findViewById(R.id.rv_appointments);
                        if (rvAppointments == null) {
                            Log.e(TAG, "RecyclerView rv_appointments not found");
                            Toast.makeText(AppointmentActivity.this, "UI error: RecyclerView not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        rvAppointments.setLayoutManager(new LinearLayoutManager(AppointmentActivity.this));
                        AppointmentAdapter adapter = new AppointmentAdapter(appointments);
                        rvAppointments.setAdapter(adapter);

                        new AlertDialog.Builder(AppointmentActivity.this)
                                .setView(dialogView)
                                .setTitle("Booked Appointments")
                                .setNegativeButton("Close", null)
                                .setCancelable(true)
                                .show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating appointments dialog: " + e.getMessage(), e);
                        Toast.makeText(AppointmentActivity.this, "Failed to display appointments", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading appointments: " + error.getMessage());
                    Toast.makeText(AppointmentActivity.this, "Error loading appointments", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showBookedAppointmentsDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to display appointments", Toast.LENGTH_SHORT).show();
        }
    }

    private static class AppointmentData {
        private final String id, username, checkupType, time, doctor, provider, virtualNumber;
        private boolean confirmed;

        AppointmentData(String id, String username, String checkupType, String time, String doctor, String provider, String virtualNumber, boolean confirmed) {
            this.id = id != null ? id : "";
            this.username = username != null ? username : "";
            this.checkupType = checkupType != null ? checkupType : "";
            this.time = time != null ? time : "";
            this.doctor = doctor != null ? doctor : "";
            this.provider = provider != null ? provider : "";
            this.virtualNumber = virtualNumber != null ? virtualNumber : "";
            this.confirmed = confirmed;
        }

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getCheckupType() { return checkupType; }
        public String getTime() { return time; }
        public String getDoctor() { return doctor; }
        public String getProvider() { return provider; }
        public String getVirtualNumber() { return virtualNumber; }
        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    }

    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
        private final List<AppointmentData> appointments;

        AppointmentAdapter(List<AppointmentData> appointments) {
            this.appointments = appointments != null ? appointments : new ArrayList<>();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            try {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_card, parent, false);
                return new ViewHolder(view);
            } catch (Exception e) {
                Log.e(TAG, "Error inflating item_appointment_card: " + e.getMessage(), e);
                throw new RuntimeException("Failed to inflate appointment card layout", e);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                AppointmentData appt = appointments.get(position);
                holder.tvPatient.setText("Patient: " + appt.getUsername());
                holder.tvType.setText("Type: " + appt.getCheckupType());
                holder.tvTime.setText("Time: " + appt.getTime());
                holder.tvDoctor.setText("Doctor: " + appt.getDoctor());
                holder.tvProvider.setText("Provider: " + appt.getProvider());
                holder.tvVirtualNumber.setText("Virtual #: " + appt.getVirtualNumber());
                holder.tvStatus.setText(appt.isConfirmed() ? "Confirmed" : "Pending");
                holder.tvStatus.setTextColor(appt.isConfirmed() ? getResources().getColor(android.R.color.holo_green_dark) : getResources().getColor(android.R.color.holo_orange_dark));

                holder.btnConfirm.setVisibility(appt.isConfirmed() ? View.GONE : View.VISIBLE);
                holder.btnConfirm.setOnClickListener(v -> confirmAppointment(appt.getId(), position));
                holder.btnDelete.setOnClickListener(v -> deleteAppointment(appt.getId(), position));

                holder.itemView.setOnClickListener(v -> {
                    if (holder.actionContainer != null) {
                        int visibility = holder.actionContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                        holder.actionContainer.setVisibility(visibility);
                    } else {
                        Log.e(TAG, "actionContainer is null at position " + position);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error binding appointment at position " + position + ": " + e.getMessage(), e);
            }
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        private void confirmAppointment(String appointmentId, int position) {
            try {
                if (appointmentId == null || appointmentId.isEmpty()) {
                    Toast.makeText(AppointmentActivity.this, "Invalid appointment ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Confirming appointment: " + appointmentId);
                Map<String, Object> updates = new HashMap<>();
                updates.put("confirmed", true);
                updates.put("notificationSent", false);
                appointmentsRef.child(appointmentId).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            appointments.get(position).setConfirmed(true);
                            notifyItemChanged(position);
                            Toast.makeText(AppointmentActivity.this, "Appointment confirmed", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AppointmentActivity.this, "Failed to confirm appointment", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to confirm appointment: " + e.getMessage());
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error confirming appointment: " + e.getMessage(), e);
                Toast.makeText(AppointmentActivity.this, "Error confirming appointment", Toast.LENGTH_SHORT).show();
            }
        }

        private void deleteAppointment(String appointmentId, int position) {
            try {
                if (appointmentId == null || appointmentId.isEmpty()) {
                    Toast.makeText(AppointmentActivity.this, "Invalid appointment ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "Deleting appointment: " + appointmentId);
                new AlertDialog.Builder(AppointmentActivity.this)
                        .setTitle("Delete Appointment")
                        .setMessage("Are you sure you want to delete this appointment?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            appointmentsRef.child(appointmentId).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        appointments.remove(position);
                                        notifyItemRemoved(position);
                                        Toast.makeText(AppointmentActivity.this, "Appointment deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AppointmentActivity.this, "Failed to delete appointment", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to delete appointment: " + e.getMessage());
                                    });
                        })
                        .setNegativeButton("No", null)
                        .setCancelable(true)
                        .show();
            } catch (Exception e) {
                Log.e(TAG, "Error in deleteAppointment: " + e.getMessage(), e);
                Toast.makeText(AppointmentActivity.this, "Error deleting appointment", Toast.LENGTH_SHORT).show();
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPatient, tvType, tvTime, tvDoctor, tvProvider, tvVirtualNumber, tvStatus;
            Button btnConfirm, btnDelete;
            ViewGroup actionContainer;

            ViewHolder(View itemView) {
                super(itemView);
                try {
                    tvPatient = itemView.findViewById(R.id.tv_patient);
                    tvType = itemView.findViewById(R.id.tv_type);
                    tvTime = itemView.findViewById(R.id.tv_time);
                    tvDoctor = itemView.findViewById(R.id.tv_doctor);
                    tvProvider = itemView.findViewById(R.id.tv_provider);
                    tvVirtualNumber = itemView.findViewById(R.id.tv_virtual_number);
                    tvStatus = itemView.findViewById(R.id.tv_status);
                    btnConfirm = itemView.findViewById(R.id.btn_confirm);
                    btnDelete = itemView.findViewById(R.id.btn_delete);
                    actionContainer = itemView.findViewById(R.id.action_container);
                    if (actionContainer == null) {
                        Log.e(TAG, "actionContainer not found in item_appointment_card layout");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing Appointment ViewHolder: " + e.getMessage(), e);
                }
            }
        }
    }

    private void showBookAppointmentDialog() {
        try {
            Log.d(TAG, "Showing book appointment dialog");
            new AlertDialog.Builder(this)
                    .setTitle("Book Appointment")
                    .setMessage("Do you want to book an appointment?")
                    .setPositiveButton("Yes", (dialog, which) -> showUserInfoDialog())
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showBookAppointmentDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening booking dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserInfoDialog() {
        try {
            Log.d(TAG, "Showing user info dialog for username: " + username);
            usersRef.orderByChild("username").equalTo(username)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                    try {
                                        userId = userSnapshot.getKey();
                                        String firstName = userSnapshot.child("firstName").getValue(String.class);
                                        String lastName = userSnapshot.child("lastName").getValue(String.class);
                                        Long age = userSnapshot.child("age").getValue(Long.class);
                                        String sex = userSnapshot.child("sex").getValue(String.class);
                                        String phoneNumber = userSnapshot.child("phoneNumber").getValue(String.class);

                                        String userInfo = String.format("Name: %s %s\nAge: %s\nSex: %s\nPhone: %s",
                                                firstName != null ? firstName : "",
                                                lastName != null ? lastName : "",
                                                age != null ? age.toString() : "",
                                                sex != null ? sex : "",
                                                phoneNumber != null ? phoneNumber : "");

                                        new AlertDialog.Builder(AppointmentActivity.this)
                                                .setTitle("Confirm User Information")
                                                .setMessage("Is this information correct?\n\n" + userInfo)
                                                .setPositiveButton("Yes", (dialog, which) -> showCheckupTypeDialog())
                                                .setNegativeButton("No", (dialog, which) -> showEditInfoDialog(firstName, lastName, age, sex, phoneNumber))
                                                .setCancelable(true)
                                                .show();
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing user data: " + e.getMessage(), e);
                                        Toast.makeText(AppointmentActivity.this, "Error loading user info", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Log.e(TAG, "User data not found for username: " + username);
                                Toast.makeText(AppointmentActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error loading user data: " + error.getMessage());
                            Toast.makeText(AppointmentActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in showUserInfoDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading user information", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditInfoDialog(String firstName, String lastName, Long age, String sex, String phoneNumber) {
        try {
            Log.d(TAG, "Showing edit info dialog");
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_edit_info, null);
            EditText etFirstName = dialogView.findViewById(R.id.et_first_name);
            EditText etLastName = dialogView.findViewById(R.id.et_last_name);
            EditText etAge = dialogView.findViewById(R.id.et_age);
            EditText etSex = dialogView.findViewById(R.id.et_sex);
            EditText etPhone = dialogView.findViewById(R.id.et_phone);

            etFirstName.setText(firstName != null ? firstName : "");
            etLastName.setText(lastName != null ? lastName : "");
            etAge.setText(age != null ? age.toString() : "");
            etSex.setText(sex != null ? sex : "");
            etPhone.setText(phoneNumber != null ? phoneNumber : "");

            new AlertDialog.Builder(this)
                    .setTitle("Edit User Information")
                    .setView(dialogView)
                    .setPositiveButton("Save", (dialog, which) -> {
                        try {
                            String newFirstName = etFirstName.getText().toString().trim();
                            String newLastName = etLastName.getText().toString().trim();
                            String newAge = etAge.getText().toString().trim();
                            String newSex = etSex.getText().toString().trim();
                            String newPhone = etPhone.getText().toString().trim();

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("firstName", newFirstName);
                            updates.put("lastName", newLastName);
                            if (!newAge.isEmpty()) {
                                try {
                                    updates.put("age", Long.parseLong(newAge));
                                } catch (NumberFormatException e) {
                                    Toast.makeText(AppointmentActivity.this, "Invalid age format", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            updates.put("sex", newSex);
                            updates.put("phoneNumber", newPhone);

                            usersRef.child(userId).updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AppointmentActivity.this, "Information updated", Toast.LENGTH_SHORT).show();
                                        showUserInfoDialog();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AppointmentActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "User info update failed: " + e.getMessage());
                                    });
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving user info: " + e.getMessage(), e);
                            Toast.makeText(AppointmentActivity.this, "Error saving user info", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showEditInfoDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error editing user information", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCheckupTypeDialog() {
        try {
            Log.d(TAG, "Showing checkup type dialog");
            String[] checkupTypes = {"Family Doctor", "ENT", "Pediatrics", "OB-GYN", "Internal Medicine", "Others"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Checkup Type")
                    .setItems(checkupTypes, (dialog, which) -> {
                        String selectedCheckup = checkupTypes[which];
                        if ("Others".equals(selectedCheckup)) {
                            showCustomCheckupDialog();
                        } else {
                            showHealthcareCardDialog(selectedCheckup);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showCheckupTypeDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error selecting checkup type", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomCheckupDialog() {
        try {
            Log.d(TAG, "Showing custom checkup dialog");
            EditText etCustomCheckup = new EditText(this);
            etCustomCheckup.setHint("Specify Checkup Type");
            etCustomCheckup.setTextColor(getResources().getColor(android.R.color.black));
            etCustomCheckup.setHintTextColor(getResources().getColor(android.R.color.darker_gray));

            new AlertDialog.Builder(this)
                    .setTitle("Specify Checkup Type")
                    .setView(etCustomCheckup)
                    .setPositiveButton("OK", (dialog, which) -> {
                        String customCheckup = etCustomCheckup.getText().toString().trim();
                        if (!customCheckup.isEmpty()) {
                            showHealthcareCardDialog(customCheckup);
                        } else {
                            Toast.makeText(this, "Please specify a checkup type", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showCustomCheckupDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error specifying checkup type", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHealthcareCardDialog(String checkupType) {
        try {
            Log.d(TAG, "Showing healthcare card dialog for checkup: " + checkupType);
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_healthcare_card, null);
            EditText editAccountNumber = dialogView.findViewById(R.id.edit_account_number);
            EditText editCardNumber = dialogView.findViewById(R.id.edit_card_number);
            Spinner spinnerProvider = dialogView.findViewById(R.id.spinner_provider);

            String[] providers = {"Maxicare", "Intellicare", "None"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providers);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProvider.setAdapter(adapter);

            spinnerProvider.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    boolean isNone = providers[position].equals("None");
                    editAccountNumber.setVisibility(isNone ? View.GONE : View.VISIBLE);
                    editCardNumber.setVisibility(isNone ? View.GONE : View.VISIBLE);
                    if (isNone) {
                        editAccountNumber.setText("");
                        editCardNumber.setText("");
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            spinnerProvider.setSelection(2);
            editAccountNumber.setVisibility(View.GONE);
            editCardNumber.setVisibility(View.GONE);

            new AlertDialog.Builder(this)
                    .setTitle("Healthcare Card Information")
                    .setView(dialogView)
                    .setPositiveButton("Next", (dialog, which) -> {
                        try {
                            String provider = spinnerProvider.getSelectedItem().toString();
                            String accountNumber = editAccountNumber.getText().toString().trim();
                            String cardNumber = editCardNumber.getText().toString().trim();

                            if (provider.equals("None")) {
                                showTimeAndDoctorDialog(checkupType, provider, "", "");
                            } else if (validateAccountNumber(accountNumber) && validateCardNumber(cardNumber)) {
                                showTimeAndDoctorDialog(checkupType, provider, accountNumber, cardNumber);
                            } else {
                                Toast.makeText(AppointmentActivity.this, "Invalid format: Account (00-00-00000-00000-00), Card (0000-0000-0000-0000)", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing healthcare card: " + e.getMessage(), e);
                            Toast.makeText(AppointmentActivity.this, "Error processing healthcare card", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error in showHealthcareCardDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading healthcare card dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateAccountNumber(String accountNumber) {
        String pattern = "^\\d{2}-\\d{2}-\\d{5}-\\d{5}-\\d{2}$";
        return accountNumber.matches(pattern);
    }

    private boolean validateCardNumber(String cardNumber) {
        String pattern = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$";
        return cardNumber.matches(pattern);
    }

    private void showTimeAndDoctorDialog(String checkupType, String provider, String accountNumber, String cardNumber) {
        try {
            Log.d(TAG, "Showing time and doctor dialog for checkup: " + checkupType);
            timesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> times = new ArrayList<>();
                    if (snapshot.exists()) {
                        for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                            String time = timeSnapshot.child("time").getValue(String.class);
                            if (time != null) {
                                times.add(time);
                            }
                        }
                    }

                    if (times.isEmpty()) {
                        new AlertDialog.Builder(AppointmentActivity.this)
                                .setTitle("No Available Times")
                                .setMessage("No appointment time available. Try again later or contact support.")
                                .setPositiveButton("Retry", (dialogInterface, which) ->
                                        showTimeAndDoctorDialog(checkupType, provider, accountNumber, cardNumber))
                                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                                .setCancelable(true)
                                .show();
                        return;
                    }

                    new AlertDialog.Builder(AppointmentActivity.this)
                            .setTitle("Select Time")
                            .setItems(times.toArray(new String[0]), (dialog, which) -> {
                                String selectedTime = times.get(which);
                                showDoctorDialog(checkupType, provider, accountNumber, cardNumber, selectedTime);
                            })
                            .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                            .setCancelable(true)
                            .show();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading times: " + error.getMessage());
                    Toast.makeText(AppointmentActivity.this, "Error loading times", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showTimeAndDoctorDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading available times", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDoctorDialog(String checkupType, String provider, String accountNumber, String cardNumber, String time) {
        try {
            Log.d(TAG, "Showing doctor dialog for time: " + time);
            timesRef.orderByChild("time").equalTo(time).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    final String[] timeRefKey = new String[1];

                    for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                        timeRefKey[0] = timeSnapshot.getKey();
                    }

                    if (timeRefKey[0] == null) {
                        Toast.makeText(AppointmentActivity.this, "Time slot not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    timesRef.child(timeRefKey[0]).child("doctorAvailability").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            List<String> availableDoctors = new ArrayList<>();
                            String[] allDoctors = {"J.A. Costales", "R.F. Manila", "P.K. Jao", "G. Escueta"};
                            if (snapshot.exists()) {
                                Object doctorData = snapshot.getValue();
                                if (doctorData instanceof ArrayList) {
                                    availableDoctors.addAll((ArrayList<String>) doctorData);
                                } else if (doctorData instanceof String) {
                                    String assignedDoctor = (String) doctorData;
                                    if (assignedDoctor != null && !assignedDoctor.equals("None")) {
                                        availableDoctors.add(assignedDoctor);
                                    }
                                }
                            } else {
                                for (String doctor : allDoctors) {
                                    if (isDoctorAccredited(doctor, provider)) {
                                        availableDoctors.add(doctor);
                                    }
                                }
                            }

                            if (availableDoctors.isEmpty()) {
                                Toast.makeText(AppointmentActivity.this, "No available doctors for this time slot", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new AlertDialog.Builder(AppointmentActivity.this)
                                    .setTitle("Select Doctor")
                                    .setItems(availableDoctors.toArray(new String[0]), (dialog, which) -> {
                                        String selectedDoctor = availableDoctors.get(which);
                                        bookAppointment(checkupType, provider, accountNumber, cardNumber, time, selectedDoctor);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .setCancelable(true)
                                    .show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error loading doctors: " + error.getMessage());
                            Toast.makeText(AppointmentActivity.this, "Error loading doctors", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading times: " + error.getMessage());
                    Toast.makeText(AppointmentActivity.this, "Error loading times", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showDoctorDialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading doctors", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDoctorAccredited(String doctor, String provider) {
        Map<String, List<String>> accreditations = new HashMap<>();
        accreditations.put("J.A. Costales", Arrays.asList("Maxicare", "Intellicare"));
        accreditations.put("R.F. Manila", Arrays.asList("Maxicare"));
        accreditations.put("P.K. Jao", Arrays.asList("Intellicare"));
        accreditations.put("G. Escueta", Arrays.asList("None"));
        return accreditations.getOrDefault(doctor, new ArrayList<>()).contains(provider);
    }

    private void bookAppointment(String checkupType, String provider, String accountNumber, String cardNumber, String time, String doctor) {
        try {
            if (userId == null) {
                Log.e(TAG, "bookAppointment failed: userId is null");
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Booking appointment for userId: " + userId + ", Username: " + username);
            generateVirtualNumberAsync(new GenerateVirtualNumberCallback() {
                @Override
                public void onSuccess(String virtualNumber) {
                    Map<String, Object> appointmentData = new HashMap<>();
                    appointmentData.put("userId", userId);
                    appointmentData.put("username", username);
                    appointmentData.put("checkupType", checkupType);
                    appointmentData.put("time", time);
                    appointmentData.put("doctor", doctor);
                    appointmentData.put("provider", provider);
                    appointmentData.put("accountNumber", accountNumber.isEmpty() ? null : accountNumber);
                    appointmentData.put("cardNumber", cardNumber.isEmpty() ? null : cardNumber);
                    appointmentData.put("virtualNumber", virtualNumber);
                    appointmentData.put("timestamp", ServerValue.TIMESTAMP);
                    appointmentData.put("confirmed", false);
                    appointmentData.put("notificationSent", false);

                    String appointmentId = appointmentsRef.push().getKey();
                    if (appointmentId == null) {
                        Log.e(TAG, "Error generating appointment ID");
                        Toast.makeText(AppointmentActivity.this, "Error generating appointment ID", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d(TAG, "Saving appointment ID: " + appointmentId);
                    appointmentsRef.child(appointmentId).setValue(appointmentData)
                            .addOnSuccessListener(aVoid -> {
                                try {
                                    Log.d(TAG, "Appointment booked successfully: " + appointmentId);
                                    String userMessage = "You successfully booked an appointment at " + time + "! Virtual Number: " + virtualNumber;
                                    sendLocalNotification("Appointment booked", userMessage, appointmentId);
                                    saveNotification(userId, "Appointment booked", userMessage, "booking", appointmentId);
                                    Toast.makeText(AppointmentActivity.this, userMessage, Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in onSuccessListener: " + e.getMessage(), e);
                                    Toast.makeText(AppointmentActivity.this, "Error post-booking: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Appointment booking failed: " + e.getMessage(), e);
                                Toast.makeText(AppointmentActivity.this, "Failed to book appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(AppointmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in bookAppointment: " + e.getMessage(), e);
            Toast.makeText(this, "Error booking appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveNotification(String userId, String title, String message, String type, String appointmentId) {
        if (userId == null) {
            Log.e(TAG, "saveNotification failed: userId is null");
            return;
        }
        Log.d(TAG, "Saving notification for userId: " + userId + ", type: " + type + ", appointmentId: " + appointmentId);
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", userId);
        notificationData.put("title", title);
        notificationData.put("message", message);
        notificationData.put("type", type);
        notificationData.put("appointmentId", appointmentId);
        notificationData.put("timestamp", ServerValue.TIMESTAMP);

        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) {
            Log.e(TAG, "Failed to generate notification ID");
            return;
        }
        notificationsRef.child(notificationId).setValue(notificationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved with ID: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save notification: " + e.getMessage()));
    }

    private void generateVirtualNumberAsync(GenerateVirtualNumberCallback callback) {
        try {
            DatabaseReference lastNumberRef = adminRef.child("lastVirtualNumber");
            lastNumberRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Long lastNumber = currentData.getValue(Long.class);
                    long newNumber = (lastNumber != null ? lastNumber : 0) + 1;
                    if (newNumber > 9999) {
                        newNumber = 1;
                    }
                    currentData.setValue(newNumber);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    if (error != null) {
                        Log.e(TAG, "Transaction failed: " + error.getMessage());
                        callback.onFailure("Failed to generate virtual number");
                    } else if (committed && currentData != null) {
                        long virtualNumber = currentData.getValue(Long.class);
                        callback.onSuccess(String.format("%04d", virtualNumber));
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error generating virtual number: " + e.getMessage(), e);
            callback.onFailure("Error generating virtual number");
        }
    }

    private interface GenerateVirtualNumberCallback {
        void onSuccess(String virtualNumber);
        void onFailure(String errorMessage);
    }

    private void sendLocalNotification(String title, String message, String identifier) {
        try {
            Log.d(TAG, "Sending local notification: " + title);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);
            int id = identifier != null ? identifier.hashCode() : new Random().nextInt(1000);
            manager.notify(id, builder.build());
            Log.d(TAG, "Local notification sent with ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error sending local notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted");
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied");
                Toast.makeText(this, "Notifications disabled. Enable in settings.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adminNotificationListener != null) {
            appointmentsRef.removeEventListener(adminNotificationListener);
            adminNotificationListener = null;
            Log.d(TAG, "Admin notification listener removed in onDestroy");
        }
    }
}