package com.winlator.fusion.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.winlator.fusion.R;

public class WinetricksFloatingView extends LinearLayout {
    private SharedPreferences preferences;
    private final PointF startPoint = new PointF();
    private boolean isDragging = false;
    private short lastX, lastY;
    private boolean restoreSavedPosition = true;

    // UI references
    private EditText editVerb;
    private TextView textOutput;
    private Button btnExecuteWinetricks;
    private Button btnOpenWinetricksFolder;
    private Button btnTransparentToggle;
    private Button btnMinimize;

    // Callbacks
    private WinetricksListener listener;

    public WinetricksFloatingView(Context context) {
        this(context, null);
    }

    public WinetricksFloatingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WinetricksFloatingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutParams(
                new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        );
        setOrientation(HORIZONTAL);

        // Inflate the dialog layout
        View contentView = LayoutInflater.from(getContext())
                .inflate(R.layout.winetricks_content_dialog, this, false);

        // Find widgets inside the layout
        editVerb = contentView.findViewById(R.id.editWinetricksVerb);
        textOutput = contentView.findViewById(R.id.textWinetricksOutput);
        btnExecuteWinetricks = contentView.findViewById(R.id.btnExecuteWinetricks);
        btnOpenWinetricksFolder = contentView.findViewById(R.id.btnOpenWinetricksFolder);
        btnTransparentToggle = contentView.findViewById(R.id.btnTransparentToggle);
        btnMinimize = contentView.findViewById(R.id.btnHideWinetricks);
        Button btnRestartWineserver = contentView.findViewById(R.id.btnRestartWineserver);

        LinearLayout rightLayout = contentView.findViewById(R.id.rightLayout);

        // Setup dragging on the move handle
        View moveHandle = contentView.findViewById(R.id.btnMove);
        if (moveHandle != null) {
            moveHandle.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startPoint.set(event.getX(), event.getY());
                        isDragging = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isDragging) {
                            float newX = getX() + (event.getX() - startPoint.x);
                            float newY = getY() + (event.getY() - startPoint.y);
                            movePanel(newX, newY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        savePositionIfValid();
                        break;
                }
                return true;
            });
        }

        // Wire up button clicks
        btnExecuteWinetricks.setOnClickListener(v -> {
            if (listener != null) {
                String verb = editVerb.getText().toString().trim();
                listener.onWinetricksStableClick(verb, textOutput);
            }
        });

        btnOpenWinetricksFolder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenWinetricksFolder(textOutput);
            }
        });

        btnTransparentToggle.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleTransparency(WinetricksFloatingView.this);
            }
        });

        btnMinimize.setOnClickListener(v -> {
            setVisibility(View.GONE);
        });

        if (btnRestartWineserver != null) {
            btnRestartWineserver.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestartWineserverClick(textOutput);
                }
            });
        }

        // Load SharedPreferences for position storage
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Restore saved position
        if (restoreSavedPosition) {
            String savedPosition = preferences.getString("winetricks_view", null);
            if (savedPosition != null) {
                String[] parts = savedPosition.split("\\|");
                if (parts.length == 2) {
                    try {
                        float savedX = Float.parseFloat(parts[0]);
                        float savedY = Float.parseFloat(parts[1]);
                        setX(savedX);
                        setY(savedY);
                    } catch (NumberFormatException ignored) {}
                }
            }
            restoreSavedPosition = false;
        }

        // Add the inflated content
        addView(contentView);
    }

    private void movePanel(float x, float y) {
        View parent = (View) getParent();
        if (parent == null) return;

        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();

        int width = getWidth();
        int height = getHeight();

        final int margin = 16;

        if (x < margin) x = margin;
        if (y < margin) y = margin;
        if (x + width > parentWidth - margin) x = parentWidth - width - margin;
        if (y + height > parentHeight - margin) y = parentHeight - height - margin;

        setX(x);
        setY(y);

        lastX = (short) x;
        lastY = (short) y;
    }

    private void savePositionIfValid() {
        if (lastX > 0 && lastY > 0) {
            preferences.edit().putString("winetricks_view", lastX + "|" + lastY).apply();
        }
        lastX = 0;
        lastY = 0;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            View parent = (View) getParent();
            if (parent != null) {
                float x = getX();
                float y = getY();

                int parentWidth = parent.getWidth();
                int parentHeight = parent.getHeight();

                int width = getWidth();
                int height = getHeight();

                final int margin = 16;

                if (x + width > parentWidth - margin) x = parentWidth - width - margin;
                if (y + height > parentHeight - margin) y = parentHeight - height - margin;

                setX(Math.max(x, margin));
                setY(Math.max(y, margin));
            }
        }
    }

    public void setWinetricksListener(WinetricksListener listener) {
        this.listener = listener;
    }

    public EditText getEditVerb() {
        return editVerb;
    }

    public TextView getTextOutput() {
        return textOutput;
    }

    public interface WinetricksListener {
        void onWinetricksStableClick(String verb, TextView outputView);
        void onWinetricksLatestClick(String verb, TextView outputView);
        void onOpenWinetricksFolder(TextView outputView);
        void onToggleTransparency(View floatingView);
        void onRestartWineserverClick(TextView outputView);
    }
}
