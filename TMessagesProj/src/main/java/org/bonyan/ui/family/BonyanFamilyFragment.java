package org.bonyan.ui.family;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bonyan.data.local.BonyanDatabase;
import org.bonyan.data.local.entity.Person;
import org.bonyan.ui.base.BonyanBaseFragment;
import org.bonyan.ui.profile.BonyanPersonDetailFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Family/Contacts Fragment for displaying persons and managing family relations.
 */
public class BonyanFamilyFragment extends BonyanBaseFragment {

    private static final int MENU_SEARCH = 1;
    private static final int MENU_SETTINGS = 2;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private List<Person> personList = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isSearchMode = false;

    @Override
    public boolean onFragmentCreate() {
        loadPersons();
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
        actionBar.setTitle("خانواده");

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_SEARCH, org.telegram.messenger.R.drawable.msg_search);
        menu.addItem(MENU_SETTINGS, org.telegram.messenger.R.drawable.msg_settings);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (isSearchMode) {
                        toggleSearchMode(false);
                    } else {
                        finishFragment();
                    }
                } else if (id == MENU_SEARCH) {
                    toggleSearchMode(true);
                } else if (id == MENU_SETTINGS) {
                    // TODO: Open filter/settings dialog
                }
            }
        });

        fragmentView = new RecyclerListView(context);
        listView = (RecyclerListView) fragmentView;
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        listView.setGlowColor(Theme.getColor(Theme.key_actionBarDefault));
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < personList.size()) {
                Person person = personList.get(position);
                BonyanPersonDetailFragment fragment = new BonyanPersonDetailFragment(person);
                presentFragment(fragment);
            }
        });

        return fragmentView;
    }

    private void toggleSearchMode(boolean enable) {
        isSearchMode = enable;
        if (enable) {
            actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
            actionBar.setTitle(LocaleController.getString("Search", org.telegram.messenger.R.string.Search));
        } else {
            actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
            actionBar.setTitle("خانواده");
            loadPersons();
        }
    }

    private void loadPersons() {
        executor.execute(() -> {
            BonyanDatabase db = BonyanDatabase.getInstance(getParentActivity());
            List<Person> persons = db.personDao().getAllExceptLoggedIn();
            
            AndroidUtilities.runOnUIThread(() -> {
                personList.clear();
                personList.addAll(persons);
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void searchPersons(String query) {
        executor.execute(() -> {
            BonyanDatabase db = BonyanDatabase.getInstance(getParentActivity());
            List<Person> persons = db.personDao().searchByName(query);
            
            AndroidUtilities.runOnUIThread(() -> {
                personList.clear();
                personList.addAll(persons);
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return personList.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            BonyanPersonCell cell = new BonyanPersonCell(mContext);
            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof BonyanPersonCell) {
                BonyanPersonCell cell = (BonyanPersonCell) holder.itemView;
                Person person = personList.get(position);
                cell.setPerson(person);
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }
    }
}
