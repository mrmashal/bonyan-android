package org.bonyan.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Bottom sheet for the Family FAB with options to add new member or link existing user.
 * Styled according to Telegram's bottom sheet design.
 */
public class BonyanFamilyFabSheet extends BottomSheet {

    public interface OnOptionSelectedListener {
        void onAddNewMember();
        void onLinkExistingUser();
    }

    private OnOptionSelectedListener listener;

    public BonyanFamilyFabSheet(Context context, boolean needFocus) {
        super(context, needFocus);
        init();
    }

    public BonyanFamilyFabSheet(Context context, boolean needFocus, int backgroundType) {
        super(context, needFocus, backgroundType, null);
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
        titleView.setText(LocaleController.getString("Bonyan_AddFamilyMember", R.string.Bonyan_AddFamilyMember));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(1, 20);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 16, 8, 16, 0));

        // Add New Member Option
        container.addView(createOptionRow(
            R.drawable.msg_addcontact,
            LocaleController.getString("Bonyan_AddNewMember", R.string.Bonyan_AddNewMember),
            LocaleController.getString("Bonyan_AddNewMemberDesc", R.string.Bonyan_AddNewMemberDesc),
            v -> {
                dismiss();
                if (listener != null) {
                    listener.onAddNewMember();
                }
            }
        ), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 8, 12, 0));

        // Link Existing User Option
        container.addView(createOptionRow(
            R.drawable.msg_link,
            LocaleController.getString("Bonyan_LinkExistingUser", R.string.Bonyan_LinkExistingUser),
            LocaleController.getString("Bonyan_LinkExistingUserDesc", R.string.Bonyan_LinkExistingUserDesc),
            v -> {
                dismiss();
                if (listener != null) {
                    listener.onLinkExistingUser();
                }
            }
        ), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 8, 12, 8));

        containerView = container;
    }

    private View createOptionRow(int iconRes, String title, String description, View.OnClickListener clickListener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        row.setOnClickListener(clickListener);

        // Icon
        ImageView iconView = new ImageView(getContext());
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.MULTIPLY));
        row.addView(iconView, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        // Text container
        LinearLayout textContainer = new LinearLayout(getContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);

        // Title
        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(1, 16);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Description
        if (description != null && !description.isEmpty()) {
            TextView descView = new TextView(getContext());
            descView.setText(description);
            descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            descView.setTextSize(1, 13);
            textContainer.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        row.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        return row;
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }
}
