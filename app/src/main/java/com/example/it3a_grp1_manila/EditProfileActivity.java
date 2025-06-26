package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfileActivity extends AppCompatActivity {
    private EditText editFirstName, editLastName, editAge, editContactNumber;
    private Spinner spinnerSex;
    private Button saveButton;
    private DatabaseReference usersRef;
    private SharedPreferences prefs;
    private String username;
    private String userId; // To store the Firebase user ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Initialize UI elements
        editFirstName = findViewById(R.id.edit_first_name);
        editLastName = findViewById(R.id.edit_last_name);
        editAge = findViewById(R.id.edit_age);
        editContactNumber = findViewById(R.id.edit_contact_number);
        spinnerSex = findViewById(R.id.spinner_sex);
        saveButton = findViewById(R.id.save_button);

        // Set up Spinner for sex
        String[] sexOptions = {"Select Sex", "Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sexOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSex.setAdapter(adapter);

        // Get username from Intent
        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = prefs.getString("username", "User");
        }

        // Initialize Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Load current profile data
        usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String firstName = userSnapshot.child("firstName").getValue(String.class);
                                String lastName = userSnapshot.child("lastName").getValue(String.class);
                                Long age = userSnapshot.child("age").getValue(Long.class);
                                String sex = userSnapshot.child("sex").getValue(String.class);
                                String phoneNumber = userSnapshot.child("phoneNumber").getValue(String.class);
                                userId = userSnapshot.getKey(); // Store the user ID
                                if (firstName != null && lastName != null) {
                                    editFirstName.setText(firstName);
                                    editLastName.setText(lastName);
                                }
                                if (age != null) {
                                    editAge.setText(String.valueOf(age));
                                }
                                if (sex != null) {
                                    int spinnerPosition = adapter.getPosition(sex);
                                    spinnerSex.setSelection(spinnerPosition);
                                }
                                if (phoneNumber != null) {
                                    editContactNumber.setText(phoneNumber);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(EditProfileActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
                    }
                });

        // Set up save button click listener
        saveButton.setOnClickListener(v -> {
            String newFirstName = editFirstName.getText().toString().trim();
            String newLastName = editLastName.getText().toString().trim();
            String newAgeStr = editAge.getText().toString().trim();
            String newSex = spinnerSex.getSelectedItem().toString();
            String newPhoneNumber = editContactNumber.getText().toString().trim();

            if (!newFirstName.isEmpty() && !newLastName.isEmpty() && !newAgeStr.isEmpty() && !newPhoneNumber.isEmpty()) {
                try {
                    int newAge = Integer.parseInt(newAgeStr);
                    if (newAge > 0 && userId != null) {
                        usersRef.child(userId).child("firstName").setValue(newFirstName);
                        usersRef.child(userId).child("lastName").setValue(newLastName);
                        usersRef.child(userId).child("age").setValue(newAge);
                        usersRef.child(userId).child("sex").setValue(newSex);
                        usersRef.child(userId).child("phoneNumber").setValue(newPhoneNumber);
                        Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Age must be a positive number", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid age format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }
}