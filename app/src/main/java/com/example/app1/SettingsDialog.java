package com.example.app1;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

public class SettingsDialog extends Dialog {

    public interface SettingsListener {
        void onSoundToggle();
        void onReplay();
        void onMainMenu();
        void onResume();
    }

    private final SettingsListener listener;
    private final boolean isMuted;

    public SettingsDialog(@NonNull Context context, boolean isMuted, SettingsListener listener) {
        super(context);
        this.listener = listener;
        this.isMuted = isMuted;
        init();
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.settings_game);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnSound = findViewById(R.id.btn_settings_sound);
        Button btnReplay = findViewById(R.id.btn_settings_replay);
        Button btnHome = findViewById(R.id.btn_settings_home);
        Button btnResume = findViewById(R.id.btn_settings_resume);

        btnSound.setText(isMuted ? "Sound: Off" : "Sound: On");

        btnSound.setOnClickListener(v -> {
            if (listener != null) listener.onSoundToggle();
            dismiss();
        });

        btnReplay.setOnClickListener(v -> {
            if (listener != null) listener.onReplay();
            dismiss();
        });

        btnHome.setOnClickListener(v -> {
            if (listener != null) listener.onMainMenu();
            dismiss();
        });

        btnResume.setOnClickListener(v -> {
            if (listener != null) listener.onResume();
            dismiss();
        });

        setCancelable(false);
    }
}
