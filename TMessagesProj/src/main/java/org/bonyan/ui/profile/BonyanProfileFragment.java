package org.bonyan.ui.profile;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bonyan.data.local.BonyanDatabase;
import org.bonyan.data.local.entity.Person;
import org.bonyan.ui.base.BonyanBaseFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Profile Fragment for the logged-in user.
 * Displays and allows editing of personal information.
 */
public class BonyanProfileFragment extends BonyanBaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int MENU_SETTINGS = 1;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private Person currentUser;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.userInfoDidLoad);
        loadUserData();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.userInfoDidLoad);
        executor.shutdown();
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Settings", org.telegram.messenger.R.string.Settings));

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_SETTINGS, org.telegram.messenger.R.drawable.msg_settings);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_SETTINGS) {
                    // TODO: Open filter/settings dialog
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        listView.setGlowColor(Theme.getColor(Theme.key_actionBarDefault));
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userInfoDidLoad) {
            loadUserData();
        }
    }

    private void loadUserData() {
        executor.execute(() -> {
            BonyanDatabase db = BonyanDatabase.getInstance(getParentActivity());
            Person user = db.personDao().getLoggedInUser();
            if (user == null) {
                // Create a default user for testing
                user = new Person();
                user.setId("user_" + System.currentTimeMillis());
                user.setName("کاربر نمونه");
                user.setGender("MALE");
                user.setBirthYear(1990);
                user.setPhone("09123456789");
                user.setLoggedInUser(true);
                db.personDao().insert(user);
            }
            currentUser = user;
            AndroidUtilities.runOnUIThread(() -> {
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void showEditDialog(String title, String currentValue, OnEditCompleteListener listener) {
        if (getParentActivity() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);

        final org.telegram.ui.Components.EditTextBoldCursor editText = new org.telegram.ui.Components.EditTextBoldCursor(getParentActivity());
        editText.setText(currentValue);
        editText.setTextSize(18);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));
        editText.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setSingleLine(true);

        builder.setView(editText);

        builder.setPositiveButton(LocaleController.getString("OK", org.telegram.messenger.R.string.OK), (dialog, which) -> {
            listener.onEditComplete(editText.getText().toString());
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", org.telegram.messenger.R.string.Cancel), null);

        builder.show().setOnShowListener(dialog -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
    }

    private interface OnEditCompleteListener {
        void onEditComplete(String newValue);
    }

    private void updatePerson(Person person) {
        person.setUpdatedAt(System.currentTimeMillis());
        executor.execute(() -> {
            BonyanDatabase db = BonyanDatabase.getInstance(getParentActivity());
            db.personDao().update(person);
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_TEXT_SETTING = 1;
        private static final int TYPE_SECTION_HEADER = 2;

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return 8; // Header + 1 section + name, gender, birth_year, phone, national_id, reputation
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_HEADER;
            if (position == 1) return TYPE_SECTION_HEADER;
            return TYPE_TEXT_SETTING;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_HEADER) {
                HeaderCell headerCell = new HeaderCell(mContext);
                headerCell.setText(LocaleController.getString("UserInfo", org.telegram.messenger.R.string.UserInfo));
                view = headerCell;
            } else if (viewType == TYPE_SECTION_HEADER) {
                HeaderCell headerCell = new HeaderCell(mContext);
                headerCell.setText("مشخصات فردی");
                view = headerCell;
            } else {
                view = new TextSettingsCell(mContext);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof TextSettingsCell) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (currentUser == null) return;

                switch (position) {
                    case 2:
                        cell.setTextAndValue("نام", currentUser.getName() != null ? currentUser.getName() : "-", true);
                        break;
                    case 3:
                        String genderText = "-";
                        if ("MALE".equals(currentUser.getGender())) genderText = "مرد";
                        else if ("FEMALE".equals(currentUser.getGender())) genderText = "زن";
                        cell.setTextAndValue("جنسیت", genderText, true);
                        break;
                    case 4:
                        cell.setTextAndValue("سال تولد", currentUser.getBirthYear() > 0 ? String.valueOf(currentUser.getBirthYear()) : "-", true);
                        break;
                    case 5:
                        cell.setTextAndValue("شماره تماس", currentUser.getPhone() != null ? currentUser.getPhone() : "-", true);
                        break;
                    case 6:
                        cell.setTextAndValue("کد ملی", currentUser.getNationalId() != null ? currentUser.getNationalId() : "-", true);
                        break;
                    case 7:
                        cell.setTextAndValue("منزلت (Reputation)", String.valueOf(currentUser.getReputationScore()), false);
                        break;
                }

                cell.setOnClickListener(v -> {
                    if (currentUser == null) return;
                    switch (position) {
                        case 2: // Name
                            showEditDialog("ویرایش نام", currentUser.getName(), newValue -> {
                                currentUser.setName(newValue);
                                updatePerson(currentUser);
                                notifyDataSetChanged();
                            });
                            break;
                        case 3: // Gender
                            showGenderPicker();
                            break;
                        case 4: // Birth Year
                            showEditDialog("ویرایش سال تولد", currentUser.getBirthYear() > 0 ? String.valueOf(currentUser.getBirthYear()) : "", newValue -> {
                                try {
                                    int year = Integer.parseInt(newValue);
                                    currentUser.setBirthYear(year);
                                    updatePerson(currentUser);
                                    notifyDataSetChanged();
                                } catch (NumberFormatException e) {
                                    // Invalid input, ignore
                                }
                            });
                            break;
                        case 5: // Phone
                            showEditDialog("ویرایش شماره تماس", currentUser.getPhone() != null ? currentUser.getPhone() : "", newValue -> {
                                currentUser.setPhone(newValue);
                                updatePerson(currentUser);
                                notifyDataSetChanged();
                            });
                            break;
                        case 6: // National ID
                            showEditDialog("ویرایش کد ملی", currentUser.getNationalId() != null ? currentUser.getNationalId() : "", newValue -> {
                                currentUser.setNationalId(newValue);
                                updatePerson(currentUser);
                                notifyDataSetChanged();
                            });
                            break;
                    }
                });
            }
        }

        private void showGenderPicker() {
            if (getParentActivity() == null) return;

            String[] options = {"مرد", "زن"};
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle("انتخاب جنسیت");
            builder.setItems(options, (dialog, which) -> {
                if (currentUser != null) {
                    currentUser.setGender(which == 0 ? "MALE" : "FEMALE");
                    updatePerson(currentUser);
                    notifyDataSetChanged();
                }
            });
            builder.show();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == TYPE_TEXT_SETTING;
        }
    }
}
