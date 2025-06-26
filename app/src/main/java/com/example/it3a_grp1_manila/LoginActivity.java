package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvSignUp;
    private DatabaseReference usersRef;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Check if user is already logged in
        if (prefs.getBoolean("isLoggedIn", false)) {
            String username = prefs.getString("username", "User");
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Initialize views
        etUsername = findViewById(R.id.et_email); // Assuming this is now used for username
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvSignUp = findViewById(R.id.tv_sign_up);

        // Login button click listener
        btnLogin.setOnClickListener(view -> loginUser());

        // Navigate to RegisterActivity when user clicks "Sign Up"
        tvSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        // Query the database for the username
        usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Username found, check password
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String storedPassword = userSnapshot.child("password").getValue(String.class);
                                String firstName = userSnapshot.child("firstName").getValue(String.class);
                                String lastName = userSnapshot.child("lastName").getValue(String.class);

                                if (storedPassword != null && storedPassword.equals(password)) {
                                    // Successful login
                                    Toast.makeText(LoginActivity.this, "Welcome " + firstName + " " + lastName, Toast.LENGTH_LONG).show();
                                    // Save login state, username, and firstName
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean("isLoggedIn", true);
                                    editor.putString("username", username);
                                    editor.putString("firstName", firstName); // Save firstName
                                    editor.apply();
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    intent.putExtra("username", username);
                                    intent.putExtra("firstName", firstName); // Pass firstName
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Incorrect password
                                    Toast.makeText(LoginActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            // Username not found
                            Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Database error", error.toException());
                    }
                });
    }

}