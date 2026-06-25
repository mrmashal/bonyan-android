Based on my analysis of the Telegram Android source code, here's an overview of how dates are stored and displayed:

## Date Storage

**Internal Storage:** Dates are stored as Unix timestamps (milliseconds since epoch) using standard `long` values. The code uses `java.util.Calendar` and `java.util.Date` for internal date manipulations.

**Key Classes:**
- [`LocaleController.java`](file:///d:/dev/bonyan-android/TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java) - Central date formatting hub
- [`CalendarActivity.java`](file:///d:/dev/bonyan-android/TMessagesProj/src/main/java/org/telegram/ui/CalendarActivity.java) - Calendar UI component
- [`SunDate.java`](file:///d:/dev/bonyan-android/TMessagesProj/src/main/java/org/telegram/messenger/time/SunDate.java) - Sun position calculations (for prayer times)

## Date Display in UI

The app uses a custom `FastDateFormat` system (in [`time/`](file:///d:/dev/bonyan-android/TMessagesProj/src/main/java/org/telegram/messenger/time/) package) with multiple formatters for different contexts:

- `formatterDay` - "HH:mm" or "h:mm a"
- `formatterDayMonth` - "dd MMM"
- `formatterYear` - "dd.MM.yy"
- `formatterWeek` - Day of week abbreviations
- Various schedule formatters for messages

## Calendar System Support

**No, the Telegram Android app does NOT support non-Gregorian calendar systems.** Here's what I found:

1. **Gregorian Only:** The code exclusively uses `java.util.Calendar.getInstance()` and `java.util.GregorianCalendar` patterns. There are no references to:
   - Persian/Jalali calendar
   - Islamic/Hijri calendar
   - Umm al-Qura calendar
   - Any other non-Gregorian calendar system

2. **Locale-Based Formatting:** While the app supports RTL languages (Arabic, Persian, Hebrew) and locale-specific date formatting patterns, the underlying calendar is always Gregorian.

3. **Special Case - SunDate:** The [`SunDate.java`](file:///d:/dev/bonyan-android/TMessagesProj/src/main/java/org/telegram/messenger/time/SunDate.java) class calculates sun position for prayer times, but this is astronomical calculation, not a calendar system.

## Summary

| Aspect | Implementation |
|--------|---------------|
| Storage | Unix timestamps (long) |
| Internal API | `java.util.Calendar`/`Date` |
| Formatting | Custom `FastDateFormat` |
| Calendar System | **Gregorian only** |
| Non-Gregorian support | **None** |