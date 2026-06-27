package org.bonyan.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Bottom sheet for filtering and sorting the family list.
 * Provides options for view mode, sort criteria, and relationship filters.
 */
public class BonyanFamilyFilterSheet extends BottomSheet {

    public interface OnFilterChangedListener {
        void onViewModeChanged(boolean isTreeView);
        void onSortCriteriaChanged(SortCriteria criteria);
        void onRelationshipFilterChanged(Map<String, Boolean> filters);
    }

    public enum SortCriteria {
        NAME,
        REPUTATION,
        BIRTH_YEAR,
        RECENTLY_ADDED
    }

    private OnFilterChangedListener listener;
    private boolean isTreeView = false;
    private SortCriteria currentSort = SortCriteria.NAME;
    private Map<String, Boolean> relationshipFilters = new HashMap<>();
    private final org.telegram.ui.ActionBar.Theme.ResourcesProvider resourceProvider;

    public BonyanFamilyFilterSheet(Context context, boolean needFocus, org.telegram.ui.ActionBar.Theme.ResourcesProvider resourceProvider) {
        super(context, needFocus, resourceProvider);
        this.resourceProvider = resourceProvider;
        init();
    }

    private void init() {
        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));

        // Title
        TextView titleView = new TextView(getContext());
        titleView.setText(LocaleController.getString("Bonyan_FilterAndSort", R.string.Bonyan_FilterAndSort));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(1, 20);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 16, 8, 16, 0));

        // View Mode Section
        HeaderCell viewModeHeader = new HeaderCell(getContext(), resourceProvider);
        viewModeHeader.setText(LocaleController.getString("Bonyan_ViewMode", R.string.Bonyan_ViewMode));
        container.addView(viewModeHeader);

        TextCheckCell listViewCell = new TextCheckCell(getContext(), resourceProvider);
        listViewCell.setTextAndCheck(LocaleController.getString("Bonyan_ListView", R.string.Bonyan_ListView), !isTreeView, false);
        listViewCell.setOnClickListener(v -> {
            isTreeView = false;
            updateViewModeCells(listViewCell, null);
            if (listener != null) {
                listener.onViewModeChanged(isTreeView);
            }
        });
        container.addView(listViewCell);

        TextCheckCell treeViewCell = new TextCheckCell(getContext(), resourceProvider);
        treeViewCell.setTextAndCheck(LocaleController.getString("Bonyan_TreeView", R.string.Bonyan_TreeView), isTreeView, false);
        treeViewCell.setOnClickListener(v -> {
            isTreeView = true;
            updateViewModeCells(listViewCell, treeViewCell);
            if (listener != null) {
                listener.onViewModeChanged(isTreeView);
            }
        });
        container.addView(treeViewCell);

        // Sort Section
        HeaderCell sortHeader = new HeaderCell(getContext(), resourceProvider);
        sortHeader.setText(LocaleController.getString("Bonyan_SortBy", R.string.Bonyan_SortBy));
        container.addView(sortHeader);

        for (SortCriteria criteria : SortCriteria.values()) {
            CheckBoxCell sortCell = new CheckBoxCell(getContext(), 1, false, resourceProvider);
            String label;
            switch (criteria) {
                case NAME:
                    label = LocaleController.getString("Bonyan_SortByName", R.string.Bonyan_SortByName);
                    break;
                case REPUTATION:
                    label = LocaleController.getString("Bonyan_SortByReputation", R.string.Bonyan_SortByReputation);
                    break;
                case BIRTH_YEAR:
                    label = LocaleController.getString("Bonyan_SortByBirthYear", R.string.Bonyan_SortByBirthYear);
                    break;
                case RECENTLY_ADDED:
                    label = LocaleController.getString("Bonyan_SortByRecentlyAdded", R.string.Bonyan_SortByRecentlyAdded);
                    break;
                default:
                    label = criteria.name();
            }
            sortCell.setText(label, "", currentSort == criteria, false);
            final SortCriteria finalCriteria = criteria;
            sortCell.setOnClickListener(v -> {
                currentSort = finalCriteria;
                if (listener != null) {
                    listener.onSortCriteriaChanged(currentSort);
                }
                dismiss();
            });
            container.addView(sortCell);
        }

        containerView = container;
    }

    private void updateViewModeCells(TextCheckCell listCell, TextCheckCell treeCell) {
        if (listCell != null) {
            listCell.setChecked(!isTreeView);
        }
        // Tree view cell would be updated similarly if needed
    }

    public void setOnFilterChangedListener(OnFilterChangedListener listener) {
        this.listener = listener;
    }

    public void setCurrentSort(SortCriteria sort) {
        this.currentSort = sort;
    }

    public void setTreeView(boolean treeView) {
        this.isTreeView = treeView;
    }
}
