package org.bonyan.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.bonyan.data.local.entity.Person;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.Cells.LetterSectionCell;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for the family/persons list.
 * Provides alphabetical sections and fast scrolling like Telegram's contacts.
 */
public class BonyanFamilyListAdapter extends RecyclerListView.SectionsAdapter {

    private final Context context;
    private List<Person> persons = new ArrayList<>();
    private List<Section> sections = new ArrayList<>();
    private final Map<String, String> relationMap = new HashMap<>();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_PERSON = 1;

    private static class Section {
        String letter;
        int startPosition;
        int count;

        Section(String letter, int startPosition, int count) {
            this.letter = letter;
            this.startPosition = startPosition;
            this.count = count;
        }
    }

    public interface OnPersonClickListener {
        void onPersonClick(Person person, int position);
    }

    public interface OnPersonLongClickListener {
        boolean onPersonLongClick(Person person, int position);
    }

    private OnPersonClickListener clickListener;
    private OnPersonLongClickListener longClickListener;

    public BonyanFamilyListAdapter(Context context) {
        this.context = context;
    }

    public void setOnPersonClickListener(OnPersonClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnPersonLongClickListener(OnPersonLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setData(List<Person> newPersons) {
        this.persons = newPersons != null ? newPersons : new ArrayList<>();
        buildSections();
        notifyDataSetChanged();
    }

    public void setRelationMap(Map<String, String> relations) {
        this.relationMap.clear();
        if (relations != null) {
            this.relationMap.putAll(relations);
        }
        notifyDataSetChanged();
    }

    private void buildSections() {
        sections.clear();
        if (persons.isEmpty()) return;

        char currentChar = 0;
        int sectionStart = 0;
        int count = 0;

        for (int i = 0; i < persons.size(); i++) {
            Person person = persons.get(i);
            String name = person.getName();
            if (name == null || name.isEmpty()) continue;

            char firstChar = Character.toUpperCase(name.charAt(0));
            if (firstChar != currentChar) {
                if (currentChar != 0) {
                    sections.add(new Section(String.valueOf(currentChar), sectionStart, count));
                }
                currentChar = firstChar;
                sectionStart = i;
                count = 1;
            } else {
                count++;
            }
        }

        if (currentChar != 0 && count > 0) {
            sections.add(new Section(String.valueOf(currentChar), sectionStart, count));
        }
    }

    @Override
    public int getSectionCount() {
        return sections.size();
    }

    @Override
    public int getCountForSection(int section) {
        if (section < 0 || section >= sections.size()) return 0;
        return sections.get(section).count;
    }

    @Override
    public View getSectionHeaderView(int sectionIndex, View view) {
        if (view == null) {
            view = new LetterSectionCell(context);
        }
        LetterSectionCell cell = (LetterSectionCell) view;
        if (sectionIndex >= 0 && sectionIndex < sections.size()) {
            cell.setLetter(sections.get(sectionIndex).letter);
        } else {
            cell.setLetter("");
        }
        return view;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            LetterSectionCell cell = new LetterSectionCell(context);
            return new RecyclerView.ViewHolder(cell) {};
        } else {
            BonyanPersonCell cell = new BonyanPersonCell(context);
            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerView.ViewHolder(cell) {};
        }
    }

    @Override
    public void onBindViewHolder(int section, int position, RecyclerListView.Holder holder) {
        BonyanPersonCell cell = (BonyanPersonCell) holder.itemView;
        int globalPosition = getGlobalPosition(section, position);
        if (globalPosition >= 0 && globalPosition < persons.size()) {
            Person person = persons.get(globalPosition);
            String relation = relationMap.get(person.getId());
            cell.setPerson(person);
            cell.setRelationType(relation);

            cell.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPersonClick(person, globalPosition);
                }
            });

            cell.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onPersonLongClick(person, globalPosition);
                }
                return false;
            });
        }
    }

    @Override
    public int getItemViewType(int section, int position) {
        return VIEW_TYPE_PERSON;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder, int section, int row) {
        return true;
    }

    @Override
    public Object getItem(int section, int position) {
        int globalPosition = 0;
        for (int i = 0; i < section && i < sections.size(); i++) {
            globalPosition += sections.get(i).count;
        }
        globalPosition += position;
        if (globalPosition >= 0 && globalPosition < persons.size()) {
            return persons.get(globalPosition);
        }
        return null;
    }

    private int getGlobalPosition(int sectionIndex, int positionInSection) {
        int globalPos = 0;
        for (int i = 0; i < sectionIndex && i < sections.size(); i++) {
            globalPos += sections.get(i).count;
        }
        return globalPos + positionInSection;
    }

    public Person getPersonAt(int position) {
        if (position >= 0 && position < persons.size()) {
            return persons.get(position);
        }
        return null;
    }

    public boolean isEmpty() {
        return persons.isEmpty();
    }

    public String getLetter(int sectionIndex) {
        if (sectionIndex >= 0 && sectionIndex < sections.size()) {
            return sections.get(sectionIndex).letter;
        }
        return null;
    }
}
