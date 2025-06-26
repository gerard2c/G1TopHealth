package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private EditText lastName, firstName, username, phoneNumber, password, confirmPassword;
    private CheckBox agreementCheckbox;
    private Button signupButton;
    private TextView loginHere;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Initialize UI elements
        lastName = findViewById(R.id.last_name);
        firstName = findViewById(R.id.first_name);
        username = findViewById(R.id.username);
        phoneNumber = findViewById(R.id.phoneNumber);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        agreementCheckbox = findViewById(R.id.checkbox_agreement);
        signupButton = findViewById(R.id.btn_signup);
        loginHere = findViewById(R.id.loginhere);

        // Initialize default admin account
        initializeAdminAccount();

        // Sign Up button click listener
        signupButton.setOnClickListener(v -> registerUser());

        // Login here text click listener
        loginHere.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initializeAdminAccount() {
        String adminUsername = "adminTopHealth";
        String adminEmail = adminUsername + "@it3a.com";
        String adminPassword = "password123";

        mAuth.signInWithEmailAndPassword(adminEmail, adminPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Admin account exists
                        Log.d(TAG, "Admin account already exists");
                    } else {
                        // Create admin account
                        mAuth.createUserWithEmailAndPassword(adminEmail, adminPassword)
                                .addOnCompleteListener(createTask -> {
                                    if (createTask.isSuccessful()) {
                                        FirebaseUser user = createTask.getResult().getUser();
                                        if (user != null) {
                                            String userId = user.getUid();
                                            Map<String, Object> adminData = new HashMap<>();
                                            adminData.put("lastName", "Admin");
                                            adminData.put("firstName", "TopHealth");
                                            adminData.put("username", adminUsername);
                                            adminData.put("phoneNumber", "1234567890");
                                            adminData.put("isAdmin", true);

                                            usersRef.child(userId).setValue(adminData)
                                                    .addOnCompleteListener(dataTask -> {
                                                        if (dataTask.isSuccessful()) {
                                                            Log.d(TAG, "Admin account created successfully");
                                                        } else {
                                                            Log.e(TAG, "Failed to store admin data", dataTask.getException());
                                                        }
                                                    });
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to create admin account", createTask.getException());
                                    }
                                    // Sign out after creating admin
                                    mAuth.signOut();
                                });
                    }
                });
    }

    private void registerUser() {
        String lastNameStr = lastName.getText().toString().trim();
        String firstNameStr = firstName.getText().toString().trim();
        String usernameStr = username.getText().toString().trim();
        String phoneNumberStr = phoneNumber.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();
        String confirmPasswordStr = confirmPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(lastNameStr)) {
            lastName.setError("Last name is required");
            return;
        }
        if (TextUtils.isEmpty(firstNameStr)) {
            firstName.setError("First name is required");
            return;
        }
        if (TextUtils.isEmpty(usernameStr)) {
            username.setError("Username is required");
            return;
        }
        if (usernameStr.toLowerCase().contains("admin")) {
            username.setError("Usernames cannot contain 'admin'");
            Toast.makeText(this, "Usernames with 'admin' are reserved.", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(phoneNumberStr)) {
            phoneNumber.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(passwordStr)) {
            password.setError("Password is required");
            return;
        }
        if (TextUtils.isEmpty(confirmPasswordStr)) {
            confirmPassword.setError("Please confirm your password");
            return;
        }
        if (!passwordStr.equals(confirmPasswordStr)) {
            confirmPassword.setError("Passwords do not match");
            return;
        }
        if (passwordStr.length() < 6) {
            password.setError("Password must be at least 6 characters");
            return;
        }
        if (!agreementCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms of Use and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create email from username
        String email = usernameStr + "@it3a.com";

        // Check if username exists
        usersRef.orderByChild("username").equalTo(usernameStr)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(RegisterActivity.this, "Username already exists.", Toast.LENGTH_LONG).show();
                        } else {
                            // Register user with Firebase Authentication
                            mAuth.createUserWithEmailAndPassword(email, passwordStr)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null) {
                                                String userId = user.getUid();
                                                Map<String, Object> userData = new HashMap<>();
                                                userData.put("lastName", lastNameStr);
                                                userData.put("firstName", firstNameStr);
                                                userData.put("username", usernameStr);
                                                userData.put("phoneNumber", phoneNumberStr);
                                                userData.put("isAdmin", false);

                                                usersRef.child(userId).setValue(userData)
                                                        .addOnCompleteListener(dataTask -> {
                                                            if (dataTask.isSuccessful()) {
                                                                Toast.makeText(RegisterActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();
                                                                // Sign out to enforce login
                                                                mAuth.signOut();
                                                                // Redirect to LoginActivity with intent to go to HomeActivity
                                                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                                intent.putExtra("redirectToHome", true);
                                                                startActivity(intent);
                                                                finish();
                                                            } else {
                                                                Toast.makeText(RegisterActivity.this, "Failed to store user data: " + dataTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                                Log.e(TAG, "Failed to store user data", dataTask.getException());
                                                            }
                                                        });
                                            }
                                        } else {
                                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                            if (errorMessage.contains("Permission denied")) {
                                                Toast.makeText(RegisterActivity.this, "Registration failed: Permission denied. Check Firebase Database rules.", Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                            }
                                            Log.e(TAG, "Registration failed", task.getException());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        String errorMessage = error.getMessage();
                        if (errorMessage.contains("Permission denied")) {
                            Toast.makeText(RegisterActivity.this, "Error: Permission denied.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "Database error", error.toException());
                    }
                });
    }
}