package org.bonyan.ui.profile;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bonyan.data.local.BonyanDatabase;
import org.bonyan.data.local.entity.FamilyRelation;
import org.bonyan.data.local.entity.Person;
import org.bonyan.ui.base.BonyanBaseFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ToastBuilder;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Person Detail Fragment for viewing another person's profile.
 * Provides read-only view of profile and ability to claim family relationship.
 */
public class BonyanPersonDetailFragment extends BonyanBaseFragment {

    private final Person targetPerson;
    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private Person currentUser;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BonyanPersonDetailFragment(Person person) {
        super();
        this.targetPerson = person;
    }

    @Override
    public boolean onFragmentCreate() {
        loadCurrentUser();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        executor.shutdown();
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(targetPerson.getName() != null ? targetPerson.getName() : "نمایه");

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
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

        // Add claim relationship button at the bottom
        org.telegram.ui.Components.BottomButton bottomButton = new org.telegram.ui.Components.BottomButton(context);
        bottomButton.setText("ادعای ارتباط خانوادگی");
        bottomButton.setBackgroundColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        bottomButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        bottomButton.setOnClickListener(v -> claimRelationship());
        frameLayout.addView(bottomButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM));

        // Add padding to list to account for bottom button
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(48));

        return fragmentView;
    }

    private void loadCurrentUser() {
        executor.execute(() -> {
            BonyanDatabase db = BonyanDatabase.getInstance(getParentActivity());
            currentUser = db.personDao().getLoggedInUser();
        });
    }

    private void claimRelationship() {
        if (currentUser == null || targetPerson == null) {
            return;
        }

        // Show relation type picker
        String[] options = {"والد", "فرزند", "همسر", "خواهر/برادر"};
        String[] types = {"PARENT", "CHILD", "SPOUSE", "SIBLING"};

        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle("انتخاب نوع رابطه");
        builder.setItems(options, (dialog, which) -> {
            saveRelationship(types[which]);
        });
        builder.show();
    }

    private void saveRelationship(String relationType) {
        executor.execute(() -> {
            BonyanDatabase db = BonyanDatabase.getInstance(getParentActivity());

            FamilyRelation relation = new FamilyRelation();
            relation.setId(UUID.randomUUID().toString());
            relation.setPersonAId(currentUser.getId());
            relation.setPersonBId(targetPerson.getId());
            relation.setRelationType(relationType);
            relation.setSyncStatus(FamilyRelation.SYNC_STATUS_PENDING);
            relation.setRequestedBy(currentUser.getId());

            db.familyRelationDao().insert(relation);

            AndroidUtilities.runOnUIThread(() -> {
                new ToastBuilder()
                    .message("درخواست ارسال شد")
                    .duration(org.telegram.ui.Components.Toast.LENGTH_SHORT)
                    .show();
            });
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private static final int TYPE_SECTION_HEADER = 0;
        private static final int TYPE_TEXT_SETTING = 1;

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return 7; // 1 section header + name, gender, birth_year, phone, national_id, reputation
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_SECTION_HEADER;
            return TYPE_TEXT_SETTING;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_SECTION_HEADER) {
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
                if (targetPerson == null) return;

                switch (position) {
                    case 1:
                        cell.setTextAndValue("نام", targetPerson.getName() != null ? targetPerson.getName() : "-", true);
                        break;
                    case 2:
                        String genderText = "-";
                        if ("MALE".equals(targetPerson.getGender())) genderText = "مرد";
                        else if ("FEMALE".equals(targetPerson.getGender())) genderText = "زن";
                        cell.setTextAndValue("جنسیت", genderText, true);
                        break;
                    case 3:
                        cell.setTextAndValue("سال تولد", targetPerson.getBirthYear() > 0 ? String.valueOf(targetPerson.getBirthYear()) : "-", true);
                        break;
                    case 4:
                        cell.setTextAndValue("شماره تماس", targetPerson.getPhone() != null ? targetPerson.getPhone() : "-", true);
                        break;
                    case 5:
                        cell.setTextAndValue("کد ملی", targetPerson.getNationalId() != null ? targetPerson.getNationalId() : "-", true);
                        break;
                    case 6:
                        cell.setTextAndValue("منزلت (Reputation)", String.valueOf(targetPerson.getReputationScore()), false);
                        break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false; // Read-only in detail view
        }
    }
}
