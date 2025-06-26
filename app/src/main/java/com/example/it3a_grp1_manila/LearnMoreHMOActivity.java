package com.example.it3a_grp1_manila;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class LearnMoreHMOActivity extends AppCompatActivity {

    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learnmore_hmo);

        backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}
