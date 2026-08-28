package com.example.app1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class GameBoardView extends View {

    private int[][] board;
    private Bitmap[] pokemonImages;
    private Point selectedTile;
    private List<Point> path;

    private Paint selectionPaint;
    private Paint linePaint;
    private float tileWidth;
    private float tileHeight;
    private Paint tileBackgroundPaint;
    private Paint borderPaint;

    private OnTileClickListener onTileClickListener;

    public interface OnTileClickListener {
        void onTileClick(int row, int col);
    }

    public void setOnTileClickListener(OnTileClickListener listener) {
        this.onTileClickListener = listener;
    }

    public GameBoardView(Context context) { super(context); init(); }
    public GameBoardView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public GameBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.RED);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(5);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#FFEB3B")); // Pikachu Yellow
        linePaint.setStrokeWidth(12);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setShadowLayer(20, 0, 0, Color.YELLOW); // Glow effect

        tileBackgroundPaint = new Paint();
        tileBackgroundPaint.setColor(Color.parseColor("#FFFBEB"));
        tileBackgroundPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#4CAF50"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        
        // Disable hardware acceleration for shadow layer to work on all versions
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setBoard(int[][] board) { 
        this.board = board; 
        updateDimensions();
        invalidate(); 
    }
    
    public void setPokemonImages(Bitmap[] images) { 
        this.pokemonImages = images; 
        invalidate(); 
    }
    
    public void setSelectedTile(Point tile) { this.selectedTile = tile; invalidate(); }
    public void drawPath(List<Point> path) { this.path = path; invalidate(); }

    public void clearPathAndSelection() {
        this.path = null;
        this.selectedTile = null;
        invalidate();
    }

    private void updateDimensions() {
        if (board != null && getWidth() > 0) {
            tileWidth = (float) getWidth() / (board[0].length - 2);
            tileHeight = (float) getHeight() / (board.length - 2);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDimensions();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (board == null || pokemonImages == null) return;
        
        if (tileWidth == 0) updateDimensions();

        for (int i = 1; i < board.length - 1; i++) {
            for (int j = 1; j < board[0].length - 1; j++) {
                int pokemonId = board[i][j];
                if (pokemonId != 0 && pokemonId < pokemonImages.length && pokemonImages[pokemonId] != null) {
                    float left = (j - 1) * tileWidth;
                    float top = (i - 1) * tileHeight;
                    RectF tileRect = new RectF(left + 2, top + 2, left + tileWidth - 2, top + tileHeight - 2);

                    canvas.drawRoundRect(tileRect, 8f, 8f, tileBackgroundPaint);
                    canvas.drawRoundRect(tileRect, 8f, 8f, borderPaint);

                    float padding = tileWidth * 0.1f;
                    RectF bitmapRect = new RectF(left + padding, top + padding, left + tileWidth - padding, top + tileHeight - padding);
                    canvas.drawBitmap(pokemonImages[pokemonId], null, bitmapRect, null);
                }
            }
        }

        if (selectedTile != null) {
            float left = (selectedTile.x - 1) * tileWidth;
            float top = (selectedTile.y - 1) * tileHeight;
            canvas.drawRect(left, top, left + tileWidth, top + tileHeight, selectionPaint);
        }

        if (path != null && path.size() > 1) {
            for (int i = 0; i < path.size() - 1; i++) {
                Point p1 = path.get(i);
                Point p2 = path.get(i + 1);
                float startX = (p1.x - 1) * tileWidth + tileWidth / 2;
                float startY = (p1.y - 1) * tileHeight + tileHeight / 2;
                float endX = (p2.x - 1) * tileWidth + tileWidth / 2;
                float endY = (p2.y - 1) * tileHeight + tileHeight / 2;
                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && onTileClickListener != null && board != null) {
            int col = (int) (event.getX() / tileWidth) + 1;
            int row = (int) (event.getY() / tileHeight) + 1;

            if (row >= 1 && row < board.length - 1 && col >= 1 && col < board[0].length - 1) {
                onTileClickListener.onTileClick(row, col);
            }
        }
        return true;
    }
}