package com.example.app1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private static final int DEBUG_START_LEVEL = 1;
    private static final int TOTAL_ROWS = 9;
    private static final int TOTAL_COLS = 16;
    private static final int TOTAL_POKEMON_TYPES = 18;
    private static final int GAME_TIME_IN_SECONDS = 260;
    private static final int INITIAL_SHUFFLES = 10;
    private static final String PREFS_NAME = "PikachuPrefs";

    private TextView tvLevel, tvShuffleCount, tvScore;
    private ImageButton btnPause, btnShuffle;
    private ProgressBar timeProgressBar;
    private GameBoardView gameBoardView;

    private int[][] board;
    private int currentLevel = DEBUG_START_LEVEL;
    private int currentScore = 0;
    private int shufflesLeft = INITIAL_SHUFFLES;
    private Point firstSelection = null;
    private int remainingPairs;

    private boolean isMuted = false;
    private long timeRemainingMillis = GAME_TIME_IN_SECONDS * 1000L;
    private MediaPlayer backgroundMusicPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer gameTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_game);

        mapUIComponents();
        setupButtonListeners();
        loadResources();
        playBackgroundMusic();
    }

    private void mapUIComponents() {
        tvLevel = findViewById(R.id.tv_level);
        tvShuffleCount = findViewById(R.id.tv_shuffle_count);
        tvScore = findViewById(R.id.tv_score);
        btnPause = findViewById(R.id.btn_pause);
        btnShuffle = findViewById(R.id.btn_shuffle);
        timeProgressBar = findViewById(R.id.time_progress_bar);
        gameBoardView = findViewById(R.id.game_board_view);
    }

    private void setupButtonListeners() {
        btnShuffle.setOnClickListener(v -> handleShuffle());
        btnPause.setOnClickListener(v -> showSettingsDialog());
    }

    private void playBackgroundMusic() {
        if (backgroundMusicPlayer == null) {
            backgroundMusicPlayer = MediaPlayer.create(this, R.raw.background_music);
            backgroundMusicPlayer.setLooping(true);
        }
        if (!backgroundMusicPlayer.isPlaying() && !isMuted) {
            backgroundMusicPlayer.start();
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.pause();
            }
            Toast.makeText(this, "Sound Off", Toast.LENGTH_SHORT).show();
        } else {
            if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.start();
            }
            Toast.makeText(this, "Sound On", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadResources() {
        final Bitmap[] pokemonImages = new Bitmap[TOTAL_POKEMON_TYPES + 1];
        final int[] loadedCount = {0};

        Toast.makeText(this, "Loading Pokemon from PokéAPI...", Toast.LENGTH_SHORT).show();

        for (int i = 1; i <= TOTAL_POKEMON_TYPES; i++) {
            final int index = i;
            String url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + i + ".png";

            Glide.with(this)
                    .asBitmap()
                    .load(url)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            pokemonImages[index] = resource;
                            checkProgress();
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) { }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            checkProgress();
                        }

                        private void checkProgress() {
                            loadedCount[0]++;
                            if (loadedCount[0] == TOTAL_POKEMON_TYPES) {
                                runOnUiThread(() -> {
                                    gameBoardView.setPokemonImages(pokemonImages);
                                    boolean shouldContinue = getIntent().getBooleanExtra("CONTINUE", false);
                                    if (shouldContinue) {
                                        loadGameState();
                                    } else {
                                        startNewGame();
                                    }
                                });
                            }
                        }
                    });
        }
    }

    private void startNewGame() {
        board = new int[TOTAL_ROWS + 2][TOTAL_COLS + 2];
        remainingPairs = (TOTAL_ROWS * TOTAL_COLS) / 2;

        List<Integer> pokemonIDs = new ArrayList<>();
        for (int i = 0; i < remainingPairs; i++) {
            int pokemonType = (i % TOTAL_POKEMON_TYPES) + 1;
            pokemonIDs.add(pokemonType);
            pokemonIDs.add(pokemonType);
        }
        Collections.shuffle(pokemonIDs);

        int k = 0;
        for (int i = 1; i <= TOTAL_ROWS; i++) {
            for (int j = 1; j <= TOTAL_COLS; j++) {
                board[i][j] = pokemonIDs.get(k++);
            }
        }

        gameBoardView.setBoard(board);
        gameBoardView.setOnTileClickListener(this::handleTileClick);
        timeRemainingMillis = GAME_TIME_IN_SECONDS * 1000L;
        updateUI();
        startTimer(timeRemainingMillis);
    }

    private void startTimer(long millisInFuture) {
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        gameTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingMillis = millisUntilFinished;
                int progress = (int) (millisUntilFinished * 100 / (GAME_TIME_IN_SECONDS * 1000));
                timeProgressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                timeProgressBar.setProgress(0);
                showGameOverDialog();
            }
        };

        gameTimer.start();
    }

    private void showSettingsDialog() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        SettingsDialog dialog = new SettingsDialog(this, isMuted, new SettingsDialog.SettingsListener() {
            @Override
            public void onSoundToggle() {
                toggleMute();
                showSettingsDialog();
            }

            @Override
            public void onReplay() {
                restartGameFromBeginning();
            }

            @Override
            public void onMainMenu() {
                saveGameState();
                finish();
            }

            @Override
            public void onResume() {
                startTimer(timeRemainingMillis);
            }
        });
        dialog.show();
    }

    private void showGameOverDialog() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_over);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button btnReplay = dialog.findViewById(R.id.btn_dialog_replay);
        Button btnHome = dialog.findViewById(R.id.btn_dialog_home);

        btnReplay.setOnClickListener(v -> {
            dialog.dismiss();
            restartGameFromBeginning();
        });

        btnHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void handleTileClick(int row, int col) {
        if (board[row][col] == 0) return;

        if (firstSelection == null) {
            firstSelection = new Point(col, row);
            gameBoardView.setSelectedTile(firstSelection);
        } else {
            Point secondSelection = new Point(col, row);
            if (firstSelection.equals(secondSelection)) {
                firstSelection = null;
                gameBoardView.setSelectedTile(null);
                return;
            }

            if (board[firstSelection.y][firstSelection.x] == board[secondSelection.y][secondSelection.x]) {
                List<Point> path = findPath(firstSelection, secondSelection);
                if (path != null) {
                    final Point p1 = firstSelection;
                    final Point p2 = secondSelection;
                    firstSelection = null;
                    
                    gameBoardView.setEnabled(false);
                    gameBoardView.drawPath(path);

                    handler.postDelayed(() -> {
                        board[p1.y][p1.x] = 0;
                        board[p2.y][p2.x] = 0;
                        
                        gameBoardView.clearPathAndSelection();
                        currentScore += 10;
                        remainingPairs--;
                        updateUI();
                        checkGameState();
                        gameBoardView.setEnabled(true);
                    }, 300);
                } else {
                    firstSelection = null;
                    gameBoardView.setSelectedTile(null);
                }
            } else {
                firstSelection = secondSelection;
                gameBoardView.setSelectedTile(firstSelection);
            }
        }
    }

    private void checkGameState() {
        if (remainingPairs == 0) {
            handleWin();
        } else if (!isMoveAvailable()) {
            Toast.makeText(this, "No more moves! Shuffling...", Toast.LENGTH_SHORT).show();
            handleShuffle();
        }
    }

    private void handleShuffle() {
        if (shufflesLeft > 0) {
            shufflesLeft--;
            List<Integer> ids = new ArrayList<>();
            for (int i = 1; i <= TOTAL_ROWS; i++) {
                for (int j = 1; j <= TOTAL_COLS; j++) {
                    if (board[i][j] != 0) ids.add(board[i][j]);
                }
            }
            Collections.shuffle(ids);
            int k = 0;
            for (int i = 1; i <= TOTAL_ROWS; i++) {
                for (int j = 1; j <= TOTAL_COLS; j++) {
                    if (board[i][j] != 0) board[i][j] = ids.get(k++);
                }
            }
            gameBoardView.invalidate();
            updateUI();
        }
    }

    private void handleWin() {
        if (gameTimer != null) gameTimer.cancel();
        Toast.makeText(this, "Victory!", Toast.LENGTH_LONG).show();
        currentLevel++;
        startNewGame();
    }

    private void updateUI() {
        tvLevel.setText(String.valueOf(currentLevel));
        tvScore.setText(String.valueOf(currentScore));
        tvShuffleCount.setText(String.valueOf(shufflesLeft));
        gameBoardView.invalidate();
    }

    // --- LOGIC TÌM ĐƯỜNG (PATHFINDING) ---
    
    private List<Point> findPath(Point p1, Point p2) {
        // 1. Đường thẳng
        if (checkLine(p1, p2)) {
            List<Point> path = new ArrayList<>();
            path.add(p1);
            path.add(p2);
            return path;
        }

        // 2. Đường chữ L (1 góc vuông)
        Point corner = checkLPath(p1, p2);
        if (corner != null) {
            List<Point> path = new ArrayList<>();
            path.add(p1);
            path.add(corner);
            path.add(p2);
            return path;
        }

        // 3. Đường chữ U/Z (2 góc vuông)
        return checkTwoCornersPath(p1, p2);
    }

    private boolean checkLine(Point p1, Point p2) {
        if (p1.x == p2.x) {
            int start = Math.min(p1.y, p2.y);
            int end = Math.max(p1.y, p2.y);
            for (int i = start + 1; i < end; i++) {
                if (board[i][p1.x] != 0) return false;
            }
            return true;
        } else if (p1.y == p2.y) {
            int start = Math.min(p1.x, p2.x);
            int end = Math.max(p1.x, p2.x);
            for (int i = start + 1; i < end; i++) {
                if (board[p1.y][i] != 0) return false;
            }
            return true;
        }
        return false;
    }

    private Point checkLPath(Point p1, Point p2) {
        Point c1 = new Point(p2.x, p1.y);
        if (board[c1.y][c1.x] == 0 && checkLine(p1, c1) && checkLine(c1, p2)) return c1;
        
        Point c2 = new Point(p1.x, p2.y);
        if (board[c2.y][c2.x] == 0 && checkLine(p1, c2) && checkLine(c2, p2)) return c2;
        
        return null;
    }

    private List<Point> checkTwoCornersPath(Point p1, Point p2) {
        // Quét ngang
        for (int x = 0; x < board[0].length; x++) {
            if (x == p1.x) continue;
            Point t1 = new Point(x, p1.y);
            if (board[t1.y][t1.x] == 0 && checkLine(p1, t1)) {
                Point t2 = checkLPath(t1, p2);
                if (t2 != null) {
                    List<Point> path = new ArrayList<>();
                    path.add(p1);
                    path.add(t1);
                    path.add(t2);
                    path.add(p2);
                    return path;
                }
            }
        }
        // Quét dọc
        for (int y = 0; y < board.length; y++) {
            if (y == p1.y) continue;
            Point t1 = new Point(p1.x, y);
            if (board[t1.y][t1.x] == 0 && checkLine(p1, t1)) {
                Point t2 = checkLPath(t1, p2);
                if (t2 != null) {
                    List<Point> path = new ArrayList<>();
                    path.add(p1);
                    path.add(t1);
                    path.add(t2);
                    path.add(p2);
                    return path;
                }
            }
        }
        return null;
    }

    private boolean isMoveAvailable() {
        List<Point> tiles = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ROWS; i++) {
            for (int j = 1; j <= TOTAL_COLS; j++) {
                if (board[i][j] != 0) tiles.add(new Point(j, i));
            }
        }
        for (int i = 0; i < tiles.size(); i++) {
            for (int j = i + 1; j < tiles.size(); j++) {
                Point p1 = tiles.get(i);
                Point p2 = tiles.get(j);
                if (board[p1.y][p1.x] == board[p2.y][p2.x] && findPath(p1, p2) != null) return true;
            }
        }
        return false;
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctrl = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ctrl.hide(WindowInsetsCompat.Type.systemBars());
        ctrl.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void restartGameFromBeginning() {
        if (gameTimer != null) gameTimer.cancel();
        currentLevel = DEBUG_START_LEVEL;
        currentScore = 0;
        shufflesLeft = INITIAL_SHUFFLES;
        startNewGame();
    }

    private void saveGameState() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt("level", currentLevel);
        editor.putInt("score", currentScore);
        editor.putInt("shuffles", shufflesLeft);
        editor.putLong("time", timeRemainingMillis);
        editor.putInt("remainingPairs", remainingPairs);
        StringBuilder sb = new StringBuilder();
        for (int[] rows : board) {
            for (int val : rows) sb.append(val).append(",");
        }
        editor.putString("board", sb.toString());
        editor.apply();
    }

    private void loadGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLevel = prefs.getInt("level", 1);
        currentScore = prefs.getInt("score", 0);
        shufflesLeft = prefs.getInt("shuffles", 10);
        timeRemainingMillis = prefs.getLong("time", GAME_TIME_IN_SECONDS * 1000L);
        remainingPairs = prefs.getInt("remainingPairs", 0);
        String boardStr = prefs.getString("board", "");
        if (!boardStr.isEmpty()) {
            String[] parts = boardStr.split(",");
            board = new int[TOTAL_ROWS + 2][TOTAL_COLS + 2];
            int k = 0;
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[0].length; j++) board[i][j] = Integer.parseInt(parts[k++]);
            }
        }
        gameBoardView.setBoard(board);
        gameBoardView.setOnTileClickListener(this::handleTileClick);
        updateUI();
        startTimer(timeRemainingMillis);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) backgroundMusicPlayer.pause();
        saveGameState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying() && !isMuted) backgroundMusicPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) gameTimer.cancel();
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
        }
    }
}
