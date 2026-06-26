/*
 * This file is part of Bonyan - Comprehensive Family & Mission Tracking System
 * Planner/Calendar Fragment for scheduling and time management
 */

package org.bonyan.ui;
import org.telegram.ui.*;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.MainTabsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BonyanPlannerFragment extends BonyanBaseFragment implements MainTabsActivity.TabFragmentDelegate {

    // Task model
    public static class Task {
        public String id;
        public String title;
        public String description;
        public long dateMs;
        public int priority; // 0=none 1=low 2=med 3=high
        public boolean completed;
        public boolean hasReminder;

        public Task() { id = UUID.randomUUID().toString(); }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("title", title);
            o.put("description", description == null ? "" : description);
            o.put("dateMs", dateMs); o.put("priority", priority);
            o.put("completed", completed); o.put("hasReminder", hasReminder);
            return o;
        }

        public static Task fromJson(JSONObject o) throws JSONException {
            Task t = new Task();
            t.id = o.optString("id", UUID.randomUUID().toString());
            t.title = o.optString("title", "");
            t.description = o.optString("description", "");
            t.dateMs = o.optLong("dateMs", 0);
            t.priority = o.optInt("priority", 0);
            t.completed = o.optBoolean("completed", false);
            t.hasReminder = o.optBoolean("hasReminder", false);
            return t;
        }
    }

    // List items
    private static final int TYPE_DAY_HEADER  = 0;
    private static final int TYPE_TASK        = 1;
    private static final int TYPE_EMPTY_DAY   = 2;
    private static final int TYPE_OVERDUE_HDR = 3;

    private static class ListItem {
        int type; long dayMs; Task task;
        static ListItem dayHeader(long d, boolean ov) {
            ListItem i = new ListItem(); i.type = ov ? TYPE_OVERDUE_HDR : TYPE_DAY_HEADER; i.dayMs = d; return i; }
        static ListItem task(Task t) {
            ListItem i = new ListItem(); i.type = TYPE_TASK; i.task = t; return i; }
        static ListItem emptyDay(long d) {
            ListItem i = new ListItem(); i.type = TYPE_EMPTY_DAY; i.dayMs = d; return i; }
    }

    // State
    private final List<Task> tasks = new ArrayList<>();
    private final List<ListItem> listItems = new ArrayList<>();
    private long selectedDayMs;
    private boolean calendarExpanded = false;
    private boolean suppressCalendarSync = false;

    // Views
    private CalendarHeaderView calendarView;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private TaskListAdapter adapter;
    private FragmentFloatingButton floatingButton;
    private TaskFormSheet taskFormSheet;

    // Insets
    private boolean hasMainTabs;
    private int navigationBarHeight;
    private int additionNavigationBarHeight;
    private int additionFloatingButtonOffset;

    private static final String PREFS_NAME = "planner_tasks";
    private static final String PREFS_KEY  = "tasks_json";

    // Lifecycle
    public BonyanPlannerFragment() { super(); }
    public BonyanPlannerFragment(Bundle args) { super(); arguments = args; }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        selectedDayMs = todayMidnight();
        hasMainTabs = arguments != null && arguments.getBoolean("hasMainTabs", false);
        additionNavigationBarHeight = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS) : 0;
        additionFloatingButtonOffset = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT + DialogsActivity.MAIN_TABS_MARGIN) : 0;
        loadTasks(); rebuildList();
        return true;
    }

    @Override public void onFragmentDestroy() { super.onFragmentDestroy(); }

    @Override
    public void onResume() {
        super.onResume();
        updateFabVisibility(false);
        updateFabPosition();
    }

    // ── createView ────────────────────────────────────────────────────────────

    @Override
    public View createView(Context context) {
        actionBar.setTitle(getString(R.string.PlannerTitle));
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
        if (!hasMainTabs) actionBar.setBackButtonDrawable(new org.telegram.ui.ActionBar.BackDrawable(false));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        SizeNotifierFrameLayout root = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onMeasure(int wSpec, int hSpec) {
                measureChildWithMargins(actionBar, wSpec, 0, hSpec, 0);
                if (calendarView != null) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) calendarView.getLayoutParams();
                    if (lp != null) lp.topMargin = actionBar.getMeasuredHeight();
                }
                updateListPadding();
                super.onMeasure(wSpec, hSpec);
            }
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                updateFabPosition();
            }
        };
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        layoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(layoutManager);
        adapter = new TaskListAdapter(context);
        listView.setAdapter(adapter);
        listView.setClipToPadding(false);

        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setAddDuration(220); anim.setRemoveDuration(180); anim.setChangeDuration(160);
        listView.setItemAnimator(anim);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) { syncCalendarToList(); }
        });

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= listItems.size()) return;
            ListItem item = listItems.get(position);
            long dayMs = -1;
            if (item.type == TYPE_DAY_HEADER && item.dayMs > 0) dayMs = item.dayMs;
            else if (item.type == TYPE_TASK && item.task != null) dayMs = item.task.dateMs;
            if (dayMs > 0 && calendarView != null) {
                selectedDayMs = dayMs;
                calendarView.selectDay(dayMs);
            }
        });

        root.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        calendarView = new CalendarHeaderView(context);
        root.addView(calendarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        floatingButton = new FragmentFloatingButton(context, resourceProvider);
        floatingButton.setImageResource(R.drawable.ic_ab_new);
        floatingButton.setOnClickListener(v -> openTaskForm(null));
        root.addView(floatingButton, FragmentFloatingButton.createDefaultLayoutParams());

        calendarView.setOnDaySelectedListener(dayMs -> { selectedDayMs = dayMs; scrollListToDay(dayMs); });
        calendarView.setOnExpandToggleListener(expanded -> calendarExpanded = expanded);
        calendarView.setOnExpandFrameListener(this::updateListPadding);

        return fragmentView = root;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        navigationBarHeight = bottom;
        updateListPadding();
        updateFabPosition();
        updateFabVisibility(false);
    }

    private void updateListPadding() {
        if (listView == null || calendarView == null || actionBar == null) return;
        int topPad = actionBar.getMeasuredHeight() + calendarView.computeHeight();
        int botPad = navigationBarHeight + additionNavigationBarHeight;
        listView.setPadding(0, topPad, 0, botPad);
    }

    private void updateFabPosition() {
        if (floatingButton != null) {
            // Position FAB above the bottom navigation tabs like Telegram's chat-list
            int bottomOffset = navigationBarHeight + additionNavigationBarHeight;
            floatingButton.setTranslationY(-bottomOffset);
        }
    }

    private void updateFabVisibility(boolean animated) {
        if (floatingButton != null) floatingButton.setButtonVisible(true, animated);
    }

    @Override public boolean canParentTabsSlide(MotionEvent ev, boolean forward) { return true; }

    // Task form
    private void openTaskForm(Task editTask) {
        if (taskFormSheet != null) return;
        taskFormSheet = new TaskFormSheet(getParentActivity(), editTask, selectedDayMs, resourceProvider) {
            @Override protected void onTaskSaved(Task task, boolean isEdit) {
                taskFormSheet = null;
                if (isEdit) updateTask(task); else addTask(task);
            }
            @Override protected void onDismissed() { taskFormSheet = null; }
        };
        taskFormSheet.show();
    }

    // Data
    private void addTask(Task task) { tasks.add(task); saveTasks(); rebuildList(); adapter.notifyDataSetChanged(); scrollListToDay(task.dateMs); }
    private void updateTask(Task task) {
        for (int i = 0; i < tasks.size(); i++) if (tasks.get(i).id.equals(task.id)) { tasks.set(i, task); break; }
        saveTasks(); rebuildList(); adapter.notifyDataSetChanged();
    }
    private void deleteTask(Task task) { tasks.removeIf(t -> t.id.equals(task.id)); saveTasks(); rebuildList(); adapter.notifyDataSetChanged(); }
    private void toggleTaskComplete(Task task) {
        task.completed = !task.completed; saveTasks();
        int pos = findTaskPosition(task); if (pos >= 0) adapter.notifyItemChanged(pos);
    }

    // List building
    private void rebuildList() {
        listItems.clear();
        long today = todayMidnight();
        List<Task> overdue = new ArrayList<>();
        for (Task t : tasks) if (t.dateMs < today && !t.completed) overdue.add(t);
        if (!overdue.isEmpty()) { listItems.add(ListItem.dayHeader(0, true)); for (Task t : overdue) listItems.add(ListItem.task(t)); }
        for (int d = 0; d < 60; d++) {
            long dayMs = today + (long) d * 86400000L;
            List<Task> dt = new ArrayList<>();
            for (Task t : tasks) if (t.dateMs == dayMs) dt.add(t);
            listItems.add(ListItem.dayHeader(dayMs, false));
            if (dt.isEmpty()) listItems.add(ListItem.emptyDay(dayMs));
            else for (Task t : dt) listItems.add(ListItem.task(t));
        }
    }

    private int findDayHeaderPosition(long dayMs) {
        for (int i = 0; i < listItems.size(); i++) { ListItem it = listItems.get(i); if (it.type == TYPE_DAY_HEADER && it.dayMs == dayMs) return i; } return -1;
    }
    private int findTaskPosition(Task task) {
        for (int i = 0; i < listItems.size(); i++) { ListItem it = listItems.get(i); if (it.type == TYPE_TASK && it.task.id.equals(task.id)) return i; } return -1;
    }

    // Scroll sync
    private void scrollListToDay(long dayMs) {
        int pos = findDayHeaderPosition(dayMs);
        if (pos >= 0) {
            suppressCalendarSync = true;
            layoutManager.scrollToPositionWithOffset(pos, 0);
            AndroidUtilities.runOnUIThread(() -> suppressCalendarSync = false, 400);
        }
    }

    private void syncCalendarToList() {
        if (suppressCalendarSync || calendarView == null) return;
        int first = layoutManager.findFirstVisibleItemPosition();
        if (first < 0 || first >= listItems.size()) return;
        for (int i = first; i < Math.min(first + 3, listItems.size()); i++) {
            ListItem it = listItems.get(i);
            if (it.type == TYPE_DAY_HEADER && it.dayMs > 0) {
                if (calendarView.getSelectedDayMs() != it.dayMs) {
                    calendarView.setSelectedDay(it.dayMs, false);
                    selectedDayMs = it.dayMs;
                }
                break;
            }
        }
    }

    // Persistence
    private void loadTasks() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(PREFS_KEY, "[]");
        tasks.clear();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) tasks.add(Task.fromJson(arr.getJSONObject(i)));
        } catch (JSONException e) { FileLog.e(e); }
    }

    private void saveTasks() {
        try {
            JSONArray arr = new JSONArray();
            for (Task t : tasks) arr.put(t.toJson());
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREFS_KEY, arr.toString()).apply();
        } catch (JSONException e) { FileLog.e(e); }
    }

    // Helpers
    static long todayMidnight() { return dayMidnight(System.currentTimeMillis()); }
    static long dayMidnight(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    static long dayMidnight(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    static String formatShortDate(long dayMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        return sdf.format(new Date(dayMs));
    }
    private String formatDayHeader(long dayMs) {
        long today = todayMidnight();
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(dayMs);
        if (dayMs == today) return getString(R.string.PlannerToday);
        if (dayMs == today + 86400000L) return getString(R.string.PlannerTomorrow);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        return sdf.format(c.getTime());
    }

    // TaskListAdapter
    private class TaskListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;
        TaskListAdapter(Context ctx) { context = ctx; }
        @Override public int getItemCount() { return listItems.size(); }
        @Override public int getItemViewType(int position) { return listItems.get(position).type; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_DAY_HEADER || viewType == TYPE_OVERDUE_HDR) return new DayHeaderViewHolder(new DayHeaderView(context));
            if (viewType == TYPE_EMPTY_DAY) return new EmptyDayViewHolder(new EmptyDayView(context));
            return new TaskViewHolder(new TaskItemView(context));
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
            ListItem item = listItems.get(position);
            if (h instanceof DayHeaderViewHolder) ((DayHeaderViewHolder) h).bind(item.dayMs, item.type == TYPE_OVERDUE_HDR);
            else if (h instanceof EmptyDayViewHolder) ((EmptyDayViewHolder) h).bind(item.dayMs);
            else if (h instanceof TaskViewHolder) ((TaskViewHolder) h).bind(item.task);
        }
    }

    private static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        private final DayHeaderView view;
        DayHeaderViewHolder(DayHeaderView v) { super(v); view = v; }
        void bind(long dayMs, boolean overdue) { view.bind(dayMs, overdue); }
    }
    private static class EmptyDayViewHolder extends RecyclerView.ViewHolder {
        private final EmptyDayView view;
        EmptyDayViewHolder(EmptyDayView v) { super(v); view = v; }
        void bind(long dayMs) { view.bind(dayMs); }
    }
    private static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TaskItemView view;
        TaskViewHolder(TaskItemView v) { super(v); view = v; }
        void bind(Task task) { view.bind(task); }
    }

    // DayHeaderView
    private class DayHeaderView extends FrameLayout {
        private final TextView titleView, countView;
        private long boundDayMs;
        DayHeaderView(Context context) {
            super(context);
            setPadding(dp(12), dp(8), dp(12), dp(4));
            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            titleView.setTypeface(AndroidUtilities.bold());
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));
            countView = new TextView(context);
            countView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            countView.setTypeface(AndroidUtilities.bold());
            countView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            addView(countView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));
        }
        void bind(long dayMs, boolean overdue) {
            boundDayMs = dayMs;
            if (overdue) {
                titleView.setText(getString(R.string.PlannerOverdue));
                titleView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
            } else {
                titleView.setText(formatDayHeader(dayMs));
                titleView.setTextColor(dayMs == todayMidnight()
                        ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader)
                        : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            }
            int count = 0;
            for (Task t : tasks) {
                if (!overdue && t.dateMs == dayMs) count++;
                else if (overdue && t.dateMs < todayMidnight() && !t.completed) count++;
            }
            countView.setText(count > 0 ? String.valueOf(count) : "");
            countView.setVisibility(count > 0 ? VISIBLE : GONE);
        }
    }

    // TaskItemView
    private class TaskItemView extends FrameLayout {
        private final View card, priorityBar;
        private final CheckboxView checkbox;
        private final TextView titleView, descView;
        private final ImageView reminderIcon;
        private Task boundTask;

        TaskItemView(Context context) {
            super(context);
            setPadding(dp(12), dp(3), dp(12), dp(3));
            card = new View(context) {
                private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                private final RectF r = new RectF();
                @Override protected void onDraw(Canvas canvas) {
                    p.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    r.set(0, 0, getWidth(), getHeight());
                    canvas.drawRoundRect(r, dp(12), dp(12), p);
                }
            };
            card.setWillNotDraw(false);
            addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
            priorityBar = new View(context);
            addView(priorityBar, LayoutHelper.createFrame(4, LayoutHelper.MATCH_PARENT, Gravity.START | Gravity.FILL_VERTICAL, 12, 3, 0, 3));
            checkbox = new CheckboxView(context);
            addView(checkbox, LayoutHelper.createFrame(24, 24, Gravity.START | Gravity.CENTER_VERTICAL, 24, 0, 0, 0));
            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setMaxLines(1);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, 58, 10, 40, 0));
            descView = new TextView(context);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            descView.setMaxLines(1);
            descView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            addView(descView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, 58, 30, 40, 10));
            reminderIcon = new ImageView(context);
            reminderIcon.setImageResource(R.drawable.msg_mute);
            reminderIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), PorterDuff.Mode.SRC_IN));
            reminderIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            addView(reminderIcon, LayoutHelper.createFrame(16, 16, Gravity.END | Gravity.CENTER_VERTICAL, 0, 0, 14, 0));
            checkbox.setOnClickListener(v -> { if (boundTask != null) toggleTaskComplete(boundTask); });
            setOnLongClickListener(v -> { if (boundTask != null) showTaskOptions(boundTask, v); return true; });
        }

        void bind(Task task) {
            boundTask = task;
            titleView.setText(task.title);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            if (!TextUtils.isEmpty(task.description)) { descView.setText(task.description); descView.setVisibility(VISIBLE); } else descView.setVisibility(GONE);
            descView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            checkbox.setChecked(task.completed, false);
            float alpha = task.completed ? 0.45f : 1f;
            titleView.setAlpha(alpha); descView.setAlpha(alpha);
            int barColor;
            switch (task.priority) {
                case 3: barColor = 0xFFE53935; break;
                case 2: barColor = 0xFFFB8C00; break;
                case 1: barColor = 0xFF43A047; break;
                default: barColor = Color.TRANSPARENT; break;
            }
            priorityBar.setBackgroundColor(barColor);
            reminderIcon.setVisibility(task.hasReminder ? VISIBLE : GONE);
        }
    }

    // CheckboxView
    private static class CheckboxView extends View {
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint checkPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean checked;
        private float checkProgress;
        private ValueAnimator checkAnim;

        CheckboxView(Context context) {
            super(context);
            checkPaint.setStyle(Paint.Style.STROKE);
            checkPaint.setStrokeCap(Paint.Cap.ROUND);
            checkPaint.setStrokeJoin(Paint.Join.ROUND);
            checkPaint.setColor(Color.WHITE);
        }

        void setChecked(boolean checked, boolean animated) {
            if (this.checked == checked && !animated) return;
            this.checked = checked;
            if (checkAnim != null) checkAnim.cancel();
            if (animated) {
                checkAnim = ValueAnimator.ofFloat(checkProgress, checked ? 1f : 0f);
                checkAnim.setDuration(220);
                checkAnim.setInterpolator(new DecelerateInterpolator());
                checkAnim.addUpdateListener(a -> { checkProgress = (float) a.getAnimatedValue(); invalidate(); });
                checkAnim.start();
            } else { checkProgress = checked ? 1f : 0f; invalidate(); }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f - dp(1);
            rect.set(cx - r, cy - r, cx + r, cy + r);
            circlePaint.setStyle(Paint.Style.FILL);
            if (checkProgress > 0) {
                int base = Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton);
                int empty = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
                float inv = 1f - checkProgress;
                circlePaint.setColor(Color.rgb(
                        (int)(Color.red(empty)*inv + Color.red(base)*checkProgress),
                        (int)(Color.green(empty)*inv + Color.green(base)*checkProgress),
                        (int)(Color.blue(empty)*inv + Color.blue(base)*checkProgress)));
            } else circlePaint.setColor(Color.TRANSPARENT);
            canvas.drawOval(rect, circlePaint);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(dp(1.5f));
            circlePaint.setColor(checkProgress > 0 ? Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton) : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            canvas.drawOval(rect, circlePaint);
            if (checkProgress > 0) {
                checkPaint.setStrokeWidth(dp(2f));
                float p1x = cx - r*0.35f, p1y = cy, p2x = cx - r*0.05f, p2y = cy + r*0.35f, p3x = cx + r*0.4f, p3y = cy - r*0.3f;
                if (checkProgress < 0.5f) { float t = checkProgress*2f; canvas.drawLine(p1x, p1y, p1x+(p2x-p1x)*t, p1y+(p2y-p1y)*t, checkPaint); }
                else { float t = (checkProgress-0.5f)*2f; canvas.drawLine(p1x, p1y, p2x, p2y, checkPaint); canvas.drawLine(p2x, p2y, p2x+(p3x-p2x)*t, p2y+(p3y-p2y)*t, checkPaint); }
            }
        }
    }

    // EmptyDayView
    private static class EmptyDayView extends FrameLayout {
        EmptyDayView(Context context) {
            super(context);
            setPadding(dp(12), dp(2), dp(12), dp(2));
            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            tv.setText(getString(R.string.PlannerNoTasks));
            tv.setPadding(dp(16), dp(10), dp(16), dp(10));
            addView(tv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        void bind(long dayMs) {}
    }

    // Task context menu
    private void showTaskOptions(Task task, View anchor) {
        org.telegram.ui.Components.ItemOptions.makeOptions(BonyanPlannerFragment.this, anchor)
                .add(R.drawable.msg_edit, getString(R.string.Edit), () -> openTaskForm(task))
                .add(R.drawable.msg_delete, getString(R.string.Delete), true, () -> {
                    org.telegram.ui.ActionBar.AlertDialog.Builder b = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                    b.setTitle(getString(R.string.PlannerDeleteTask));
                    b.setMessage(getString(R.string.PlannerDeleteTaskConfirm));
                    b.setPositiveButton(getString(R.string.Delete), (d, w) -> deleteTask(task));
                    b.setNegativeButton(getString(R.string.Cancel), null);
                    showDialog(b.create());
                }).show();
    }

    // CalendarHeaderView
    interface OnDaySelectedListener  { void onDaySelected(long dayMs); }
    interface OnExpandToggleListener { void onToggle(boolean expanded); }
    interface OnExpandFrameListener  { void onFrame(); }

    private class CalendarHeaderView extends FrameLayout {

        private OnDaySelectedListener dayListener;
        private OnExpandToggleListener expandListener;
        private OnExpandFrameListener expandFrameListener;

        private long selectedDayMs;
        private long weekAnchorDayMs;

        static final int CELL_H_DP   = 44;
        static final int STRIP_H_DP  = 32;
        static final int HANDLE_H_DP = 24;
        static final int DOW_H_DP    = 32;
        static final int MONTH_YEAR_H_DP = 32;

        private float expandFraction = 0f;
        private ValueAnimator expandAnim;

        private float expandScrollY = 0f;
        private int baseYear;

        private float downX, downY, dragStartY, dragStartScrollY, dragStartFraction;
        private boolean isDragging, isHorizontalSwipe, hasMoved, dragOnHandle, handlePressed;
        private int touchSlop;
        private ValueAnimator handleRippleAnim;
        private float handleRippleAlpha = 0f;

        // Fling tracking
        private android.view.VelocityTracker velocityTracker;
        private android.widget.Scroller flingScroller;
        private static final int FLING_MIN_VELOCITY = 100; // Minimum velocity for fling (dp/s)
        private static final int FLING_MAX_VELOCITY = 8000; // Maximum fling velocity

        private final Paint bgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint selPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint todayRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint txtPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dimPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf        = new RectF();

        private final ImageView returnTodayBtn;
        private final TextView monthYearLabel;

        CalendarHeaderView(Context context) {
            super(context);
            setWillNotDraw(false);
            setClipChildren(false);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

            // Initialize fling utilities
            velocityTracker = android.view.VelocityTracker.obtain();
            flingScroller = new android.widget.Scroller(context, null, true);

            Calendar now = Calendar.getInstance();
            baseYear = now.get(Calendar.YEAR) - 2;
            selectedDayMs = BonyanPlannerFragment.todayMidnight();
            weekAnchorDayMs = selectedDayMs;
            expandScrollY = computeScrollYForDay(selectedDayMs);

            returnTodayBtn = new ImageView(context);
            returnTodayBtn.setImageResource(R.drawable.ic_ab_back);
            returnTodayBtn.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton), PorterDuff.Mode.SRC_IN));
            returnTodayBtn.setScaleType(ImageView.ScaleType.CENTER);
            returnTodayBtn.setRotation(LocaleController.isRTL ? 0f : 180f);
            returnTodayBtn.setVisibility(GONE);
            returnTodayBtn.setOnClickListener(v -> {
                long today = BonyanPlannerFragment.todayMidnight();
                setSelectedDay(today, true); setWeekAnchor(today);
                if (dayListener != null) dayListener.onDaySelected(today);
            });
            addView(returnTodayBtn, LayoutHelper.createFrame(32, STRIP_H_DP,
                    (LocaleController.isRTL ? Gravity.START : Gravity.END) | Gravity.TOP,
                    LocaleController.isRTL ? 8 : 0, 0, LocaleController.isRTL ? 0 : 8, 0));

            // Month-Year header label (visible in both modes)
            monthYearLabel = new TextView(context);
            monthYearLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            monthYearLabel.setTypeface(AndroidUtilities.bold());
            monthYearLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            monthYearLabel.setGravity(LocaleController.isRTL ? Gravity.START : Gravity.START);
            addView(monthYearLabel, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    (LocaleController.isRTL ? Gravity.START : Gravity.START) | Gravity.TOP,
                    LocaleController.isRTL ? dp(16) : dp(16), dp(4), 0, 0));

            updateCompactMonthLabel();
        }

        void setOnDaySelectedListener(OnDaySelectedListener l)  { dayListener = l; }
        void setOnExpandToggleListener(OnExpandToggleListener l) { expandListener = l; }
        void setOnExpandFrameListener(OnExpandFrameListener l)   { expandFrameListener = l; }
        float getExpandFraction() { return expandFraction; }
        long getSelectedDayMs() { return selectedDayMs; }

        void setWeekAnchor(long dayMs) {
            weekAnchorDayMs = dayMs;
            updateCompactMonthLabel();
            invalidate();
        }

        void setSelectedDay(long dayMs, boolean animated) {
            selectedDayMs = dayMs;
            weekAnchorDayMs = dayMs;
            updateCompactMonthLabel();
            if (expandFraction > 0.5f) {
                float targetY = computeScrollYForDay(dayMs);
                if (animated) animateScrollTo(targetY); else expandScrollY = targetY;
            }
            returnTodayBtn.setVisibility(dayMs == BonyanPlannerFragment.todayMidnight() ? GONE : VISIBLE);
            invalidate(); requestLayout();
        }

        void selectDay(long dayMs) { setSelectedDay(dayMs, true); }

        int computeHeight() {
            int headerH = dp(MONTH_YEAR_H_DP); // Month-year header visible in both modes
            int dowH = dp(DOW_H_DP); // Day-of-week header now always visible in both modes
            // Compact: header + DOW header + strip (for month label) + 1 cell row + handle
            int compactH = headerH + dowH + dp(STRIP_H_DP) + dp(CELL_H_DP) + dp(HANDLE_H_DP);
            // Expanded: header + DOW header + 6 cell rows + handle
            int expandedH = headerH + dowH + dp(CELL_H_DP) * 6 + dp(HANDLE_H_DP);
            return (int)(compactH + (expandedH - compactH) * expandFraction);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthSpec), computeHeight());
            int cs = MeasureSpec.makeMeasureSpec(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < getChildCount(); i++) getChildAt(i).measure(cs, cs);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int w = r - l;
            float ca = 1f - expandFraction;
            returnTodayBtn.setAlpha(ca);
            monthYearLabel.setAlpha(1f); // Month-year label always visible

            int stripH = dp(STRIP_H_DP);
            int headerH = dp(MONTH_YEAR_H_DP);
            int myW = monthYearLabel.getMeasuredWidth();

            if (LocaleController.isRTL) {
                returnTodayBtn.layout(dp(8), 0, dp(40), stripH);
                monthYearLabel.layout(w - dp(16) - myW, headerH / 2, w - dp(16), headerH / 2 + dp(16));
            } else {
                returnTodayBtn.layout(w - dp(40), 0, w - dp(8), stripH);
                monthYearLabel.layout(dp(16), headerH / 2, dp(16) + myW, headerH / 2 + dp(16));
            }
        }

        // Drawing
        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            int cellW = w / 7;
            int handleTop = h - dp(HANDLE_H_DP);
            int headerH = dp(MONTH_YEAR_H_DP);

            // Background (only up to handle, handle drawn separately)
            bgPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            canvas.drawRect(0, 0, w, handleTop, bgPaint);

            // Calendar content - unified rendering
            drawUnifiedCalendar(canvas, w, cellW, handleTop, headerH);

            // Draw handle bar on top
            drawHandleBar(canvas, w, h, handleTop);
        }

        private void drawHandleBar(Canvas canvas, int w, int h, int handleTop) {
            // Handle background
            bgPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            canvas.drawRect(0, handleTop, w, h, bgPaint);

            // Ripple effect when pressed
            if (handleRippleAlpha > 0) {
                bgPaint.setColor(Theme.getColor(Theme.key_listSelector));
                bgPaint.setAlpha((int)(handleRippleAlpha * 255));
                canvas.drawRect(0, handleTop, w, h, bgPaint);
                bgPaint.setAlpha(255);
            }

            // Top separator line
            dimPaint.setColor(Theme.getColor(Theme.key_divider));
            canvas.drawRect(0, handleTop, w, handleTop + dp(1), dimPaint);

            // Bottom separator line
            canvas.drawRect(0, h - dp(1), w, h, dimPaint);

            // Grabber pill
            int pillW = dp(36), pillH = dp(4);
            int pillCx = w / 2, pillCy = handleTop + dp(HANDLE_H_DP) / 2;
            rf.set(pillCx - pillW / 2f, pillCy - pillH / 2f,
                   pillCx + pillW / 2f, pillCy + pillH / 2f);
            dimPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            dimPaint.setAlpha(80);
            canvas.drawRoundRect(rf, dp(2), dp(2), dimPaint);
            dimPaint.setAlpha(255);
        }

        private void startHandleRipple() {
            if (handleRippleAnim != null) handleRippleAnim.cancel();
            handleRippleAnim = ValueAnimator.ofFloat(0.3f, 0f);
            handleRippleAnim.setDuration(200);
            handleRippleAnim.addUpdateListener(a -> {
                handleRippleAlpha = (float) a.getAnimatedValue();
                invalidate();
            });
            handleRippleAnim.start();
        }

        @Override
        public void computeScroll() {
            // Handle fling animation
            if (flingScroller.computeScrollOffset()) {
                expandScrollY = flingScroller.getCurrY();
                updateMonthYearLabelFromScroll();
                invalidate();

                // If fling animation finished, snap to nearest row
                if (flingScroller.isFinished()) {
                    snapToNearestRowWithFling(0);
                }
            }
        }

        private void drawUnifiedCalendar(Canvas canvas, int w, int cellW, int handleTop, int headerH) {
            // Determine visibility and positions based on expandFraction
            // In compact mode: show 1 week row at STRIP_H_DP + MONTH_YEAR_H_DP offset
            // In expanded mode: show 6 week rows + DOW header

            // Draw day-of-week header (visible in both modes)
            // Always show day-of-week header in both compact and expanded views
            float dowAlpha = 1.0f; // Always fully visible
            // Draw day-of-week labels
            String[] dowLabels = LocaleController.isRTL ?
                new String[]{"S","F","T","W","T","M","S"} : // RTL: Sun to Sat reversed
                new String[]{"S","M","T","W","T","F","S"};
            txtPaint.setTextSize(dp(11));
            txtPaint.setTypeface(AndroidUtilities.bold());
            txtPaint.setTextAlign(Paint.Align.CENTER);
            int dowColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
            int alpha = (int)(dowAlpha * 255);
            txtPaint.setColor(dowColor);
            txtPaint.setAlpha(alpha);
            int dowH = dp(DOW_H_DP);
            int dowY = headerH + dowH / 2 + dp(4);
            for (int col = 0; col < 7; col++) {
                int labelCol = LocaleController.isRTL ? 6 - col : col;
                int cx = col * cellW + cellW / 2;
                canvas.drawText(dowLabels[labelCol], cx, dowY, txtPaint);
            }
            txtPaint.setAlpha(255);

            // Now draw the calendar grid
            // We always draw the grid cells, but with different transforms based on mode
            if (expandFraction < 0.99f) {
                // Draw compact week view
                drawCompactWeek(canvas, w, cellW, headerH, expandFraction);
            }

            if (expandFraction > 0.01f) {
                // Draw expanded month view (with scroll)
                drawExpandedGrid(canvas, w, cellW, handleTop, headerH, expandFraction);
            }
        }

        private void drawCompactWeek(Canvas canvas, int w, int cellW, int headerH, float fraction) {
            int dowH = dp(DOW_H_DP); // Day-of-week header height (now always visible)
            int rowH = dp(CELL_H_DP);
            // Position the row below the header + DOW header
            int offsetY = headerH + dowH;

            // Calculate week anchor (find Sunday of the week containing weekAnchorDayMs)
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(weekAnchorDayMs);
            int dow = c.get(Calendar.DAY_OF_WEEK); // 1=Sun
            c.add(Calendar.DAY_OF_YEAR, -(dow - 1)); // go to Sunday

            for (int col = 0; col < 7; col++) {
                long dayMs = c.getTimeInMillis();
                boolean isSelected = dayMs == selectedDayMs;
                boolean isToday = dayMs == BonyanPlannerFragment.todayMidnight();

                // In compact mode, days before today are slightly dimmed
                boolean isPast = dayMs < BonyanPlannerFragment.todayMidnight();

                drawCellCompact(canvas, col, cellW, offsetY, rowH, dayMs, isSelected, isToday, isPast);
                c.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        private void drawCellCompact(Canvas canvas, int col, int cellW, int offsetY, int rowH,
                                     long dayMs, boolean isSelected, boolean isToday, boolean isPast) {
            int cx = col * cellW + cellW / 2;
            int cy = offsetY + rowH / 2;
            float r = dp(17);

            // Draw selection circle
            if (isSelected) {
                selPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
                rf.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawOval(rf, selPaint);
            } else if (isToday) {
                todayRing.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
                todayRing.setAlpha(35);
                rf.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawOval(rf, todayRing);
            }

            // Draw day number
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(dayMs);
            int dayNum = c.get(Calendar.DAY_OF_MONTH);

            txtPaint.setTextSize(dp(15));
            txtPaint.setTypeface(isSelected || isToday ? AndroidUtilities.bold() : android.graphics.Typeface.DEFAULT);
            txtPaint.setTextAlign(Paint.Align.CENTER);

            if (isSelected) {
                txtPaint.setColor(Color.WHITE);
            } else if (isToday) {
                txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
            } else if (isPast) {
                txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                txtPaint.setAlpha(130);
            } else {
                txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }

            canvas.drawText(String.valueOf(dayNum), cx, cy + dp(5), txtPaint);

            // Task dot
            for (Task t : tasks) {
                if (t.dateMs == dayMs) {
                    dotPaint.setColor(isSelected ? Color.WHITE
                            : Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
                    canvas.drawCircle(cx, cy + r - dp(2), dp(2.5f), dotPaint);
                    break;
                }
            }

            txtPaint.setAlpha(255); // Reset alpha
        }

        private void drawExpandedGrid(Canvas canvas, int w, int cellW, int handleTop, int headerH, float fraction) {
            int dowH = dp(DOW_H_DP);
            int gridTop = headerH + dowH;

            // Clip to ensure calendar doesn't draw over handle area
            canvas.save();
            canvas.clipRect(0, gridTop, w, handleTop);

            int gridH = handleTop - gridTop;
            float scrollY = expandScrollY;

            // Determine which row is first visible
            int firstRow = (int)(scrollY / dp(CELL_H_DP));
            float rowOffset = scrollY - firstRow * dp(CELL_H_DP);
            int visibleRows = (int)Math.ceil((gridH + rowOffset) / (float)dp(CELL_H_DP)) + 1;

            // Track which month is "current" for emphasis
            long topVisibleDay = rowToDay(firstRow);
            Calendar topCal = Calendar.getInstance();
            topCal.setTimeInMillis(topVisibleDay);
            int currentMonth = topCal.get(Calendar.MONTH);
            int currentYear = topCal.get(Calendar.YEAR);

            for (int ri = 0; ri < visibleRows; ri++) {
                int row = firstRow + ri;
                int rowY = gridTop + (int)(ri * dp(CELL_H_DP) - rowOffset);

                for (int col = 0; col < 7; col++) {
                    long dayMs = cellIndexToDay(row * 7 + col);
                    Calendar dc = Calendar.getInstance();
                    dc.setTimeInMillis(dayMs);
                    boolean isNextMonth = dc.get(Calendar.MONTH) != currentMonth
                            || dc.get(Calendar.YEAR) != currentYear;

                    // Call the unified cell drawing method
                    drawCellExpanded(canvas, col, cellW, rowY, dayMs, isNextMonth);

                    // Draw month label above the 1st of each month
                    if (dc.get(Calendar.DAY_OF_MONTH) == 1) {
                        SimpleDateFormat mf = new SimpleDateFormat("MMM", Locale.getDefault());
                        String mLabel = mf.format(dc.getTime());
                        txtPaint.setTextSize(dp(9));
                        txtPaint.setTypeface(AndroidUtilities.bold());
                        txtPaint.setTextAlign(Paint.Align.CENTER);
                        txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                        txtPaint.setAlpha(180);
                        int cx = col * cellW + cellW / 2;
                        canvas.drawText(mLabel, cx, rowY + dp(7), txtPaint);
                        txtPaint.setAlpha(255);
                    }
                }
            }

            canvas.restore();
        }

        private void drawCellExpanded(Canvas canvas, int col, int cellW, int rowY, long dayMs, boolean dimmed) {
            long today = BonyanPlannerFragment.todayMidnight();
            boolean isSel = dayMs == selectedDayMs;
            boolean isToday = dayMs == today;
            boolean isPast = dayMs < today;

            int cx = col * cellW + cellW / 2;
            int cy = rowY + dp(CELL_H_DP) / 2;
            float r = dp(17);

            // Selection indicator
            if (isSel) {
                selPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
                rf.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawOval(rf, selPaint);
            } else if (isToday) {
                todayRing.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
                todayRing.setAlpha(35);
                rf.set(cx - r, cy - r, cx + r, cy + r);
                canvas.drawOval(rf, todayRing);
            }

            // Day number
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(dayMs);
            int dayNum = c.get(Calendar.DAY_OF_MONTH);

            txtPaint.setTextSize(dp(15));
            txtPaint.setTypeface(isSel || isToday ? AndroidUtilities.bold() : android.graphics.Typeface.DEFAULT);
            txtPaint.setTextAlign(Paint.Align.CENTER);

            if (isSel) {
                txtPaint.setColor(Color.WHITE);
            } else if (isToday) {
                txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
            } else if (dimmed) {
                boolean isDark = Theme.isCurrentThemeDark();
                txtPaint.setColor(isDark ? 0xFFCCCCCC : 0xFF555555);
                txtPaint.setAlpha(isPast ? 100 : 160);
            } else if (isPast) {
                txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                txtPaint.setAlpha(130);
            } else {
                txtPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }

            canvas.drawText(String.valueOf(dayNum), cx, cy + dp(5), txtPaint);

            // Task dot
            for (Task t : tasks) {
                if (t.dateMs == dayMs) {
                    dotPaint.setColor(isSel ? Color.WHITE
                            : Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
                    canvas.drawCircle(cx, cy + r - dp(2), dp(2.5f), dotPaint);
                    break;
                }
            }

            txtPaint.setAlpha(255);
        }

        // Touch
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            float x = ev.getX(), y = ev.getY();
            int h = getHeight();
            int handleTop = h - dp(HANDLE_H_DP);

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = x; downY = y;
                    hasMoved = false; isHorizontalSwipe = false;
                    dragOnHandle = y >= handleTop;
                    handlePressed = dragOnHandle;
                    if (handlePressed) {
                        startHandleRipple();
                    }
                    // Start velocity tracking
                    if (velocityTracker != null) {
                        velocityTracker.clear();
                        velocityTracker.addMovement(ev);
                    }
                    // Allow dragging if:
                    // - in compact mode (expand/collapse)
                    // - OR dragging on handle (expand/collapse)
                    // - OR in expanded mode AND not on handle (for vertical scrolling)
                    boolean allowDrag = expandFraction < 0.99f || dragOnHandle ||
                            (expandFraction > 0.5f && !dragOnHandle);
                    if (allowDrag) {
                        isDragging = true;
                        dragStartY = y;
                        dragStartFraction = expandFraction;
                        dragStartScrollY = expandScrollY;
                        if (expandAnim != null) expandAnim.cancel();
                        // Cancel any ongoing fling
                        flingScroller.abortAnimation();
                    } else {
                        isDragging = false;
                    }
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    float dx = x - downX, dy = y - downY;
                    if (!hasMoved && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        hasMoved = true;
                        isHorizontalSwipe = Math.abs(dx) > Math.abs(dy);
                    }
                    if (isHorizontalSwipe) { isDragging = false; return false; }
                    if (!isDragging) return false;

                    // Track velocity for fling detection
                    if (velocityTracker != null) {
                        velocityTracker.addMovement(ev);
                    }

                    // In expanded mode (not on handle), allow vertical scrolling
                    boolean inExpandedScrollArea = expandFraction > 0.5f && !dragOnHandle && downY < handleTop;
                    if (inExpandedScrollArea) {
                        // Vertical scrolling in expanded mode
                        float newScroll = dragStartScrollY - (y - dragStartY);
                        expandScrollY = newScroll;
                        // Update month-year label while scrolling
                        updateMonthYearLabelFromScroll();
                        invalidate();
                    } else {
                        // Dragging to expand/collapse
                        int weekH  = dp(STRIP_H_DP) + dp(CELL_H_DP);
                        int monthH = dp(DOW_H_DP) + dp(CELL_H_DP) * 6;
                        float travel = monthH - weekH;
                        float delta = y - dragStartY;
                        float newFrac = Math.max(0f, Math.min(1f,
                                dragStartFraction + delta / travel));
                        expandFraction = newFrac;
                        requestLayout(); invalidate();
                        if (expandFrameListener != null) expandFrameListener.onFrame();
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Reset handle pressed state
                    if (handlePressed) {
                        handlePressed = false;
                        handleRippleAlpha = 0f;
                        invalidate();
                    }

                    // Check if we were in expanded scrolling mode
                    boolean wasScrollingExpanded = isDragging && expandFraction > 0.5f && !dragOnHandle && downY < handleTop;

                    // Handle tap on handle to toggle between compact and expanded
                    boolean tappedOnHandle = !hasMoved && ev.getAction() == MotionEvent.ACTION_UP &&
                                           dragOnHandle && !isDragging;

                    if (tappedOnHandle) {
                        // Toggle between compact and expanded
                        float target = expandFraction >= 0.5f ? 0f : 1f;
                        if (target < 0.5f) {
                            // Collapsing: bring selected week into view
                            weekAnchorDayMs = selectedDayMs;
                            updateCompactMonthLabel();
                        }
                        animateTo(target, 280);
                    } else if (isDragging && !(expandFraction > 0.99f && !dragOnHandle)) {
                        isDragging = false;
                        float target = expandFraction >= 0.5f ? 1f : 0f;
                        if (target < 0.5f) {
                            // Collapsing: bring selected week into view
                            weekAnchorDayMs = selectedDayMs;
                            updateCompactMonthLabel();
                        }
                        float rem = Math.abs(target - expandFraction);
                        animateTo(target, Math.max((int)(rem * 280), 80));
                    } else if (!hasMoved && ev.getAction() == MotionEvent.ACTION_UP && !tappedOnHandle) {
                        // Tap — select day (works in both compact and expanded modes)
                        int w = getWidth(), cellW = w / 7;
                        long tapped = getTappedDay(x, y, w, cellW);
                        if (tapped > 0) {
                            setSelectedDay(tapped, true);
                            if (dayListener != null) dayListener.onDaySelected(tapped);
                        }
                    }

                    // Trigger snap-to-grid scrolling if we were in expanded scrolling mode
                    if (wasScrollingExpanded && expandFraction > 0.95f) {
                        // Calculate fling velocity for snap-to-grid
                        if (velocityTracker != null) {
                            velocityTracker.computeCurrentVelocity(1000);
                            float velocityY = velocityTracker.getYVelocity();
                            snapToNearestRowWithFling(velocityY);
                        } else {
                            snapToNearestRowWithFling(0);
                        }
                    }

                    isDragging = false;
                    return true;
            }
            return super.onTouchEvent(ev);
        }

        private long getTappedDay(float x, float y, int w, int cellW) {
            int col = (int)(x / cellW);
            if (col < 0 || col > 6) return -1;

            int headerH = dp(MONTH_YEAR_H_DP);
            int handleTop = getHeight() - dp(HANDLE_H_DP);

            // Check if tap is in handle area
            if (y >= handleTop) {
                return -1; // Tap on handle - handled separately
            }

            if (expandFraction < 0.5f) {
                // Compact: tap on the week row
                int stripH = dp(STRIP_H_DP);
                int rowTop = headerH + stripH;
                int rowBot = rowTop + dp(CELL_H_DP);
                if (y < rowTop || y > rowBot) return -1;
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(weekAnchorDayMs);
                int dow = c.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
                c.add(Calendar.DAY_OF_YEAR, -dow + col);
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
                return c.getTimeInMillis();
            } else {
                // Expanded: tap on the grid
                int dowH = dp(DOW_H_DP);
                int gridTop = headerH + dowH;
                if (y < gridTop) return -1;
                int row = (int)((y - gridTop + expandScrollY) / dp(CELL_H_DP));
                return cellIndexToDay(row * 7 + col);
            }
        }

        // Helpers

        /** Convert an absolute cell index (0 = Sun of week containing Jan 1 of baseYear) to a day. */
        private long cellIndexToDay(int cellIndex) {
            Calendar c = Calendar.getInstance();
            c.set(baseYear, Calendar.JANUARY, 1, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            // Go to Sunday of that week
            int dow = c.get(Calendar.DAY_OF_WEEK) - 1;
            c.add(Calendar.DAY_OF_YEAR, -dow);
            c.add(Calendar.DAY_OF_YEAR, cellIndex);
            return c.getTimeInMillis();
        }

        /** Convert a row index to the Sunday of that row. */
        private long rowToDay(int row) {
            return cellIndexToDay(row * 7);
        }

        /** Compute the expandScrollY that puts the week containing dayMs at the top. */
        private float computeScrollYForDay(long dayMs) {
            Calendar origin = Calendar.getInstance();
            origin.set(baseYear, Calendar.JANUARY, 1, 0, 0, 0);
            origin.set(Calendar.MILLISECOND, 0);
            int dow = origin.get(Calendar.DAY_OF_WEEK) - 1;
            origin.add(Calendar.DAY_OF_YEAR, -dow); // Sunday of first week

            Calendar target = Calendar.getInstance();
            target.setTimeInMillis(dayMs);
            target.set(Calendar.HOUR_OF_DAY, 0); target.set(Calendar.MINUTE, 0);
            target.set(Calendar.SECOND, 0); target.set(Calendar.MILLISECOND, 0);

            long diffMs = target.getTimeInMillis() - origin.getTimeInMillis();
            int diffDays = (int)(diffMs / 86400000L);
            int row = diffDays / 7;
            return row * dp(CELL_H_DP);
        }

        private void updateCompactMonthLabel() {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(weekAnchorDayMs);
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            String monthYearText = sdf.format(c.getTime());
            if (monthYearLabel != null) {
                monthYearLabel.setText(monthYearText);
            }
        }

        private void updateMonthYearLabelFromScroll() {
            // Calculate which month is most visible based on expandScrollY
            float scrollY = expandScrollY;
            int firstVisibleRow = (int)(scrollY / dp(CELL_H_DP));
            long firstVisibleDay = cellIndexToDay(firstVisibleRow * 7);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(firstVisibleDay);
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            if (monthYearLabel != null) {
                monthYearLabel.setText(sdf.format(c.getTime()));
            }
        }

        private void animateTo(float target, int duration) {
            if (expandAnim != null) expandAnim.cancel();
            expandAnim = ValueAnimator.ofFloat(expandFraction, target);
            expandAnim.setDuration(duration);
            expandAnim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            expandAnim.addUpdateListener(a -> {
                expandFraction = (float) a.getAnimatedValue();
                requestLayout(); invalidate();
                if (expandFrameListener != null) expandFrameListener.onFrame();
            });
            expandAnim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (expandListener != null) expandListener.onToggle(expandFraction > 0.5f);
                }
            });
            expandAnim.start();
        }

        private void animateScrollTo(float targetY) {
            ValueAnimator a = ValueAnimator.ofFloat(expandScrollY, targetY);
            a.setDuration(280);
            a.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            a.addUpdateListener(u -> { expandScrollY = (float) u.getAnimatedValue(); invalidate(); });
            a.start();
        }

        private void snapToNearestRow() {
            snapToNearestRowWithFling(0);
        }

        private void snapToNearestRowWithFling(float velocityY) {
            float rowHeight = dp(CELL_H_DP);
            float currentRow = expandScrollY / rowHeight;

            // Calculate target row based on velocity and position
            int targetRow;
            if (Math.abs(velocityY) > FLING_MIN_VELOCITY) {
                // Fling: determine direction and calculate target with momentum
                int velocityDirection = velocityY < 0 ? -1 : 1; // Negative velocity = scrolling up
                // Fling 2-4 rows based on velocity
                float velocityFactor = Math.min(Math.abs(velocityY) / FLING_MAX_VELOCITY, 1.0f);
                int flingRows = (int) (1 + velocityFactor * 3); // 1-4 rows
                targetRow = (int) currentRow + (velocityDirection * flingRows);
            } else {
                // No significant velocity: snap to nearest
                targetRow = Math.round(currentRow);
            }

            // Clamp target to valid range
            targetRow = Math.max(0, targetRow);

            float targetY = targetRow * rowHeight;
            float distance = Math.abs(targetY - expandScrollY);

            // Calculate duration based on distance and velocity (max 400ms for fling)
            int maxDuration = Math.abs(velocityY) > FLING_MIN_VELOCITY ? 400 : 250;
            float maxDistance = rowHeight * 6; // 6 rows max
            int duration = (int)Math.min(maxDuration, (distance / maxDistance) * maxDuration);
            duration = Math.max(duration, 100); // Minimum 100ms

            // Animate with CubicBezierInterpolator.EASE_OUT_QUINT
            ValueAnimator snapAnim = ValueAnimator.ofFloat(expandScrollY, targetY);
            snapAnim.setDuration(duration);
            snapAnim.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            snapAnim.addUpdateListener(animation -> {
                expandScrollY = (float) animation.getAnimatedValue();
                // Update month-year label based on scroll position
                updateMonthYearLabelFromScroll();
                invalidate();
            });
            snapAnim.start();
        }

    } // end CalendarHeaderView

    // TaskFormSheet
    private abstract static class TaskFormSheet extends android.app.Dialog {
        private final Task editTask;
        private final long defaultDayMs;
        private final Theme.ResourcesProvider resourcesProvider;
        private long chosenDayMs;
        private int chosenPriority = 0;
        private boolean chosenReminder = false;
        private EditText titleEdit, descEdit;
        private TextView dateBtn;
        private View[] priorityBtns;

        TaskFormSheet(android.app.Activity activity, Task editTask, long defaultDayMs,
                      Theme.ResourcesProvider rp) {
            super(activity, R.style.TransparentDialog);
            this.editTask = editTask; this.defaultDayMs = defaultDayMs;
            this.resourcesProvider = rp; this.chosenDayMs = defaultDayMs;
            if (editTask != null) {
                chosenDayMs = editTask.dateMs;
                chosenPriority = editTask.priority;
                chosenReminder = editTask.hasReminder;
            }
        }

        protected abstract void onTaskSaved(Task task, boolean isEdit);
        protected abstract void onDismissed();

        @Override
        protected void onCreate(android.os.Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Context context = getContext();
            FrameLayout root = new FrameLayout(context);
            root.setBackgroundColor(Color.TRANSPARENT);

            LinearLayout sheet = new LinearLayout(context);
            sheet.setOrientation(LinearLayout.VERTICAL);
            sheet.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            sheet.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight() + dp(20), dp(20));
                }
            });
            sheet.setClipToOutline(true);
            sheet.setPadding(dp(20), dp(8), dp(20), dp(24));

            // Drag handle
            View handle = new View(context);
            android.graphics.drawable.GradientDrawable hBg = new android.graphics.drawable.GradientDrawable();
            hBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            hBg.setCornerRadius(dp(2));
            hBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            handle.setBackground(hBg);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(36), dp(4));
            hlp.gravity = Gravity.CENTER_HORIZONTAL; hlp.topMargin = dp(4); hlp.bottomMargin = dp(16);
            sheet.addView(handle, hlp);

            TextView sheetTitle = new TextView(context);
            sheetTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            sheetTitle.setTypeface(AndroidUtilities.bold());
            sheetTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            sheetTitle.setText(editTask == null ? getString(R.string.PlannerNewTask) : getString(R.string.PlannerEditTask));
            sheet.addView(sheetTitle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            sheet.addView(spacer(context, 16));

            titleEdit = buildEditText(context, getString(R.string.PlannerTaskTitle), false);
            if (editTask != null) titleEdit.setText(editTask.title);
            sheet.addView(titleEdit, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            sheet.addView(spacer(context, 12));

            descEdit = buildEditText(context, getString(R.string.PlannerTaskDesc), true);
            if (editTask != null && !TextUtils.isEmpty(editTask.description)) descEdit.setText(editTask.description);
            sheet.addView(descEdit, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            sheet.addView(spacer(context, 16));

            sheet.addView(buildSectionLabel(context, getString(R.string.PlannerDate)));
            sheet.addView(spacer(context, 6));
            dateBtn = buildChipButton(context, BonyanPlannerFragment.formatShortDate(chosenDayMs));
            dateBtn.setOnClickListener(v -> showDatePicker(context));
            sheet.addView(dateBtn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            sheet.addView(spacer(context, 16));

            sheet.addView(buildSectionLabel(context, getString(R.string.PlannerPriority)));
            sheet.addView(spacer(context, 6));
            sheet.addView(buildPriorityRow(context));
            sheet.addView(spacer(context, 16));

            LinearLayout reminderRow = new LinearLayout(context);
            reminderRow.setOrientation(LinearLayout.HORIZONTAL);
            reminderRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView reminderLabel = new TextView(context);
            reminderLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            reminderLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            reminderLabel.setText(getString(R.string.PlannerReminder));
            reminderRow.addView(reminderLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            android.widget.Switch sw = new android.widget.Switch(context);
            sw.setChecked(chosenReminder);
            sw.setOnCheckedChangeListener((b, checked) -> chosenReminder = checked);
            reminderRow.addView(sw);
            sheet.addView(reminderRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            sheet.addView(spacer(context, 24));

            TextView saveBtn = new TextView(context);
            saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            saveBtn.setTypeface(AndroidUtilities.bold());
            saveBtn.setTextColor(Color.WHITE);
            saveBtn.setGravity(Gravity.CENTER);
            saveBtn.setText(getString(R.string.Done));
            android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
            saveBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            saveBg.setCornerRadius(dp(12));
            saveBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
            saveBtn.setBackground(saveBg);
            saveBtn.setPadding(0, dp(14), 0, dp(14));
            saveBtn.setOnClickListener(v -> {
                String title = titleEdit.getText().toString().trim();
                if (TextUtils.isEmpty(title)) { titleEdit.setError(getString(R.string.PlannerTitleRequired)); return; }
                Task task = editTask != null ? editTask : new Task();
                task.title = title; task.description = descEdit.getText().toString().trim();
                task.dateMs = chosenDayMs; task.priority = chosenPriority; task.hasReminder = chosenReminder;
                onTaskSaved(task, editTask != null); dismiss();
            });
            sheet.addView(saveBtn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            root.addView(sheet, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
            setContentView(root);

            android.view.Window window = getWindow();
            if (window != null) {
                window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT);
                window.setGravity(Gravity.BOTTOM);
                window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
            setOnDismissListener(d -> onDismissed());
            sheet.setTranslationY(dp(600));
            sheet.post(() -> sheet.animate().translationY(0).setDuration(380).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).start());
        }

        private void showDatePicker(Context context) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(chosenDayMs);
            android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(context,
                    (view, year, month, day) -> {
                        chosenDayMs = BonyanPlannerFragment.dayMidnight(year, month, day);
                        dateBtn.setText(BonyanPlannerFragment.formatShortDate(chosenDayMs));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        }

        private View buildPriorityRow(Context context) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(4);
            String[] labels = {
                getString(R.string.PlannerPriorityNone), getString(R.string.PlannerPriorityLow),
                getString(R.string.PlannerPriorityMed),  getString(R.string.PlannerPriorityHigh)
            };
            int[] colors = { 0xFF9E9E9E, 0xFF43A047, 0xFFFB8C00, 0xFFE53935 };
            priorityBtns = new View[4];
            for (int i = 0; i < 4; i++) {
                final int idx = i;
                TextView btn = new TextView(context);
                btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                btn.setTypeface(AndroidUtilities.bold());
                btn.setGravity(Gravity.CENTER);
                btn.setText(labels[i]);
                btn.setTextColor(Color.WHITE);
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                bg.setCornerRadius(dp(8));
                bg.setColor(colors[i]);
                btn.setBackground(bg);
                btn.setPadding(dp(4), dp(8), dp(4), dp(8));
                btn.setAlpha(chosenPriority == i ? 1f : 0.35f);
                btn.setOnClickListener(v -> {
                    chosenPriority = idx;
                    for (int j = 0; j < priorityBtns.length; j++)
                        priorityBtns[j].animate().alpha(j == idx ? 1f : 0.35f).setDuration(150).start();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(dp(2), 0, dp(2), 0);
                row.addView(btn, lp);
                priorityBtns[i] = btn;
            }
            return row;
        }

        private EditText buildEditText(Context context, String hint, boolean multiline) {
            EditText et = new EditText(context);
            et.setHint(hint);
            et.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            et.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            et.setPadding(0, dp(8), 0, dp(8));
            if (multiline) {
                et.setMinLines(2); et.setMaxLines(4);
                et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            } else {
                et.setSingleLine(true);
                et.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
            }
            return et;
        }

        private TextView buildChipButton(Context context, String text) {
            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            tv.setTypeface(AndroidUtilities.bold());
            tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
            tv.setText(text);
            tv.setPadding(dp(14), dp(7), dp(14), dp(7));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(20));
            bg.setStroke(dp(1), Theme.getColor(Theme.key_windowBackgroundWhiteBlueButton));
            bg.setColor(Color.TRANSPARENT);
            tv.setBackground(bg);
            return tv;
        }

        private TextView buildSectionLabel(Context context, String text) {
            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            tv.setTypeface(AndroidUtilities.bold());
            tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            tv.setText(text.toUpperCase(Locale.getDefault()));
            tv.setLetterSpacing(0.05f);
            return tv;
        }

        private View spacer(Context context, int heightDp) {
            View v = new View(context);
            v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)));
            return v;
        }

    } // end TaskFormSheet

} // end BonyanPlannerFragment
