package org.bonyan.ui.family;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.bonyan.data.local.entity.Person;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

/**
 * Custom cell for displaying a person in the family/contacts list.
 * Mimics the look and feel of Telegram's SharedContactCell.
 */
public class BonyanPersonCell extends FrameLayout {

    private final BackupImageView avatarImageView;
    private final TextView nameTextView;
    private final TextView statusTextView;

    private Person currentPerson;
    private String currentRelationType;

    public BonyanPersonCell(Context context) {
        super(context);

        // Enable ripple effect
        setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        setWillNotDraw(false);

        // Avatar image
        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(23));
        addView(avatarImageView, LayoutHelper.createFrame(46, 46, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 12, 9, LocaleController.isRTL ? 12 : 0, 0));

        // Name text
        nameTextView = new TextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setTextSize(1, 16);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 68 : 72, 10, LocaleController.isRTL ? 72 : 68, 0));

        // Status/relation text
        statusTextView = new TextView(context);
        statusTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusTextView.setTextSize(1, 14);
        statusTextView.setLines(1);
        statusTextView.setMaxLines(1);
        statusTextView.setSingleLine(true);
        statusTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT));
        addView(statusTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 68 : 72, 32, LocaleController.isRTL ? 72 : 68, 0));
    }

    public void setPerson(Person person) {
        this.currentPerson = person;
        updateViews();
    }

    public void setRelationType(String relationType) {
        this.currentRelationType = relationType;
        updateViews();
    }

    private void updateViews() {
        if (currentPerson == null) return;

        nameTextView.setText(currentPerson.getName() != null ? currentPerson.getName() : "-");

        String status = "";
        if (currentRelationType != null) {
            status = formatRelationType(currentRelationType);
        } else if (currentPerson.getPhone() != null) {
            status = currentPerson.getPhone();
        }
        statusTextView.setText(status);

        // Set avatar placeholder
        avatarImageView.setImage(null, null, Theme.createCircleDrawable(AndroidUtilities.dp(46), Theme.getColor(Theme.key_avatar_backgroundSaved)), null, 0, null);
    }

    private String formatRelationType(String type) {
        switch (type) {
            case "PARENT": return "والد";
            case "CHILD": return "فرزند";
            case "SPOUSE": return "همسر";
            case "SIBLING": return "خواهر/برادر";
            default: return type;
        }
    }

    public Person getPerson() {
        return currentPerson;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Theme.dividerPaint != null) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(68), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(68) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
