# Forms Development Guide for Bonyan

This document defines the standards for developing multi-input forms in the Bonyan project, which is built on the Telegram Android client codebase.

## Table of Contents
1. [Core Principles](#core-principles)
2. [Architecture Overview](#architecture-overview)
3. [Form Structure](#form-structure)
4. [Input Field Types](#input-field-types)
5. [Validation](#validation)
6. [Data Submission](#data-submission)
7. [Error Handling](#error-handling)
8. [Undo Mechanism](#undo-mechanism)
9. [Example Form Implementation](#example-form-implementation)

---

## Core Principles

### 1. Use Telegram's UI Components Only
**NEVER** use standard Android components (`EditText`, `TextView`, `Button`, etc.) unless explicitly approved.

**Always use these Telegram components:**
- `TextSettingsCell` - For text input fields with label and value
- `HeaderCell` - For section headers
- `TextDetailSettingsCell` - For fields with description text
- `TextCheckCell` - For boolean/toggle fields
- `SlideChooseView` - For option selection
- `RecyclerListView` - For scrollable form container

### 2. Offline-First Architecture
- All form data is saved immediately to local SQLite database (Room)
- UI never blocks on network calls
- Background sync via `WorkManager` when online
- UI refreshes via `NotificationCenter` events

### 3. Bridge Pattern Compliance
- All form Fragments must extend `BonyanBaseFragment` (in `org.bonyan.ui`)
- Never import `org.bonyan.*` classes from `org.telegram.*` core
- Use `IBonyanEntryPoint` interface for communication

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (org.bonyan.ui)                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  BonyanFormFragment (extends BonyanBaseFragment)   │   │
│  │  ┌───────────────────────────────────────────────┐  │   │
│  │  │  RecyclerListView with Telegram Cells         │  │   │
│  │  └───────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Data Layer (org.bonyan.data)                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  BonyanRepository (Offline-first)                    │   │
│  │  ├── Save to Local DB (Room) ✓ immediate             │   │
│  │  ├── Post to NotificationCenter                     │   │
│  │  └── Queue for Sync (WorkManager)                   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Form Structure

### Base Form Fragment Template

```java
package org.bonyan.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Base class for Bonyan forms with multiple input fields.
 * Uses Telegram's native cells for consistent UI.
 */
public abstract class BonyanFormFragment extends BonyanBaseFragment {

    protected RecyclerListView listView;
    protected ListAdapter adapter;
    protected ArrayList<ListItem> items = new ArrayList<>();
    
    // Form state
    protected boolean hasChanges = false;
    protected boolean isSaving = false;
    
    // Undo support
    protected BonyanUndoManager undoManager;
    
    /**
     * Define the form structure. Override in subclasses.
     */
    protected abstract void buildForm();
    
    /**
     * Validate all fields. Return error message or null if valid.
     */
    protected abstract String validateForm();
    
    /**
     * Save form data. Called after validation passes.
     */
    protected abstract void saveForm();
    
    @Override
    public View createView(Context context) {
        // Setup action bar
        actionBar.setBackButtonImage(Theme.getResourceProvider().getDrawable("ic_ab_back"));
        actionBar.setTitle(getTitle());
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    onBackPressed();
                } else if (id == 1) {
                    onSaveClicked();
                }
            }
        });
        
        // Add save button
        actionBar.createMenu().addItem(1, Theme.getResourceProvider().getDrawable("ic_checkmark"));
        
        // Build form structure
        buildForm();
        
        // Create list view
        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> onItemClick(position));
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        
        fragmentView = contentView;
        return fragmentView;
    }
    
    protected void onItemClick(int position) {
        if (position < 0 || position >= items.size()) return;
        
        ListItem item = items.get(position);
        if (item.onClickListener != null) {
            item.onClickListener.onClick(item);
        }
    }
    
    protected void onSaveClicked() {
        // Validate
        String error = validateForm();
        if (error != null) {
            BulletinFactory.showError(getParentActivity(), error);
            return;
        }
        
        // Save
        isSaving = true;
        saveForm();
    }
    
    protected void onSaveSuccess() {
        isSaving = false;
        hasChanges = false;
        BulletinFactory.showSuccess(getParentActivity(), "Saved successfully");
        finishFragment();
    }
    
    protected void onSaveError(String error) {
        isSaving = false;
        BulletinFactory.showError(getParentActivity(), error);
    }
    
    // List item types
    protected static class ListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_TEXT = 1;
        public static final int TYPE_DETAIL = 2;
        public static final int TYPE_CHECK = 3;
        public static final int TYPE_SHADOW = 4;
        
        public int type;
        public String key;
        public String title;
        public String value;
        public String detail;
        public boolean checked;
        public boolean divider;
        public View.OnClickListener onClickListener;
        
        public static ListItem createHeader(String title) {
            ListItem item = new ListItem();
            item.type = TYPE_HEADER;
            item.title = title;
            return item;
        }
        
        public static ListItem createText(String key, String title, String value) {
            ListItem item = new ListItem();
            item.type = TYPE_TEXT;
            item.key = key;
            item.title = title;
            item.value = value;
            return item;
        }
        
        public static ListItem createDetail(String key, String title, String value, String detail) {
            ListItem item = new ListItem();
            item.type = TYPE_DETAIL;
            item.key = key;
            item.title = title;
            item.value = value;
            item.detail = detail;
            return item;
        }
        
        public static ListItem createCheck(String key, String title, boolean checked) {
            ListItem item = new ListItem();
            item.type = TYPE_CHECK;
            item.key = key;
            item.title = title;
            item.checked = checked;
            return item;
        }
    }
    
    // Adapter
    protected class ListAdapter extends RecyclerListView.SelectionAdapter {
        private Context context;
        
        public ListAdapter(Context context) {
            this.context = context;
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public int getItemViewType(int position) {
            return items.get(position).type;
        }
        
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case ListItem.TYPE_HEADER:
                    view = new HeaderCell(context);
                    break;
                case ListItem.TYPE_TEXT:
                    view = new TextSettingsCell(context);
                    break;
                case ListItem.TYPE_DETAIL:
                    view = new TextDetailSettingsCell(context);
                    break;
                case ListItem.TYPE_CHECK:
                    view = new TextCheckCell(context);
                    break;
                case ListItem.TYPE_SHADOW:
                default:
                    view = new ShadowSectionCell(context);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
        
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ListItem item = items.get(position);
            switch (item.type) {
                case ListItem.TYPE_HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(item.title);
                    break;
                case ListItem.TYPE_TEXT:
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextAndValue(item.title, item.value != null ? item.value : "", item.divider);
                    break;
                case ListItem.TYPE_DETAIL:
                    TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                    detailCell.setTextAndValue(item.title, item.value != null ? item.value : "", item.divider);
                    if (item.detail != null) {
                        detailCell.setValue(item.detail);
                    }
                    break;
                case ListItem.TYPE_CHECK:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    checkCell.setTextAndCheck(item.title, item.checked, item.divider);
                    break;
            }
        }
        
        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= items.size()) return false;
            int type = items.get(position).type;
            return type != ListItem.TYPE_HEADER && type != ListItem.TYPE_SHADOW;
        }
    }
}
```

---

## Input Field Types

### Text Input
Use `TextSettingsCell` for single-line text fields. For multi-line text, use a custom cell with `EditTextBoldCursor`.

### Selection/Dropdown
Use `SlideChooseView` for option selection within a row, or navigate to a separate selection fragment.

### Date/Time Picker
Use `BottomSheet` with `DatePicker` or `TimePicker` components from Telegram's UI library.

### Toggle/Checkbox
Use `TextCheckCell` for boolean options.

### File/Image Upload
Use a custom cell with avatar-style upload button. Store the file path locally, sync to server when online.

---

## Validation

### Validation Rules

1. **Immediate validation**: Validate on field blur or value change for simple rules (required, format)
2. **Deferred validation**: Validate complex rules on save attempt
3. **Display errors**: Use `BulletinFactory.showError()` for validation errors, not Toast

### Required Fields
Mark required fields with a visual indicator (red asterisk in label) and validate on save.

### Custom Validators
Create validator classes in `org.bonyan.util.validation`:

```java
public class Validator {
    public interface ValidationRule {
        String validate(String value);
    }
    
    public static ValidationRule required(String fieldName) {
        return value -> (value == null || value.trim().isEmpty()) 
            ? fieldName + " is required" : null;
    }
    
    public static ValidationRule minLength(int min) {
        return value -> (value != null && value.length() < min) 
            ? "Minimum " + min + " characters required" : null;
    }
    
    public static ValidationRule pattern(String regex, String message) {
        return value -> (value != null && !value.matches(regex)) 
            ? message : null;
    }
}
```

---

## Data Submission

### Submission Flow

1. **Local Save (Immediate)**
   - Save to Room database
   - Post to `NotificationCenter`
   - Show success via `BulletinFactory.showSuccess()`

2. **Background Sync (When Online)**
   - `WorkManager` processes `SyncQueue` table
   - Retry with exponential backoff
   - Update sync status in local DB

### Repository Pattern

All form submissions go through `BonyanRepository`:

```java
public class BonyanRepository {
    
    private final LocalDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final SyncQueueDao syncQueueDao;
    
    // Save locally and queue for sync
    public void saveMission(Mission mission, Callback<Mission> callback) {
        // 1. Save to local DB
        localDataSource.saveMission(mission, localResult -> {
            
            // 2. Add to sync queue
            syncQueueDao.insert(new SyncQueueItem(
                "Mission",
                mission.getId(),
                mission.getId() == null ? "INSERT" : "UPDATE",
                JsonUtils.toJson(mission)
            ));
            
            // 3. Notify UI
            NotificationCenter.getInstance().postNotificationName(
                NotificationCenter.missionSaved, mission.getId()
            );
            
            // 4. Trigger background sync
            BonyanSyncWorker.enqueueSyncWork();
            
            callback.onSuccess(mission);
        });
    }
}
```

### Form State Management

Track form state with a dedicated class:

```java
public class FormState {
    private Map<String, FieldState> fields = new HashMap<>();
    
    public void setValue(String key, String value) {
        FieldState state = fields.computeIfAbsent(key, k -> new FieldState());
        state.value = value;
        state.dirty = true;
        validateField(key);
    }
    
    public String getValue(String key) {
        FieldState state = fields.get(key);
        return state != null ? state.value : null;
    }
    
    public boolean hasErrors() {
        return fields.values().stream().anyMatch(s -> s.error != null);
    }
    
    public boolean hasChanges() {
        return fields.values().stream().anyMatch(s -> s.dirty);
    }
    
    private static class FieldState {
        String value;
        String error;
        boolean dirty;
    }
}
```

---

## Error Handling

### Display Modes

1. **Inline errors**: Show error state on the cell itself (red text, icon)
2. **Bulletin errors**: Use `BulletinFactory.showError()` for form-level or field-level errors
3. **Dialog errors**: Use `AlertDialog` for critical errors requiring user acknowledgment

### Error Categories

```java
public enum FormErrorType {
    VALIDATION_ERROR,    // User input issue
    NETWORK_ERROR,       // Connection issue (queued for retry)
    PERMISSION_ERROR,    // User lacks permission
    SERVER_ERROR,        // Backend issue
    CONFLICT_ERROR       // Data conflict (needs resolution)
}
```

### Error Recovery

- **Validation errors**: Highlight field, allow retry
- **Network errors**: Queue for background sync, notify user of pending status
- **Permission errors**: Redirect to appropriate settings/upgrade flow
- **Conflict errors**: Present diff/merge UI

---

## Undo Mechanism

### BonyanUndoManager

All destructive form actions must support undo:

```java
public class BonyanUndoManager {
    
    private static final long UNDO_DURATION_MS = 10000; // 10 seconds
    
    private Runnable currentUndoAction;
    private Runnable currentRedoAction;
    private Bulletin undoBulletin;
    
    public void performWithUndo(String actionDescription, 
                                   Runnable action,
                                   Runnable undoAction) {
        // Execute the action
        action.run();
        
        // Store undo
        this.currentUndoAction = undoAction;
        this.currentRedoAction = action;
        
        // Show undo bulletin
        showUndoBulletin(actionDescription);
    }
    
    private void showUndoBulletin(String description) {
        if (getParentActivity() == null) return;
        
        undoBulletin = BulletinFactory.createUndoBulletin(
            getParentActivity(),
            description,
            UNDO_DURATION_MS,
            () -> {
                // Undo clicked
                if (currentUndoAction != null) {
                    currentUndoAction.run();
                    currentUndoAction = null;
                }
            }
        );
        undoBulletin.show();
    }
}
```

### Form Operations Supporting Undo

- Delete field content: Restore previous value
- Clear form: Restore all previous values
- Delete item: Restore entire item
- Revert changes: Restore to last saved state

---

## Example Form Implementation

### Mission Creation Form

```java
package org.bonyan.ui;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;

import org.bonyan.data.model.Mission;
import org.bonyan.data.repository.BonyanRepository;
import org.bonyan.util.ValidationUtils;

import java.util.ArrayList;

/**
 * Form for creating a new mission.
 * Demonstrates proper form structure with validation and offline-first submission.
 */
public class MissionCreateFormFragment extends BonyanFormFragment {

    private FormState formState = new FormState();
    private BonyanRepository repository;
    
    // Field keys
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_START_DATE = "start_date";
    private static final String FIELD_END_DATE = "end_date";
    
    public MissionCreateFormFragment() {
        repository = BonyanRepository.getInstance();
    }
    
    @Override
    protected String getTitle() {
        return "New Mission";
    }
    
    @Override
    protected void buildForm() {
        items.clear();
        
        // Section: Basic Info
        items.add(ListItem.createHeader("Mission Details"));
        
        items.add(ListItem.createText(
            FIELD_TITLE,
            "Title *",
            formState.getValue(FIELD_TITLE)
        ));
        
        items.add(ListItem.createDetail(
            FIELD_DESCRIPTION,
            "Description",
            formState.getValue(FIELD_DESCRIPTION),
            "Briefly describe the mission"
        ));
        
        // Section: Priority
        items.add(ListItem.createHeader("Priority"));
        
        items.add(ListItem.createText(
            FIELD_PRIORITY,
            "Priority Level",
            formState.getValue(FIELD_PRIORITY) != null ? formState.getValue(FIELD_PRIORITY) : "Normal"
        ));
        
        // Section: Timeline
        items.add(ListItem.createHeader("Timeline"));
        
        items.add(ListItem.createText(
            FIELD_START_DATE,
            "Start Date",
            formState.getValue(FIELD_START_DATE)
        ));
        
        items.add(ListItem.createText(
            FIELD_END_DATE,
            "End Date",
            formState.getValue(FIELD_END_DATE)
        ));
        
        // Set click listeners
        for (ListItem item : items) {
            if (item.key != null) {
                item.onClickListener = v -> onFieldClick(item);
            }
        }
    }
    
    private void onFieldClick(ListItem item) {
        switch (item.key) {
            case FIELD_TITLE:
                showTextInputDialog("Mission Title", item.value, false, text -> {
                    formState.setValue(FIELD_TITLE, text);
                    buildForm();
                    adapter.notifyDataSetChanged();
                });
                break;
                
            case FIELD_DESCRIPTION:
                showTextInputDialog("Description", item.value, true, text -> {
                    formState.setValue(FIELD_DESCRIPTION, text);
                    buildForm();
                    adapter.notifyDataSetChanged();
                });
                break;
                
            case FIELD_PRIORITY:
                showPriorityPicker(item.value, priority -> {
                    formState.setValue(FIELD_PRIORITY, priority);
                    buildForm();
                    adapter.notifyDataSetChanged();
                });
                break;
                
            case FIELD_START_DATE:
            case FIELD_END_DATE:
                showDatePicker(item.value, date -> {
                    formState.setValue(item.key, date);
                    buildForm();
                    adapter.notifyDataSetChanged();
                });
                break;
        }
    }
    
    @Override
    protected String validateForm() {
        String title = formState.getValue(FIELD_TITLE);
        
        if (title == null || title.trim().isEmpty()) {
            return "Mission title is required";
        }
        
        if (title.length() < 3) {
            return "Title must be at least 3 characters";
        }
        
        if (title.length() > 100) {
            return "Title must be less than 100 characters";
        }
        
        // Validate dates
        String startDate = formState.getValue(FIELD_START_DATE);
        String endDate = formState.getValue(FIELD_END_DATE);
        
        if (startDate != null && endDate != null) {
            if (DateUtils.parseDate(startDate).after(DateUtils.parseDate(endDate))) {
                return "End date must be after start date";
            }
        }
        
        return null; // Validation passed
    }
    
    @Override
    protected void saveForm() {
        // Build mission object
        Mission mission = new Mission();
        mission.setTitle(formState.getValue(FIELD_TITLE));
        mission.setDescription(formState.getValue(FIELD_DESCRIPTION));
        mission.setPriority(formState.getValue(FIELD_PRIORITY));
        mission.setStartDate(formState.getValue(FIELD_START_DATE));
        mission.setEndDate(formState.getValue(FIELD_END_DATE));
        mission.setStatus("DRAFT");
        
        // Save through repository (offline-first)
        repository.saveMission(mission, new BonyanRepository.Callback<Mission>() {
            @Override
            public void onSuccess(Mission result) {
                AndroidUtilities.runOnUIThread(() -> {
                    // Notify other components
                    NotificationCenter.getInstance().postNotificationName(
                        NotificationCenter.missionCreated, result.getId()
                    );
                    onSaveSuccess();
                });
            }
            
            @Override
            public void onError(String error) {
                AndroidUtilities.runOnUIThread(() -> onSaveError(error));
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh form if needed
        if (hasChanges) {
            buildForm();
            adapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public boolean onBackPressed() {
        if (hasChanges) {
            showDiscardChangesDialog();
            return false;
        }
        return super.onBackPressed();
    }
    
    private void showDiscardChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Discard changes?");
        builder.setMessage("You have unsaved changes. Are you sure you want to discard them?");
        builder.setPositiveButton("Discard", (dialog, which) -> finishFragment());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
```

---

## Summary

This guide establishes the following patterns for forms in Bonyan:

1. **UI Components**: Always use Telegram's native cells (`TextSettingsCell`, `HeaderCell`, etc.)
2. **Architecture**: Extend `BonyanFormFragment` for all multi-input forms
3. **State Management**: Use `FormState` class to track field values and changes
4. **Validation**: Implement `validateForm()` with user-friendly error messages via `BulletinFactory`
5. **Submission**: Save to local DB immediately, background sync via `WorkManager`
6. **Undo**: Integrate with `BonyanUndoManager` for all destructive actions
7. **Navigation**: Handle back-press with discard-changes confirmation when needed

All forms must follow the **offline-first principle**: UI never blocks on network, all changes saved locally first, sync happens in background.
