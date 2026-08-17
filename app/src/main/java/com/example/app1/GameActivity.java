package com.example.app1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.app.Dialog;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
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

    private TextView tvLevel, tvShuffleCount, tvScore;
    private ImageButton btnPause, btnShuffle, btnSetting;
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
        startNewGame();
        playBackgroundMusic();
    }

    private void mapUIComponents() {
        tvLevel = findViewById(R.id.tv_level);
        tvShuffleCount = findViewById(R.id.tv_shuffle_count);
        tvScore = findViewById(R.id.tv_score);
        btnPause = findViewById(R.id.btn_pause);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnSetting = findViewById(R.id.btn_settings);
        timeProgressBar = findViewById(R.id.time_progress_bar);
        gameBoardView = findViewById(R.id.game_board_view);
    }

    private void setupButtonListeners() {
        btnShuffle.setOnClickListener(v -> handleShuffle());
        btnPause.setOnClickListener(v -> showSettingsDialog());
        btnSetting.setOnClickListener(v -> showSettingsDialog());
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
        Bitmap[] pokemonImages = new Bitmap[TOTAL_POKEMON_TYPES + 1];

        final TypedArray ids = getResources().obtainTypedArray(R.array.pokemon_drawables);
        for (int i = 0; i < ids.length(); i++) {
            int resourceId = ids.getResourceId(i, 0);
            if (resourceId != 0) {
                pokemonImages[i + 1] = BitmapFactory.decodeResource(getResources(), resourceId);
            }
        }
        ids.recycle();

        gameBoardView.setPokemonImages(pokemonImages);
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
        updateUI();

        timeRemainingMillis = GAME_TIME_IN_SECONDS * 1000L;
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
                    final Point finalFirstSelection = firstSelection;
                    final Point finalSecondSelection = secondSelection;
                    firstSelection = null;
                    gameBoardView.setEnabled(false);
                    gameBoardView.drawPath(path);

                    handler.postDelayed(() -> {
                        board[finalFirstSelection.y][finalFirstSelection.x] = 0;
                        board[finalSecondSelection.y][finalSecondSelection.x] = 0;

                        if (currentLevel == 14) {
                            shiftAllRowsDown();
                        } else if (currentLevel == 15) {
                            shiftAllRowsUp();
                        } else {
                            shiftBoard(finalFirstSelection, finalSecondSelection);
                        }

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
                firstSelection = null;
                gameBoardView.setSelectedTile(null);
            }
        }
    }

    private void checkGameState() {
        if (remainingPairs == 0) {
            handleWin();
        } else if (!isMoveAvailable()) {
            Toast.makeText(this, "No more moves! Automatic shuffle.", Toast.LENGTH_SHORT).show();
            handleShuffle();
        }
    }

    private void handleShuffle() {
        if (shufflesLeft > 0) {
            shufflesLeft--;

            List<Integer> remainingPokemonIDs = new ArrayList<>();
            List<Point> occupiedSlots = new ArrayList<>();

            for (int i = 1; i <= TOTAL_ROWS; i++) {
                for (int j = 1; j <= TOTAL_COLS; j++) {
                    if (board[i][j] != 0) {
                        remainingPokemonIDs.add(board[i][j]);
                        occupiedSlots.add(new Point(j, i));
                    }
                }
            }

            Collections.shuffle(remainingPokemonIDs);

            for (int i = 0; i < occupiedSlots.size(); i++) {
                Point slot = occupiedSlots.get(i);
                int pokemonId = remainingPokemonIDs.get(i);
                board[slot.y][slot.x] = pokemonId;
            }

            gameBoardView.invalidate();
            updateUI();
        } else {
            Toast.makeText(this, "You ran out of shuffles!", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleWin() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        Toast.makeText(this, "You won level " + currentLevel + "!", Toast.LENGTH_LONG).show();
        currentLevel++;
        startNewGame();
    }

    private void updateUI() {
        tvLevel.setText(String.valueOf(currentLevel));
        tvScore.setText(String.valueOf(currentScore));
        tvShuffleCount.setText(String.valueOf(shufflesLeft));
        gameBoardView.invalidate();
    }

    private List<Point> findPath(Point p1, Point p2) {
        List<Point> path = new ArrayList<>();
        path.add(p1);

        if (checkLine(p1, p2)) {
            path.add(p2);
            return path;
        }

        Point corner = checkLPath(p1, p2);
        if (corner != null) {
            path.add(corner);
            path.add(p2);
            return path;
        }

        List<Point> uPath = checkUPath(p1, p2);
        if (uPath != null) {
            path.addAll(uPath);
            return path;
        }

        return null;
    }

    private boolean checkLine(Point p1, Point p2) {
        if (p1.y == p2.y) {
            int start = Math.min(p1.x, p2.x);
            int end = Math.max(p1.x, p2.x);
            for (int i = start + 1; i < end; i++) {
                if (board[p1.y][i] != 0) return false;
            }
            return true;
        }
        if (p1.x == p2.x) {
            int start = Math.min(p1.y, p2.y);
            int end = Math.max(p1.y, p2.y);
            for (int i = start + 1; i < end; i++) {
                if (board[i][p1.x] != 0) return false;
            }
            return true;
        }
        return false;
    }

    private Point checkLPath(Point p1, Point p2) {
        Point c1 = new Point(p1.x, p2.y);
        if (board[c1.y][c1.x] == 0 && checkLine(p1, c1) && checkLine(c1, p2)) {
            return c1;
        }
        Point c2 = new Point(p2.x, p1.y);
        if (board[c2.y][c2.x] == 0 && checkLine(p1, c2) && checkLine(c2, p2)) {
            return c2;
        }
        return null;
    }

    private List<Point> checkUPath(Point p1, Point p2) {
        for (int i = 0; i < board[0].length; i++) {
            Point testPoint = new Point(i, p1.y);
            if(board[testPoint.y][testPoint.x] == 0 || testPoint.equals(p2)) {
                if (checkLine(p1, testPoint)) {
                    Point corner = checkLPath(testPoint, p2);
                    if(corner != null) {
                        List<Point> path = new ArrayList<>();
                        path.add(testPoint);
                        path.add(corner);
                        path.add(p2);
                        return path;
                    }
                }
            }
        }
        for (int i = 0; i < board.length; i++) {
            Point testPoint = new Point(p1.x, i);
            if(board[testPoint.y][testPoint.x] == 0 || testPoint.equals(p2)) {
                if (checkLine(p1, testPoint)) {
                    Point corner = checkLPath(testPoint, p2);
                    if(corner != null) {
                        List<Point> path = new ArrayList<>();
                        path.add(testPoint);
                        path.add(corner);
                        path.add(p2);
                        return path;
                    }
                }
            }
        }
        return null;
    }

    private boolean isMoveAvailable() {
        List<Point> remainingTiles = new ArrayList<>();
        for (int i = 1; i <= TOTAL_ROWS; i++) {
            for (int j = 1; j <= TOTAL_COLS; j++) {
                if (board[i][j] != 0) {
                    remainingTiles.add(new Point(j, i));
                }
            }
        }

        for (int i = 0; i < remainingTiles.size(); i++) {
            for (int j = i + 1; j < remainingTiles.size(); j++) {
                Point p1 = remainingTiles.get(i);
                Point p2 = remainingTiles.get(j);
                if (board[p1.y][p1.x] == board[p2.y][p2.x]) {
                    if (findPath(p1, p2) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }

    private void shiftRowLeft(int row, int startCol, int endCol) {
        List<Integer> remaining = new ArrayList<>();
        for (int c = startCol; c <= endCol; c++) {
            if (board[row][c] != 0)
                remaining.add(board[row][c]);
        }
        int currentIndex = 0;
        for (int c = startCol; c <= endCol; c++) {
            board[row][c] = (currentIndex < remaining.size()) ? remaining.get(currentIndex++) : 0;
        }
    }

    private void shiftRowRight(int row, int startCol, int endCol) {
        List<Integer> remaining = new ArrayList<>();
        for (int c = startCol; c <= endCol; c++) {
            if (board[row][c] != 0)
                remaining.add(board[row][c]);
        }
        int currentIndex = remaining.size() - 1;
        for (int c = endCol; c >= startCol; c--) {
            board[row][c] = (currentIndex >= 0) ? remaining.get(currentIndex--) : 0;
        }
    }

    private void shiftColumnUp(int col, int startRow, int endRow) {
        List<Integer> remaining = new ArrayList<>();
        for (int r = startRow; r <= endRow; r++) {
            if (board[r][col] != 0)
                remaining.add(board[r][col]);
        }
        int currentIndex = 0;
        for (int r = startRow; r <= endRow; r++) {
            board[r][col] = (currentIndex < remaining.size()) ? remaining.get(currentIndex++) : 0;
        }
    }

    private void shiftColumnDown(int col, int startRow, int endRow) {
        List<Integer> remaining = new ArrayList<>();
        for (int r = startRow; r <= endRow; r++) {
            if (board[r][col] != 0)
                remaining.add(board[r][col]);
        }
        int currentIndex = remaining.size() - 1;
        for (int r = endRow; r >= startRow; r--) {
            board[r][col] = (currentIndex >= 0) ? remaining.get(currentIndex--) : 0;
        }
    }

    private void shiftAllRowsDown() {
        int[] lastRow = board[TOTAL_ROWS];
        for (int r = TOTAL_ROWS; r > 1; r--) {
            board[r] = board[r - 1];
        }
        board[1] = lastRow;
    }

    private void shiftAllRowsUp() {
        int[] firstRow = board[1];
        for (int r = 1; r < TOTAL_ROWS; r++) {
            board[r] = board[r + 1];
        }
        board[TOTAL_ROWS] = firstRow;
    }

    private void cascadeFill(int row, int col) {
        if (row > 1 && col > 1 && board[row - 1][col - 1] != 0) {
            board[row][col] = board[row - 1][col - 1];
            board[row - 1][col - 1] = 0;
            cascadeFill(row - 1, col - 1);
        } else if (row > 1 && board[row - 1][col] != 0) {
            shiftColumnUp(col, 1, row);
        }
    }

    private void cascadeFillFallLeft() {
        boolean tileMoved;
        do {
            tileMoved = false;
            for (int r = 1; r <= TOTAL_ROWS; r++) {
                for (int c = 1; c <= TOTAL_COLS; c++) {
                    if (board[r][c] == 0) {
                        if (r > 1 && c < TOTAL_COLS && board[r - 1][c + 1] != 0) {
                            board[r][c] = board[r - 1][c + 1];
                            board[r - 1][c + 1] = 0;
                            tileMoved = true;
                        }
                        else if (r > 1 && board[r - 1][c] != 0) {
                            board[r][c] = board[r - 1][c];
                            board[r - 1][c] = 0;
                            tileMoved = true;
                        }
                        else if (c < TOTAL_COLS && board[r][c + 1] != 0) {
                            board[r][c] = board[r][c+1];
                            board[r][c+1] = 0;
                            tileMoved = true;
                        }
                    }
                }
            }
        } while (tileMoved);
    }

    private void cascadeFillRiseRight() {
        boolean tileMoved;
        do {
            tileMoved = false;
            for (int r = TOTAL_ROWS; r >= 1; r--) {
                for (int c = TOTAL_COLS; c >= 1; c--) {
                    if (board[r][c] == 0) {
                        if (r < TOTAL_ROWS && c > 1 && board[r + 1][c - 1] != 0) {
                            board[r][c] = board[r + 1][c - 1];
                            board[r + 1][c - 1] = 0;
                            tileMoved = true;
                        }
                        else if (c > 1 && board[r][c - 1] != 0) {
                            board[r][c] = board[r][c - 1];
                            board[r][c - 1] = 0;
                            tileMoved = true;
                        }
                        else if (r < TOTAL_ROWS && board[r + 1][c] != 0) {
                            board[r][c] = board[r + 1][c];
                            board[r + 1][c] = 0;
                            tileMoved = true;
                        }
                    }
                }
            }
        } while (tileMoved);
    }

    private void cascadeFillRiseLeft() {
        boolean tileMoved;
        do {
            tileMoved = false;
            for (int r = TOTAL_ROWS; r >= 1; r--) {
                for (int c = 1; c <= TOTAL_COLS; c++) {
                    if (board[r][c] == 0) {
                        if (r < TOTAL_ROWS && c < TOTAL_COLS && board[r + 1][c + 1] != 0) {
                            board[r][c] = board[r + 1][c + 1];
                            board[r + 1][c + 1] = 0;
                            tileMoved = true;
                        }
                        else if (r < TOTAL_ROWS && board[r + 1][c] != 0) {
                            board[r][c] = board[r + 1][c];
                            board[r + 1][c] = 0;
                            tileMoved = true;
                        }
                        else if (c < TOTAL_COLS && board[r][c + 1] != 0) {
                            board[r][c] = board[r][c + 1];
                            board[r][c + 1] = 0;
                            tileMoved = true;
                        }
                    }
                }
            }
        } while (tileMoved);
    }

    private void shiftBoard(Point p1, Point p2) {
        if (currentLevel == 16) {
            if (p1.y > p2.y || (p1.y == p2.y && p1.x > p2.x)) {
                Point temp = p1;
                p1 = p2;
                p2 = temp;
            }
            cascadeFill(p1.y, p1.x);
            cascadeFill(p2.y, p2.x);
        } else if (currentLevel == 17) {
            cascadeFillFallLeft();
        } else if (currentLevel == 18) {
            cascadeFillRiseRight();
        } else if (currentLevel == 19) {
            cascadeFillRiseLeft();
        }
        else {
            processShiftForPoint(p1);
            processShiftForPoint(p2);
        }
    }

    private void processShiftForPoint(Point p) {
        final int horizontalMidpoint = 4;
        final int verticalMidpoint = 8;

        switch (currentLevel) {
            case 2:
                shiftColumnUp(p.x, 1, TOTAL_ROWS);
                break;
            case 3:
                shiftColumnDown(p.x, 1, TOTAL_ROWS);
                break;
            case 4:
                shiftRowLeft(p.y, 1, TOTAL_COLS);
                break;
            case 5:
                shiftRowRight(p.y, 1, TOTAL_COLS);
                break;
            case 6:
                if (p.x <= verticalMidpoint)
                    shiftRowLeft(p.y, 1, verticalMidpoint);
                else
                    shiftRowRight(p.y, verticalMidpoint + 1, TOTAL_COLS);
                break;
            case 7:
                if (p.x <= verticalMidpoint)
                    shiftRowRight(p.y, 1, verticalMidpoint);
                else
                    shiftRowLeft(p.y, verticalMidpoint + 1, TOTAL_COLS);
                break;
            case 8:
                if (p.y <= horizontalMidpoint)
                    shiftColumnUp(p.x, 1, horizontalMidpoint);
                else
                    shiftColumnDown(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                break;
            case 9:
                if (p.y <= horizontalMidpoint)
                    shiftColumnDown(p.x, 1, horizontalMidpoint);
                else
                    shiftColumnUp(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                break;
            case 10:
                if (p.x <= verticalMidpoint)
                    shiftRowLeft(p.y, 1, verticalMidpoint);
                break;
            case 11:
                if (p.x > verticalMidpoint)
                    shiftRowRight(p.y, verticalMidpoint + 1, TOTAL_COLS);
                break;
            case 12:
                if (p.y <= horizontalMidpoint) {
                    shiftColumnDown(p.x, 1, horizontalMidpoint);
                } else if (p.y > horizontalMidpoint + 1) {
                    shiftColumnUp(p.x, horizontalMidpoint + 2, TOTAL_ROWS);
                }
                break;
            case 13:
                if (p.y <= horizontalMidpoint) {
                    if (p.x <= verticalMidpoint) {
                        shiftColumnDown(p.x, 1, horizontalMidpoint);
                        shiftRowRight(p.y, 1, verticalMidpoint);
                    } else {
                        shiftColumnDown(p.x, 1, horizontalMidpoint);
                        shiftRowLeft(p.y, verticalMidpoint + 1, TOTAL_COLS);
                    }
                } else {
                    if (p.x <= verticalMidpoint) {
                        shiftColumnUp(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                        shiftRowRight(p.y, 1, verticalMidpoint);
                    } else {
                        shiftColumnUp(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                        shiftRowLeft(p.y, verticalMidpoint + 1, TOTAL_COLS);
                    }
                }
                break;

            default:
                break;
        }
    }


    private void restartGameFromBeginning() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        currentLevel = DEBUG_START_LEVEL;
        currentScore = 0;
        shufflesLeft = INITIAL_SHUFFLES;
        startNewGame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying() && !isMuted) {
            backgroundMusicPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
        }
    }
}
