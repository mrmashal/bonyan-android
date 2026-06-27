**IMPORTANT (DO NOT VIOLATE)**: START FROM THE EXISTING IMPLEMENTATION AND ONLY USE EXISTING TELEGRAM UI COMPONENTS AND LAYOUTS.

### 1. State Management (Logged-in vs. Logged-out)
As per the system policies, the "Family" tab behaves differently based on the authentication state:
*   **Unauthenticated State (Not Logged In):** The "Family" tab is not shown. Instead, Telegram’s original Settings tab is shown.
*   **Authenticated State (Logged In):** The tab transforms into the **Family Directory & Tree**, functioning similarly to Telegram’s "Contacts" page but enriched with family-tree relationships, tags, and reputation metrics.

---

### 2. Screen Anatomy & Layout (Authenticated State)

#### A. Top App Bar
*   **Title:** "Family".
*   **Top Right Corner:**:
    1.  **Search Icon and Search Bar:** Do NOT modify the existing UI.
    2.  **Settings/Filter Icon (⚙️ or Sliders):** Opens the advanced display and filtering modal.

#### B. Main Content Area (The Directory List)
The core of the screen is a fast, scrollable list of people the user has access to, styled exactly like Telegram’s contact list.
*   **Alphabetical Indexing:** A vertical A-Z scrollbar on the right edge for rapid navigation. As the user drags their finger along it, a floating "bubble" appears in the center of the screen showing the current letter.
*   **Section Headers:** Sticky headers for each letter or relationship category (e.g., "Immediate Family", "Extended Family", "Other Connections").
*   **Empty State:** If the user has no connections yet, a friendly illustration with a call-to-action: *"Build your family tree! Add members or link existing users."*

#### C. Floating Action Button (FAB)
*   **Position:** Bottom right corner, hovering just above the bottom navigation bar.
*   **Style:** Standard Telegram circular blue FAB with a white `person_add` or `account_tree` icon.
*   **Action:** Tapping the FAB opens a **Bottom Sheet** with two primary options:
    1.  **Add New Member (افزودن عضو جدید):** Opens a form to manually enter basic offline-first details (Name, Gender, Birth Year).
    2.  **Link Existing User (ادعای ارتباط خانوادگی):** Opens a search interface to find already registered users in the system and claim a family relationship with them (pending their approval).

---

### 3. List Item Design (The "Contact" Row)
Each row in the family list must be information-dense yet clean, utilizing the "Tag" system mentioned in the policies.
*   **Left:** Circular Avatar. If no photo is uploaded, it displays initials with a pastel-colored background.
*   **Center (Primary Text):** Full Name (Bold).
*   **Center (Secondary Text):** Relationship title (e.g., "برادر" - Brother, "عمو" - Uncle) OR a primary tag (e.g., "مهندس کشاورزی" - Agricultural Engineer).
*   **Right (Metadata):**
    *   A subtle badge indicating their **Reputation Score** or **Organizational Level** (e.g., "علمدار" or a numeric score) if the user has access to view it.
    *   An "Online/Offline" or "Sync Status" dot (green for online/synced, gray for offline/local-only).
*   **Access-Based Redaction:** If the current user does not have permission to see a person's full details, the row will only show the Name and Gender, with a small "lock" icon or "Limited Access" subtitle.

---

### 4. The Settings & Filter Modal
Triggered by the top-right gear icon, this opens as a **Telegram-style Bottom Sheet** or a full-screen modal. It allows users to customize their view without leaving the context.
*   **View Toggle:** Switch between "List View" (default) and "Visual Tree View" (a graphical node-based family tree).
*   **Sort By:** Name (Alphabetical), Reputation Score, Birth Year, Recently Added.
*   **Filter By (Chips/Toggles):**
    *   *Relationship Degree:* Immediate, Extended, Claimed (Pending).
    *   *Tags/Categories:* Filter by the extensive tag system (e.g., show only people with "Media Skills", "Investment Capital", or living in a specific "Geographic Location").
    *   *Organization Membership:* Show only members of specific organizations (e.g., "مکتب اسلامی").

---

### 5. Key User Flows

#### A. Viewing a Profile (Deep Dive)
Tapping on a person in the list opens their Profile screen (similar to Telegram’s contact info page).
*   **Header:** Large cover photo/avatar, Name, and a prominent badge showing their **Social Reputation (منزلت اجتماعی)** and organizational levels.
*   **Action Buttons:** Row of icons below the header:
    *   *Message/Contact:* Since direct in-app messaging is restricted to maintain focus, this button opens the external messaging links (WhatsApp, Telegram, etc.) that the user has registered on their profile.
    *   *Edit:* (Visible only if the user has access/ownership).
    *   *Share/Invite:* To invite them to a specific Mission or Organization.
*   **Info Sections:** Grouped accordions based on the tag system:
    *   *Personal Info:* National ID, Birthdate, Location, Education.
    *   *Skills & Economy:* Investments, Media capabilities, Agricultural capacities.
    *   *Family Tree Position:* A mini visual showing how they connect to the current user.
    *   *Shared Missions:* List of active missions both users are part of.

#### B. Claiming a Relationship
When a user clicks "Link Existing User" from the FAB:
1.  A search screen appears to find the person by Name, Phone Number, or National ID.
2.  Upon selecting the person, a prompt asks: *"What is your relationship to [Name]?"* (Dropdown: Father, Mother, Sibling, Spouse, etc.).
3.  The system sends a request. The UI shows a "Pending" hourglass icon next to that person's name in the Family list until the other party approves it via their "Public Mission" notifications.

---

### 6. Micro-interactions & System Feedback

*   **The "Undo" Snackbar:** As per the strict UI policy, if a user performs a destructive action (e.g., removing a family link, deleting a locally added member, or revoking access), a dark Snackbar slides up from the bottom (just above the navigation bar).
    *   *Text:* "Link removed."
    *   *Action:* "UNDO" (Highlighted in blue).
    *   *Behavior:* It features a 10-second linear progress border. If clicked, a confirmation dialog appears before reversing the action.
*   **Offline-First Syncing:** If a user adds a family member while offline, the list item will have a subtle "clock" icon. Once the internet is restored, a smooth, non-intrusive toast notification at the top says *"Family data synced"*, and the clock icon turns into a green checkmark.
*   **Swipe Actions:** Swiping left on a contact row reveals a red "Delete/Remove" background. Swiping right reveals a blue "Message (External)" background.

---

### 7. Visual Design & Theming
*   **Color Palette:** Follows Telegram’s exact Material/Cupertino standards. Primary actions (FAB, active states) use the standard Telegram Blue (`#3390EC`). Priority colors for missions/tags (Blue, Orange, Red) are respected if displayed on the profile.
*   **Typography:** Clean, sans-serif Persian font (like Vazirmatn or IRANSans) with clear hierarchy. Names are slightly larger and bolder than the secondary tag text.
*   **Dark Mode:** Fully supported. The list backgrounds shift to dark gray (`#1C2733`), and text adapts to ensure high contrast, reducing eye strain for daily use.
*   **Haptic Feedback:** Light haptic ticks when scrolling through the alphabetical index, and a heavier haptic thud when pulling down to refresh the family directory from the server.