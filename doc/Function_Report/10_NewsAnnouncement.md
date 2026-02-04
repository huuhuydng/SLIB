# Module 10: News & Announcement

## FE-103: View list of news & announcements

- **Actors:** Student
- **Navigation path:** Home -> "Tin tức thư viện" section / Bottom navigation -> News
- **Purpose:** View library news and announcements list (title: "Tin tức & Sự kiện")
- **Interface:** Card list with thumbnail, title, date
- **Data:** News list with preview
- **Filters:** Category tabs (All, Events, Updates, Tips)
- **Empty state:** "Tin tức mới sẽ xuất hiện tại đây"

---

## FE-104: View news & announcement details

- **Actors:** Student
- **Purpose:** View news details
- **Interface:** Full article view with media
- **Data:** Title, content (HTML), images, author, date

---

## FE-105: View list of news & announcement categories

- **Actors:** Librarian
- **Navigation path:** Sidebar -> "Quản lý thông báo"
- **Purpose:** Manage news categories
- **Interface:** List with CRUD actions
- **Data:** Category name, description, article count

---

## FE-106: View list of new books

- **Actors:** Student
- **Purpose:** View library's new book arrivals
- **Interface:** Book gallery with covers
- **Data:** Book title, author, cover image, arrival date

---

## FE-107: View basic information of new book

- **Actors:** Student
- **Purpose:** View new book details
- **Interface:** Book detail page
- **Data:** Title, author, ISBN, description, availability

---

## FE-108: CRUD new book

- **Actors:** Librarian
- **Purpose:** Manage new book information
- **Interface:** Form with book details
- **Data:** Title, author, ISBN, cover, description
- **Business rules:** Auto-notify subscribers when new book added

---

## FE-109: CRUD news & announcement

- **Actors:** Librarian
- **Navigation path:** Sidebar -> "Quản lý thông báo" -> Create/Edit news
- **Purpose:** Create/Edit/Delete news
- **Interface:** Rich text editor with media upload
- **Data:** Title, content, category, thumbnail, status

---

## FE-110: CRUD news & announcement category

- **Actors:** Librarian
- **Purpose:** Manage news categories
- **Interface:** Simple form
- **Data:** Category name, description, color

---

## FE-111: Set time to post news & announcement

- **Actors:** Librarian
- **Purpose:** Schedule automatic news posting
- **Interface:** Date/time picker in editor
- **Business rules:** Scheduled posts auto-publish at set time

---

## FE-112: Save news & announcement draft

- **Actors:** Librarian
- **Purpose:** Save news draft
- **Interface:** "Lưu nháp" (Save Draft) button
- **Business rules:** Auto-save every 30 seconds

---

