package com.winlator.fusion;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

    private MediaPlayer mediaPlayer;
    private WebView musicWebView;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.big_picture_activity);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.preferences = preferences;

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

        // Initialize ContainerManager
        try {
            manager = new ContainerManager(this);
        } catch (Exception e) {
            manager = null;
        }

        // Add item decoration for reduced spacing
        if (recyclerView != null) {
            recyclerView.addItemDecoration(new CarouselItemDecoration(15));

            // Setup snapping for RecyclerView
            SnapHelper snapHelper = new LinearSnapHelper();
            snapHelper.attachToRecyclerView(recyclerView);
        }

        // Load shortcuts
        if (manager != null) loadShortcutsList();

        // Play button click
        if (playButton != null) {
            playButton.setOnClickListener(v -> {
                if (currentShortcut != null) {
                    runFromShortcut(currentShortcut);
                }
            });
        }

        // RecyclerView scroll listener to update current shortcut
        if (recyclerView != null) recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        try {
            setupMusicControls();
        } catch (Exception e) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        if (backgroundView != null) backgroundView.startAnimation();
        if (preferences != null && preferences.getBoolean("bg_music_enabled", false)) startBackgroundMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        TiledBackgroundView backgroundView = findViewById(R.id.parallaxBackgroundView);
        if (backgroundView != null) backgroundView.stopAnimation();
        stopBackgroundMusic();
    }

    @Override
    protected void onDestroy() {
        stopBackgroundMusic();
        if (musicWebView != null) musicWebView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1070 && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                preferences.edit().putString("selected_mp3_path", uri.toString()).apply();
                if (preferences.getBoolean("bg_music_enabled", false)) {
                    stopBackgroundMusic();
                    startBackgroundMusic();
                }
            }
        }
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
        if (recyclerView == null) return RecyclerView.NO_POSITION;
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

    private void setupMusicControls() {
        Switch bgMusicSwitch = findViewById(R.id.bgMusicSwitch);
        if (bgMusicSwitch == null) return;

        boolean bgMusicEnabled = preferences.getBoolean("bg_music_enabled", false);
        bgMusicSwitch.setChecked(bgMusicEnabled);
        bgMusicSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("bg_music_enabled", isChecked).apply();
            if (isChecked) startBackgroundMusic();
            else stopBackgroundMusic();
        });

        RadioGroup musicSourceGroup = findViewById(R.id.musicSourceGroup);
        if (musicSourceGroup != null) {
            String musicSource = preferences.getString("music_source", "mp3");
            if (musicSource.equals("youtube")) {
                RadioButton rb = findViewById(R.id.rbYouTube);
                if (rb != null) rb.setChecked(true);
            } else {
                RadioButton rb = findViewById(R.id.rbMP3);
                if (rb != null) rb.setChecked(true);
            }
            musicSourceGroup.setOnCheckedChangeListener((group, checkedId) -> {
                String source = (checkedId == R.id.rbYouTube) ? "youtube" : "mp3";
                preferences.edit().putString("music_source", source).apply();
                if (bgMusicSwitch.isChecked()) {
                    stopBackgroundMusic();
                    startBackgroundMusic();
                }
            });
        }

        EditText youtubeUrlInput = findViewById(R.id.youtubeUrlInput);
        Button loadYoutubeBtn = findViewById(R.id.loadYoutubeButton);
        if (youtubeUrlInput != null) {
            String savedUrl = preferences.getString("saved_youtube_url", "");
            youtubeUrlInput.setText(savedUrl);
        }
        if (loadYoutubeBtn != null) {
            loadYoutubeBtn.setOnClickListener(v -> {
                String url = youtubeUrlInput != null ? youtubeUrlInput.getText().toString() : "";
                String videoId = extractYouTubeId(url);
                if (!videoId.isEmpty() && musicWebView != null) {
                    preferences.edit().putString("saved_youtube_url", url).apply();
                    loadYouTubeVideo(videoId);
                }
            });
        }

        Button selectMp3Btn = findViewById(R.id.selectMp3Button);
        if (selectMp3Btn != null) {
            selectMp3Btn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select MP3"), 1070);
            });
        }

        Button resetMp3Btn = findViewById(R.id.resetMp3Button);
        if (resetMp3Btn != null) {
            resetMp3Btn.setOnClickListener(v -> {
                preferences.edit().remove("selected_mp3_path").apply();
                if (bgMusicSwitch.isChecked() && preferences.getString("music_source", "mp3").equals("mp3")) {
                    stopBackgroundMusic();
                    playDefaultMp3FromAssets();
                }
            });
        }

        musicWebView = findViewById(R.id.musicWebView);
        if (musicWebView != null) {
            WebSettings webSettings = musicWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            musicWebView.setVisibility(View.GONE);
        }

        if (bgMusicEnabled) startBackgroundMusic();
    }

    private void startBackgroundMusic() {
        String musicSource = preferences.getString("music_source", "mp3");
        if (musicSource.equals("youtube")) {
            String savedUrl = preferences.getString("saved_youtube_url", "yNwKYgM6SkM");
            String videoId = extractYouTubeId(savedUrl);
            if (videoId.isEmpty()) videoId = "yNwKYgM6SkM";
            loadYouTubeVideo(videoId);
        } else {
            String mp3Path = preferences.getString("selected_mp3_path", "");
            if (!mp3Path.isEmpty() && new File(mp3Path).exists()) {
                playMp3(new File(mp3Path));
            } else {
                playDefaultMp3FromAssets();
            }
        }
    }

    private void stopBackgroundMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (musicWebView != null) {
            musicWebView.loadUrl("about:blank");
        }
    }

    private void loadYouTubeVideo(String videoId) {
        if (musicWebView == null) return;
        String html = "<iframe width=\"1\" height=\"1\" src=\"https://www.youtube.com/embed/" + videoId + "?autoplay=1&loop=1&playlist=" + videoId + "\" frameborder=\"0\" allow=\"autoplay\" allowfullscreen></iframe>";
        musicWebView.loadData(html, "text/html", "utf-8");
        musicWebView.postDelayed(() -> simulateTouchOnWebView(), 1000);
    }

    private void simulateTouchOnWebView() {
        if (musicWebView == null) return;
        long downTime = System.currentTimeMillis();
        long eventTime = System.currentTimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 50, 50, 0);
        musicWebView.dispatchTouchEvent(event);
        event.recycle();
        event = MotionEvent.obtain(downTime, eventTime + 100, MotionEvent.ACTION_UP, 50, 50, 0);
        musicWebView.dispatchTouchEvent(event);
        event.recycle();
    }

    private String extractYouTubeId(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.matches("^[a-zA-Z0-9_-]{11}$")) return url;
        String pattern = "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(url);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    private void playDefaultMp3FromAssets() {
        try {
            File tempFile = new File(getCacheDir(), "default_music.mp3");
            if (!tempFile.exists()) {
                InputStream is = getAssets().open("default_music.mp3");
                FileOutputStream fos = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
                fos.close();
                is.close();
            }
            playMp3(tempFile);
        } catch (Exception e) {}
    }

    private void playMp3(File mp3File) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(mp3File.getPath());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {}
    }
}
