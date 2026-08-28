package com.example.app1;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
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

    private static final String PREFS_NAME = "PikachuPrefs";

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

    @Override
    protected void onResume() {
        super.onResume();
        updateContinueButtonState();
    }

    private void updateContinueButtonState() {
        Button btnContinue = findViewById(R.id.btn_main_load);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasSavedGame = prefs.contains("board");
        btnContinue.setEnabled(hasSavedGame);
        btnContinue.setAlpha(hasSavedGame ? 1.0f : 0.5f);
    }

    private void setupButtons() {
        ImageButton btnPlay = findViewById(R.id.btn_main_play);
        Button btnContinue = findViewById(R.id.btn_main_load);
        Button btnSettings = findViewById(R.id.btn_main_settings);

        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("CONTINUE", false);
            startActivity(intent);
        });

        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("CONTINUE", true);
            startActivity(intent);
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

        btnReplay.setVisibility(View.GONE);
        
        btnResume.setText("CLOSE");
        btnResume.setOnClickListener(v -> dialog.dismiss());

        btnHome.setText("RESET DATA");
        btnHome.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
            updateContinueButtonState();
            Toast.makeText(this, "Data Reset", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnSound.setOnClickListener(v -> Toast.makeText(this, "Sound Settings Updated", Toast.LENGTH_SHORT).show());

        dialog.show();
    }
}
