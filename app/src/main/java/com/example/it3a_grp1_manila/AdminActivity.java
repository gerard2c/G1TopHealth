package com.example.it3a_grp1_manila;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";
    private DatabaseReference timesRef, doctorAvailabilityRef;
    private RecyclerView recyclerView;
    private TimeSlotAdapter adapter;
    private List<String> timeSlots;
    private Button addTimeSlotButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        timesRef = database.getReference("admin/availableTimes");
        doctorAvailabilityRef = database.getReference("admin/availableTimes"); // Adjusted to match structure
        mAuth = FirebaseAuth.getInstance();

        // Check if user is admin
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in as admin", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        checkAdminStatus(user.getUid());

        // Initialize UI
        recyclerView = findViewById(R.id.recycler_time_slots);
        addTimeSlotButton = findViewById(R.id.btn_add_time_slot);
        timeSlots = new ArrayList<>();
        adapter = new TimeSlotAdapter(timeSlots);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load time slots
        loadTimeSlots();

        // Add new time slot
        addTimeSlotButton.setOnClickListener(v -> showAddTimeSlotDialog());
    }

    private void checkAdminStatus(String uid) {
        DatabaseReference adminsRef = FirebaseDatabase.getInstance().getReference("admins").child(uid);
        adminsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(AdminActivity.this, "Access denied: Not an admin", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error checking admin status", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadTimeSlots() {
        timesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                timeSlots.clear();
                for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                    String time = timeSnapshot.child("time").getValue(String.class); // Assuming time is a child
                    if (time != null) {
                        timeSlots.add(time);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error loading time slots: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTimeSlotDialog() {
        EditText etTimeSlot = new EditText(this);
        etTimeSlot.setHint("Enter time (e.g., 09:00 AM)");
        new AlertDialog.Builder(this)
                .setTitle("Add Time Slot")
                .setView(etTimeSlot)
                .setPositiveButton("Add", (dialog, which) -> {
                    String time = etTimeSlot.getText().toString().trim();
                    if (!time.isEmpty()) {
                        DatabaseReference newTimeRef = timesRef.push();
                        String key = newTimeRef.getKey();
                        Map<String, Object> timeData = new HashMap<>();
                        timeData.put("time", time);
                        newTimeRef.setValue(timeData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminActivity.this, "Time slot added", Toast.LENGTH_SHORT).show();
                                    // Initialize doctor availability for this key
                                    Map<String, Object> initialAvailability = new HashMap<>();
                                    initialAvailability.put("J.A. Costales", false);
                                    initialAvailability.put("R.F. Manila", false);
                                    initialAvailability.put("P.K. Jao", false);
                                    initialAvailability.put("G. Escueta", false);
                                    newTimeRef.child("doctorAvailability").setValue(initialAvailability)
                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to init doctor availability", e));
                                    loadTimeSlots();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AdminActivity.this, "Failed to add time slot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Failed to add time slot", e);
                                });
                    } else {
                        Toast.makeText(this, "Please enter a valid time", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {
        private List<String> timeSlots;

        TimeSlotAdapter(List<String> timeSlots) {
            this.timeSlots = timeSlots;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String time = timeSlots.get(position);
            holder.tvTimeSlot.setText(time);
            holder.btnEditDoctors.setOnClickListener(v -> showEditDoctorsDialog(time));
            holder.btnDelete.setOnClickListener(v -> deleteTimeSlot(time));
        }

        @Override
        public int getItemCount() {
            return timeSlots.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTimeSlot;
            Button btnEditDoctors, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvTimeSlot = itemView.findViewById(R.id.tv_time_slot);
                btnEditDoctors = itemView.findViewById(R.id.btn_edit_doctors);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    private void showEditDoctorsDialog(String time) {
        try {
            timesRef.orderByChild("time").equalTo(time).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String foundKey = null;
                    for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                        foundKey = timeSnapshot.getKey();
                    }

                    if (foundKey == null) {
                        Toast.makeText(AdminActivity.this, "Time slot not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final String key = foundKey; // ✅ Final copy for use inside lambdas

                    LayoutInflater inflater = LayoutInflater.from(AdminActivity.this);
                    View dialogView = inflater.inflate(R.layout.dialog_edit_doctors, null);
                    CheckBox cbCostales = dialogView.findViewById(R.id.cb_costales);
                    CheckBox cbManila = dialogView.findViewById(R.id.cb_manila);
                    CheckBox cbJao = dialogView.findViewById(R.id.cb_jao);
                    CheckBox cbEscueta = dialogView.findViewById(R.id.cb_escueta);

                    timesRef.child(key).child("doctorAvailability").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            cbCostales.setChecked(snapshot.child("J.A Costales").getValue(Boolean.class) != null && snapshot.child("Dr. Costales").getValue(Boolean.class));
                            cbManila.setChecked(snapshot.child("R.F Manila").getValue(Boolean.class) != null && snapshot.child("Dr. Manila").getValue(Boolean.class));
                            cbJao.setChecked(snapshot.child("P.K Jao").getValue(Boolean.class) != null && snapshot.child("Dr. Jao").getValue(Boolean.class));
                            cbEscueta.setChecked(snapshot.child("G. Escueta").getValue(Boolean.class) != null && snapshot.child("Dr. Escueta").getValue(Boolean.class));

                            new AlertDialog.Builder(AdminActivity.this)
                                    .setTitle("Edit Doctors for " + time)
                                    .setView(dialogView)
                                    .setPositiveButton("Save", (dialog, which) -> {
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("J.A. Costales", cbCostales.isChecked());
                                        updates.put("R.F Manila", cbManila.isChecked());
                                        updates.put("P.K Jao", cbJao.isChecked());
                                        updates.put("G. Escueta", cbEscueta.isChecked());

                                        timesRef.child(key).child("doctorAvailability").setValue(updates)
                                                .addOnSuccessListener(aVoid -> Toast.makeText(AdminActivity.this, "Doctor availability updated", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(AdminActivity.this, "Failed to update doctors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    Log.e(TAG, "Failed to update doctors", e);
                                                });
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(AdminActivity.this, "Error loading doctor availability: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to load doctor availability", error.toException());
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(AdminActivity.this, "Error finding time slot key", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to find time slot key", error.toException());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in showEditDoctorsDialog for time: " + time, e);
            Toast.makeText(this, "An error occurred while loading the doctor dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTimeSlot(String time) {
        timesRef.orderByChild("time").equalTo(time).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot timeSnapshot : snapshot.getChildren()) {
                    timeSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error deleting time slot", Toast.LENGTH_SHORT).show();
            }
        });
    }
}