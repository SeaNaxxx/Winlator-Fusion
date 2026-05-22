package com.winlator.fusion;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.winlator.fusion.bigpicture.BigPictureAdapter;
import com.winlator.fusion.bigpicture.CarouselItemDecoration;
import com.winlator.fusion.bigpicture.TiledBackgroundView;
import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.container.Shortcut;

import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class BigPictureActivity extends AppCompatActivity {
    private ImageView coverArtView;
    private TextView gameTitleView;
    private TextView playCountView;
    private TextView playtimeView;
    private RecyclerView recyclerView;
    private ContainerManager manager;
    private BigPictureAdapter adapter;
    private ImageButton playButton;

    private Shortcut currentShortcut;

    private TextView emptyStateTextView;

    @Override
    protected void onStart() {
        super.onStart();
        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        if (backgroundView != null) backgroundView.startAnimation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        if (backgroundView != null) backgroundView.stopAnimation();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.big_picture_activity);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Restore frame duration
        int storedFrameDuration = preferences.getInt("frame_duration", 66);
        if (backgroundView != null) {
            backgroundView.setFrameDuration(storedFrameDuration);
        }

        // Frame speed seek bar
        SeekBar frameSpeedSeekBar = findViewById(R.id.frameSpeedSeekBar);
        if (frameSpeedSeekBar != null && backgroundView != null) {
            int storedSeekBarProgress = preferences.getInt("frame_duration_seekbar", 33);
            frameSpeedSeekBar.setProgress(storedSeekBarProgress);
            int reversedProgress = frameSpeedSeekBar.getMax() - storedSeekBarProgress;
            backgroundView.setFrameDuration(reversedProgress);

            TiledBackgroundView finalBgView = backgroundView;
            SharedPreferences finalPrefs = preferences;
            frameSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int reversed = seekBar.getMax() - progress;
                    finalBgView.setFrameDuration(reversed);
                    finalPrefs.edit().putInt("frame_duration_seekbar", progress).apply();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }

        // Animation selector
        RadioGroup animationSelectorGroup = findViewById(R.id.animationSelectorGroup);
        if (animationSelectorGroup != null) {
            animationSelectorGroup.setOnCheckedChangeListener(null);

            String savedAnimation = preferences.getString("selected_animation", "ab");
            if (savedAnimation.equals("ab_gear")) {
                RadioButton rb = findViewById(R.id.rbGearAnimation);
                if (rb != null) rb.setChecked(true);
            } else if (savedAnimation.equals("ab_quilt")) {
                RadioButton rb = findViewById(R.id.rbQuiltAnimation);
                if (rb != null) rb.setChecked(true);
            } else if (savedAnimation.equals("none")) {
                RadioButton rb = findViewById(R.id.rbNoAnimation);
                if (rb != null) rb.setChecked(true);
            } else {
                RadioButton rb = findViewById(R.id.rbDefaultAnimation);
                if (rb != null) rb.setChecked(true);
            }

            animationSelectorGroup.setOnCheckedChangeListener((group, checkedId) -> {
                SharedPreferences.Editor editor = preferences.edit();
                if (backgroundView != null) {
                    if (checkedId == R.id.rbGearAnimation) {
                        backgroundView.setAnimation("ab_gear");
                        editor.putString("selected_animation", "ab_gear");
                    } else if (checkedId == R.id.rbQuiltAnimation) {
                        backgroundView.setAnimation("ab_quilt");
                        editor.putString("selected_animation", "ab_quilt");
                    } else if (checkedId == R.id.rbDefaultAnimation) {
                        backgroundView.setAnimation("ab");
                        editor.putString("selected_animation", "ab");
                    } else if (checkedId == R.id.rbNoAnimation) {
                        backgroundView.stopAnimation();
                        backgroundView.setVisibility(View.GONE);
                        editor.putString("selected_animation", "none");
                    }
                    backgroundView.startAnimation();
                }
                editor.apply();
            });

            // Apply saved animation
            if (backgroundView != null) {
                String savedAnim = preferences.getString("selected_animation", "ab");
                if (savedAnim.equals("ab_gear")) {
                    backgroundView.setAnimation("ab_gear");
                } else if (savedAnim.equals("ab_quilt")) {
                    backgroundView.setAnimation("ab_quilt");
                } else if (savedAnim.equals("none")) {
                    backgroundView.stopAnimation();
                    backgroundView.setVisibility(View.GONE);
                } else {
                    backgroundView.setAnimation("ab");
                }
                backgroundView.startAnimation();
            }
        }

        // Parallax mode
        RadioGroup parallaxModeGroup = findViewById(R.id.parallaxModeGroup);
        if (parallaxModeGroup != null && backgroundView != null) {
            String savedParallaxMode = preferences.getString("parallax_mode", "default");
            switch (savedParallaxMode) {
                case "off":
                    RadioButton rbOff = findViewById(R.id.rbParallaxOff);
                    if (rbOff != null) rbOff.setChecked(true);
                    break;
                case "slow":
                    RadioButton rbSlow = findViewById(R.id.rbParallaxSlow);
                    if (rbSlow != null) rbSlow.setChecked(true);
                    break;
                case "fast":
                    RadioButton rbFast = findViewById(R.id.rbParallaxFast);
                    if (rbFast != null) rbFast.setChecked(true);
                    break;
                default:
                    RadioButton rbDefault = findViewById(R.id.rbParallaxDefault);
                    if (rbDefault != null) rbDefault.setChecked(true);
                    break;
            }

            applyParallaxMode(savedParallaxMode);

            parallaxModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                String mode;
                if (checkedId == R.id.rbParallaxOff) {
                    mode = "off";
                } else if (checkedId == R.id.rbParallaxSlow) {
                    mode = "slow";
                } else if (checkedId == R.id.rbParallaxFast) {
                    mode = "fast";
                } else {
                    mode = "default";
                }
                preferences.edit().putString("parallax_mode", mode).apply();
                applyParallaxMode(mode);
            });
        }

        // Set immersive mode
        enableImmersiveMode();

        // Settings button
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        if (settingsButton != null) {
            Drawable settingsIcon = settingsButton.getDrawable();
            if (settingsIcon != null) {
                settingsIcon.mutate();
                settingsIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }
            settingsButton.setOnClickListener(v -> {
                if (findViewById(R.id.settingsLayout).getVisibility() == View.VISIBLE) {
                    hideSettingsView();
                } else {
                    showSettingsView();
                }
            });
        }

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            Drawable backIcon = backButton.getDrawable();
            if (backIcon != null) {
                backIcon.mutate();
                backIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }
            backButton.setOnClickListener(v -> {
                if (findViewById(R.id.settingsLayout).getVisibility() == View.VISIBLE) {
                    hideSettingsView();
                } else {
                    finish();
                }
            });
        }

        coverArtView = findViewById(R.id.IVCoverArt);
        gameTitleView = findViewById(R.id.TVGameTitle);
        playCountView = findViewById(R.id.TVPlayCount);
        playtimeView = findViewById(R.id.TVPlaytime);
        recyclerView = findViewById(R.id.RecyclerView);
        playButton = findViewById(R.id.playButton);

        // Tint play button
        if (playButton != null) {
            Drawable playIcon = playButton.getDrawable();
            if (playIcon != null) {
                playIcon.mutate();
                playIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }
        }

        // Add item decoration for reduced spacing
        recyclerView.addItemDecoration(new CarouselItemDecoration(15));

        // Initialize ContainerManager
        manager = new ContainerManager(this);

        // Load shortcuts
        loadShortcutsList();

        // Setup snapping for RecyclerView
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        // Play button click
        if (playButton != null) {
            playButton.setOnClickListener(v -> {
                if (currentShortcut != null) {
                    runFromShortcut(currentShortcut);
                }
            });
        }

        // RecyclerView scroll listener to update current shortcut
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int position = getCenterItemPosition();
                    if (position != RecyclerView.NO_POSITION && adapter != null) {
                        loadShortcutData(adapter.getItem(position));
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.settingsLayout).getVisibility() == View.VISIBLE) {
            hideSettingsView();
        } else {
            super.onBackPressed();
        }
    }

    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void applyParallaxMode(String mode) {
        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        if (backgroundView == null) return;
        switch (mode) {
            case "off":
                backgroundView.setParallax(false, 0, 0);
                break;
            case "slow":
                backgroundView.setParallax(true, 1.0f, 1.0f);
                break;
            case "fast":
                backgroundView.setParallax(true, 4.0f, 4.0f);
                break;
            default:
                backgroundView.setParallax(true, 2.0f, 2.0f);
                break;
        }
    }

    private int getCenterItemPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return RecyclerView.NO_POSITION;
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

        int centerPosition = RecyclerView.NO_POSITION;
        float closestToCenter = Float.MAX_VALUE;
        int recyclerViewCenter = recyclerView.getWidth() / 2;

        for (int i = firstVisibleItemPosition; i <= lastVisibleItemPosition; i++) {
            if (i >= 0) {
                View itemView = layoutManager.findViewByPosition(i);
                if (itemView != null) {
                    int itemCenter = (itemView.getLeft() + itemView.getRight()) / 2;
                    float distanceFromCenter = Math.abs(recyclerViewCenter - itemCenter);

                    if (distanceFromCenter < closestToCenter) {
                        closestToCenter = distanceFromCenter;
                        centerPosition = i;
                    }
                }
            }
        }

        return centerPosition;
    }

    private void loadShortcutsList() {
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts(null);
        emptyStateTextView = findViewById(R.id.TVEmptyState);

        if (shortcuts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            if (playButton != null) playButton.setVisibility(View.GONE);
            if (emptyStateTextView != null) emptyStateTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if (emptyStateTextView != null) emptyStateTextView.setVisibility(View.GONE);

            adapter = new BigPictureAdapter();
            adapter.setShortcuts(shortcuts);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerView.setAdapter(adapter);

            loadShortcutData(shortcuts.get(0));
        }
    }

    public void loadShortcutData(Shortcut shortcut) {
        currentShortcut = shortcut;

        if (gameTitleView != null) {
            gameTitleView.setText(shortcut.name);
        }

        if (coverArtView != null) {
            if (shortcut.icon != null) {
                coverArtView.setImageBitmap(shortcut.icon);
            }
        }

        // Play count
        if (playCountView != null) {
            String playCount = shortcut.getExtra("playCount", "0");
            playCountView.setText(getString(R.string.play_count, playCount));
        }

        // Playtime
        if (playtimeView != null) {
            long playtimeMillis = 0;
            try {
                playtimeMillis = Long.parseLong(shortcut.getExtra("playtime", "0"));
            } catch (NumberFormatException ignored) {}
            long playtimeMinutes = playtimeMillis / 60000;
            if (playtimeMinutes < 60) {
                playtimeView.setText(getString(R.string.playtime_min, playtimeMinutes));
            } else {
                long hours = playtimeMinutes / 60;
                long mins = playtimeMinutes % 60;
                playtimeView.setText(getString(R.string.playtime_hr_min, hours, mins));
            }
        }
    }

    private void runFromShortcut(Shortcut shortcut) {
        Intent intent = new Intent(this, XServerDisplayActivity.class);
        intent.putExtra("container_id", shortcut.container.id);
        intent.putExtra("shortcut_path", shortcut.file.getPath());
        startActivity(intent);
    }

    private void showSettingsView() {
        final LinearLayout mainLayout = findViewById(R.id.mainLayout);
        final LinearLayout settingsLayout = findViewById(R.id.settingsLayout);

        if (settingsLayout == null || mainLayout == null) return;

        settingsLayout.setVisibility(View.VISIBLE);

        settingsLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                settingsLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                ObjectAnimator mainSlideOut = ObjectAnimator.ofFloat(mainLayout, "translationX", 0f, -mainLayout.getWidth());
                mainSlideOut.setInterpolator(new AccelerateDecelerateInterpolator());
                mainSlideOut.setDuration(500);
                mainSlideOut.start();

                ObjectAnimator settingsSlideIn = ObjectAnimator.ofFloat(settingsLayout, "translationX", settingsLayout.getWidth(), 0f);
                settingsSlideIn.setInterpolator(new AccelerateDecelerateInterpolator());
                settingsSlideIn.setDuration(500);
                settingsSlideIn.start();
                return true;
            }
        });
    }

    private void hideSettingsView() {
        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        LinearLayout settingsLayout = findViewById(R.id.settingsLayout);

        if (mainLayout == null || settingsLayout == null) return;

        ObjectAnimator mainSlideIn = ObjectAnimator.ofFloat(mainLayout, "translationX", -mainLayout.getWidth(), 0f);
        mainSlideIn.setInterpolator(new AccelerateDecelerateInterpolator());
        mainSlideIn.setDuration(500);
        mainSlideIn.start();

        ObjectAnimator settingsSlideOut = ObjectAnimator.ofFloat(settingsLayout, "translationX", 0f, settingsLayout.getWidth());
        settingsSlideOut.setInterpolator(new AccelerateDecelerateInterpolator());
        settingsSlideOut.setDuration(500);
        settingsSlideOut.start();
        settingsSlideOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                settingsLayout.setVisibility(View.GONE);
            }
        });
    }
}
