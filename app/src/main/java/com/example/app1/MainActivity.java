package com.example.app1;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();
    }

    private void setupButtons() {
        ImageButton btnPlay = findViewById(R.id.btn_main_play);
        Button btnContinue = findViewById(R.id.btn_main_load);
        Button btnSettings = findViewById(R.id.btn_main_settings);

        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        btnContinue.setOnClickListener(v -> {
            // Placeholder for load logic
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Resuming Game...", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> showMainSettingsDialog());
    }

    @SuppressLint("SetTextI18n")
    private void showMainSettingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.settings_game);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnSound = dialog.findViewById(R.id.btn_settings_sound);
        Button btnReplay = dialog.findViewById(R.id.btn_settings_replay);
        Button btnHome = dialog.findViewById(R.id.btn_settings_home);
        Button btnResume = dialog.findViewById(R.id.btn_settings_resume);

        btnReplay.setEnabled(false);
        btnReplay.setAlpha(0.5f);
        
        btnResume.setText("CLOSE");
        btnResume.setOnClickListener(v -> dialog.dismiss());

        btnHome.setText("ABOUT");
        btnHome.setOnClickListener(v -> Toast.makeText(this, "Pikachu Version 2024", Toast.LENGTH_SHORT).show());

        btnSound.setOnClickListener(v -> Toast.makeText(this, "Sound Settings Updated", Toast.LENGTH_SHORT).show());

        dialog.show();
    }
}
