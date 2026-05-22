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

public class DownloadProgressDialog {
    private final Activity activity;
    private Dialog dialog;
    private CountDownTimer countDownTimer;
    private long startTimeMs = 0;

    public DownloadProgressDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.download_progress_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public void show() {
        show(null);
    }

    public void show(int textResId) {
        show(textResId, null);
    }

    public void show(Runnable onCancelCallback) {
        show(0, onCancelCallback);
    }

    public void show(int textResId, final Runnable onCancelCallback) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();

        if (textResId > 0) ((TextView)dialog.findViewById(R.id.TextView)).setText(textResId);

        setProgress(0);
        startTimeMs = System.currentTimeMillis();

        dialog.findViewById(R.id.TVStatus).setVisibility(View.VISIBLE);

        if (onCancelCallback != null) {
            dialog.findViewById(R.id.BTCancel).setOnClickListener((v) -> onCancelCallback.run());
            dialog.findViewById(R.id.LLBottomBar).setVisibility(View.VISIBLE);
        }
        dialog.show();
    }

    public void setShowStatus(boolean show) {
        if (dialog != null) {
            dialog.findViewById(R.id.TVStatus).setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void setProgress(int progress) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);

        ProgressBar horizontalBar = dialog.findViewById(R.id.ProgressBar);
        TextView tvProgress = dialog.findViewById(R.id.TVProgress);

        horizontalBar.setProgress(progress);
        tvProgress.setText(progress + "%");

        updateStatusText(progress);
    }

    private void updateStatusText(int progress) {
        TextView tvStatus = dialog.findViewById(R.id.TVStatus);
        if (tvStatus.getVisibility() != View.VISIBLE) return;

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        if (progress > 0 && progress < 100) {
            long estimatedTotalMs = (elapsedMs * 100) / progress;
            long remainingMs = estimatedTotalMs - elapsedMs;
            int remainingSec = (int)(remainingMs / 1000);
            String timeStr;
            if (remainingSec > 60) {
                timeStr = String.format("%dm %ds", remainingSec / 60, remainingSec % 60);
            } else {
                timeStr = remainingSec + "s";
            }
            tvStatus.setText(activity.getString(R.string.loading) + " ~" + timeStr);
        } else if (progress >= 100) {
            tvStatus.setText(activity.getString(R.string.loading));
        }
    }

    /**
     * Start a countdown timer that shows remaining time in the status text.
     * @param countdownSeconds Total seconds for the countdown
     */
    public void startCountdown(int countdownSeconds) {
        stopCountdown();
        setShowStatus(true);
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
                if (tvStatus.getVisibility() == View.VISIBLE) {
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

    public void close() {
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
