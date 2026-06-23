/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Interface for activities that host Bonyan fragments
 */

package org.bonyan.ui;

/**
 * Interface for activities that host Bonyan fragments.
 * LaunchActivity should implement this interface.
 */
public interface BonyanFragmentContainer {
    /**
     * Called when a Bonyan tab is selected.
     *
     * @param tabId The selected tab ID
     */
    void onBonyanTabSelected(int tabId);
}
