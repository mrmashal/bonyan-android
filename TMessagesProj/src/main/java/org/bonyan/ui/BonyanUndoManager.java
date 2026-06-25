/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Undo Manager for destructive actions with 10-second Snackbar
 *
 * This class provides a global undo mechanism for all destructive actions
 * in the Bonyan app, following the Telegram design guidelines.
 */

package org.bonyan.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Bonyan Undo Manager - Global undo mechanism for destructive actions.
 *
 * This class manages the 10-second Snackbar with progress bar that appears
 * after destructive actions, allowing users to undo before the action is committed.
 *
 * USAGE:
 * 1. Before performing a destructive action, call:
 *    BonyanUndoManager.getInstance(context).showUndo(undoRunnable);
 *
 * 2. In the Runnable, perform the actual destructive action.
 *    If the user clicks UNDO, this Runnable will NOT be executed.
 *
 * 3. The UndoManager will automatically hide after 10 seconds or when
 *    the user interacts with it.
 *
 * THREAD SAFETY:
 * This class is thread-safe. All operations are posted to the main thread.
 */
public class BonyanUndoManager {

    private static final String TAG = "BonyanUndoManager";

    // Singleton instance
    private static volatile BonyanUndoManager instance;
    private static final Object lock = new Object();

    // Constants
    private static final long UNDO_DURATION_MS = 10000; // 10 seconds
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 50; // Update progress every 50ms

    // UI Components
    private Context context;
    private Handler mainHandler;
    private FrameLayout undoContainer;
    private LinearLayout undoContent;
    private TextView undoText;
    private TextView undoButton;
    private ProgressBar progressBar;
    private View anchorView;

    // State
    private Runnable pendingUndoAction;
    private Runnable pendingCommitAction;
    private ValueAnimator progressAnimator;
    private boolean isShowing = false;
    private boolean isDismissing = false;

    /**
     * Private constructor for singleton pattern.
     */
    private BonyanUndoManager() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get the singleton instance.
     *
     * @param context The context (should be Application context)
     * @return The UndoManager instance
     */
    public static BonyanUndoManager getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new BonyanUndoManager();
                }
            }
        }
        if (instance.context == null && context != null) {
            instance.context = context.getApplicationContext();
        }
        return instance;
    }

    /**
     * Shows the undo Snackbar with a pending action.
     *
     * If an undo is already showing, it will be committed immediately before
     * showing the new one.
     *
     * @param actionToCommit The action to execute if user doesn't click UNDO
     */
    public void showUndo(Runnable actionToCommit) {
        mainHandler.post(() -> showUndoInternal(actionToCommit));
    }

    /**
     * Shows the undo Snackbar with both undo and commit actions.
     *
     * @param actionToUndo The action to execute if user clicks UNDO (revert)
     * @param actionToCommit The action to execute if user doesn't click UNDO
     */
    public void showUndo(Runnable actionToUndo, Runnable actionToCommit) {
        mainHandler.post(() -> {
            this.pendingUndoAction = actionToUndo;
            showUndoInternal(actionToCommit);
        });
    }

    /**
     * Internal method to show the undo UI.
     */
    private void showUndoInternal(Runnable actionToCommit) {
        // Commit any existing pending action first
        if (isShowing && pendingCommitAction != null) {
            commitAction();
        }

        dismissCurrent();

        this.pendingCommitAction = actionToCommit;
        this.isDismissing = false;
        this.isShowing = true;

        createUndoView();
        animateShow();
        startProgressAnimation();
    }

    /**
     * Dismisses the current undo Snackbar without committing.
     * Called when user clicks UNDO.
     */
    public void dismissWithoutCommit() {
        mainHandler.post(this::dismissWithoutCommitInternal);
    }

    private void dismissWithoutCommitInternal() {
        if (!isShowing || isDismissing) {
            return;
        }

        cancelProgressAnimation();

        // Execute undo action if provided
        if (pendingUndoAction != null) {
            try {
                pendingUndoAction.run();
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("Error executing undo action", e);
                }
            }
        }

        animateDismiss();
        resetState();
    }

    /**
     * Commits the pending action and dismisses the Snackbar.
     * Called when the timer expires.
     */
    private void commitAction() {
        if (pendingCommitAction != null) {
            try {
                pendingCommitAction.run();
            } catch (Exception e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("Error executing commit action", e);
                }
            }
        }
    }

    /**
     * Dismisses the current undo UI.
     */
    private void dismissCurrent() {
        if (!isShowing) {
            return;
        }

        cancelProgressAnimation();

        if (undoContainer != null && undoContainer.getParent() != null) {
            ((ViewGroup) undoContainer.getParent()).removeView(undoContainer);
        }

        resetState();
    }

    /**
     * Resets the internal state.
     */
    private void resetState() {
        isShowing = false;
        isDismissing = false;
        pendingCommitAction = null;
        pendingUndoAction = null;
        undoContainer = null;
        undoContent = null;
    }

    /**
     * Creates the undo view hierarchy.
     */
    private void createUndoView() {
        if (context == null) return;

        // Main container
        undoContainer = new FrameLayout(context);
        undoContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        undoContainer.setElevation(AndroidUtilities.dp(4));

        // Content layout
        undoContent = new LinearLayout(context);
        undoContent.setOrientation(LinearLayout.HORIZONTAL);
        undoContent.setGravity(Gravity.CENTER_VERTICAL);
        undoContent.setPadding(
            AndroidUtilities.dp(16),
            AndroidUtilities.dp(12),
            AndroidUtilities.dp(16),
            AndroidUtilities.dp(12)
        );

        // Message text
        undoText = new TextView(context);
        undoText.setText("Action performed");
        undoText.setTextSize(14);
        undoText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        undoText.setLayoutParams(new LinearLayout.LayoutParams(
            0, LayoutHelper.WRAP_CONTENT, 1.0f
        ));

        // Undo button
        undoButton = new TextView(context);
        undoButton.setText("UNDO");
        undoButton.setTextSize(14);
        undoButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        undoButton.setAllCaps(true);
        undoButton.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
        undoButton.setOnClickListener(v -> dismissWithoutCommit());

        // Progress bar
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setProgressDrawable(context.getResources().getDrawable(android.R.drawable.progress_horizontal));
        progressBar.setMax(100);
        progressBar.setProgress(100);

        // Assemble the view
        undoContent.addView(undoText);
        undoContent.addView(undoButton);

        undoContainer.addView(undoContent, LayoutHelper.createFrame(
            LayoutHelper.MATCH_PARENT,
            LayoutHelper.WRAP_CONTENT
        ));

        undoContainer.addView(progressBar, LayoutHelper.createFrame(
            LayoutHelper.MATCH_PARENT,
            AndroidUtilities.dp(2),
            Gravity.BOTTOM
        ));
    }

    /**
     * Animates the undo view showing.
     */
    private void animateShow() {
        if (undoContainer == null) return;

        // Find the parent to add the view
        if (anchorView != null && anchorView instanceof ViewGroup) {
            ((ViewGroup) anchorView).addView(undoContainer);
        }

        // Animate in
        undoContainer.setTranslationY(AndroidUtilities.dp(100));
        undoContainer.setAlpha(0f);
        undoContainer.animate()
            .translationY(0)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT)
            .start();
    }

    /**
     * Animates the undo view dismissing.
     */
    private void animateDismiss() {
        if (undoContainer == null) return;

        undoContainer.animate()
            .translationY(AndroidUtilities.dp(100))
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(CubicBezierInterpolator.EASE_IN)
            .withEndAction(() -> {
                if (undoContainer != null && undoContainer.getParent() != null) {
                    ((ViewGroup) undoContainer.getParent()).removeView(undoContainer);
                }
            })
            .start();
    }

    /**
     * Starts the progress bar animation.
     */
    private void startProgressAnimation() {
        if (progressBar == null) return;

        progressAnimator = ValueAnimator.ofInt(100, 0);
        progressAnimator.setDuration(UNDO_DURATION_MS);
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
        });
        progressAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (isShowing) {
                    // Time's up - commit the action
                    commitAction();
                    dismissCurrent();
                }
            }
        });
        progressAnimator.start();
    }

    /**
     * Cancels the progress bar animation.
     */
    private void cancelProgressAnimation() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
    }

    /**
     * Sets the anchor view for the undo Snackbar.
     *
     * @param anchor The view to anchor the Snackbar to
     */
    public void setAnchorView(View anchor) {
        this.anchorView = anchor;
    }
}
