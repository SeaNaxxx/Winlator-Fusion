package com.winlator.fusion.core;

import android.app.Activity;
import android.app.Dialog;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.winlator.fusion.R;
import com.winlator.fusion.math.Mathf;

public class PreloaderDialog {
    private final Activity activity;
    private Dialog dialog;
    private CountDownTimer countDownTimer;
    private boolean showProgress = false;

    public PreloaderDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.preloader_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        setProgressMode(false);
    }

    /**
     * Show the preloader with text only (no progress bar).
     */
    public synchronized void show(int textResId) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();
        ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);
        setProgressMode(false);
        dialog.show();
    }

    /**
     * Show the preloader with a countdown timer and horizontal progress bar.
     */
    public synchronized void showWithCountdown(int textResId, int countdownSeconds) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();
        ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);
        setProgressMode(true);
        dialog.show();
        startCountdown(countdownSeconds);
    }

    public void showOnUiThread(final int textResId) {
        activity.runOnUiThread(() -> show(textResId));
    }

    /**
     * Enable or disable progress mode (horizontal bar + percentage).
     */
    private void setProgressMode(boolean enabled) {
        this.showProgress = enabled;
        if (dialog == null) return;

        ProgressBar horizontalBar = dialog.findViewById(R.id.ProgressBar);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);
        TextView tvStatus = dialog.findViewById(R.id.TVStatus);

        if (enabled) {
            if (horizontalBar != null) {
                horizontalBar.setProgress(0);
                horizontalBar.setVisibility(View.VISIBLE);
            }
            if (tvProgress != null) {
                tvProgress.setText("0%");
                tvProgress.setVisibility(View.VISIBLE);
            }
            if (tvStatus != null) tvStatus.setVisibility(View.VISIBLE);
        } else {
            if (horizontalBar != null) horizontalBar.setVisibility(View.GONE);
            if (tvProgress != null) tvProgress.setVisibility(View.GONE);
            if (tvStatus != null) tvStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Set the progress (0-100). Only visible in progress mode.
     */
    public void setProgress(int progress) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);
        ProgressBar horizontalBar = dialog.findViewById(R.id.ProgressBar);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);

        if (horizontalBar != null && horizontalBar.getVisibility() == View.VISIBLE) {
            horizontalBar.setProgress(progress);
        }
        if (tvProgress != null && tvProgress.getVisibility() == View.VISIBLE) {
            tvProgress.setText(progress + "%");
        }
    }

    /**
     * Start a countdown timer that shows remaining time.
     */
    public void startCountdown(int countdownSeconds) {
        stopCountdown();
        final int totalMs = countdownSeconds * 1000;
        countDownTimer = new CountDownTimer(totalMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (dialog == null) { cancel(); return; }
                int elapsedSec = (int)((totalMs - millisUntilFinished) / 1000);
                int remainingSec = (int)(millisUntilFinished / 1000);
                int progress = (int)(((float)elapsedSec / countdownSeconds) * 100);
                setProgress(progress);

                TextView tvStatus = dialog.findViewById(R.id.TVStatus);
                if (tvStatus != null && tvStatus.getVisibility() == View.VISIBLE) {
                    String timeStr;
                    if (remainingSec > 60) {
                        timeStr = String.format("%dm %ds", remainingSec / 60, remainingSec % 60);
                    } else {
                        timeStr = remainingSec + "s";
                    }
                    tvStatus.setText(activity.getString(R.string.loading) + " " + timeStr);
                }
            }

            @Override
            public void onFinish() {
                setProgress(100);
            }
        }.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public synchronized void close() {
        stopCountdown();
        try {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        catch (Exception e) {}
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
