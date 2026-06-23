/*
 * This file documents the minimal changes needed in LaunchActivity.java
 * to integrate the Bonyan Bridge Pattern.
 *
 * This is NOT a complete file - it shows the snippets that need to be
 * added to the existing LaunchActivity.java
 *
 * RULE: Keep total modifications under 50 lines.
 */

package org.telegram.ui;

// ... existing imports ...

// STEP 1: Add this import
import org.telegram.messenger.ApplicationLoader;

// ... existing class definition ...

public class LaunchActivity extends Activity implements ActionBarLayout.ActionBarLayoutDelegate, NotificationCenter.NotificationCenterDelegate, DialogsActivity.DialogsActivityDelegate,
        // STEP 2: Implement the BonyanFragmentContainer interface
        BonyanEntryPointImpl.BonyanFragmentContainer {

    // ... existing fields ...

    // STEP 3: Add these fields (under 5 lines)
    private IBonyanEntryPoint bonyanEntryPoint;
    private View bonyanBottomNav;
    private int currentBonyanTab = IBonyanEntryPoint.TAB_MISSIONS;

    // ... existing onCreate method ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ... existing onCreate code ...

        // STEP 4: Initialize Bonyan (add after ApplicationLoader.postInitApplication())
        // This is the ONLY Bonyan initialization code in LaunchActivity
        initializeBonyan();

        // ... rest of onCreate ...
    }

    // STEP 5: Add this method (under 20 lines)
    /**
     * Initializes Bonyan integration using the Bridge Pattern.
     * This method keeps Telegram core decoupled from Bonyan implementation.
     */
    private void initializeBonyan() {
        bonyanEntryPoint = ApplicationLoader.getBonyanEntryPoint();
        if (bonyanEntryPoint == null) {
            FileLog.w("Bonyan not initialized - entry point is null");
            return;
        }

        // Create and inject bottom navigation
        bonyanBottomNav = bonyanEntryPoint.getBottomNavigationView(this);
        if (bonyanBottomNav != null && drawerLayoutContainer != null) {
            // Add to drawer layout at bottom
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(56) // Standard bottom nav height
            );
            params.gravity = Gravity.BOTTOM;
            drawerLayoutContainer.addView(bonyanBottomNav, params);
        }
    }

    // STEP 6: Implement BonyanFragmentContainer interface method (3 lines)
    /**
     * Called when a Bonyan tab is selected.
     * Switches to the appropriate Bonyan fragment.
     */
    @Override
    public void onBonyanTabSelected(int tabId) {
        if (bonyanEntryPoint == null) return;

        currentBonyanTab = tabId;
        BaseFragment fragment = bonyanEntryPoint.getFragment(tabId);
        if (fragment != null) {
            // Use Telegram's existing fragment switching mechanism
            actionBarLayout.presentFragment(fragment, false, true, true);
        }
    }

    // ... rest of LaunchActivity ...
}
