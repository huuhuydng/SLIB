# Module 10 - News & Announcement Module

This folder contains the Report 4 diagrams for Module 10 - News & Announcement Module.

## Included Artifacts

- Sequence diagrams for:
  - `FE-103` View list of news & announcements
  - `FE-104` View news & announcement details
  - `FE-105` View list of news & announcement categories
  - `FE-106` View list of new books
  - `FE-107` View basic information of new book
  - `FE-108a` View and update new book
  - `FE-108b` Create new book
  - `FE-108c` Delete new book
  - `FE-109a` View and update news & announcement
  - `FE-109b` Create news & announcement
  - `FE-109c` Delete news & announcement
  - `FE-110a` View and update news & announcement category
  - `FE-110b` Create news & announcement category
  - `FE-110c` Delete news & announcement category
  - `FE-111` Set time to post news & announcement
  - `FE-112` Save news & announcement draft
- Class diagram for the News & Announcement module

## Actor Scope

- `FE-103`: Librarian, Student, Teacher
- `FE-104`: Librarian, Student, Teacher
- `FE-105`: Librarian
- `FE-106`: Librarian, Student, Teacher
- `FE-107`: Librarian, Student, Teacher
- `FE-108a` to `FE-108c`: Librarian
- `FE-109a` to `FE-109c`: Librarian
- `FE-110a` to `FE-110c`: Librarian
- `FE-111`: Librarian
- `FE-112`: Librarian

## Current Working Assumptions

- Public news listing and detail viewing are currently implemented in the mobile app through `NewsScreen`, `NewsDetailScreen`, and `NewsService`.
- Public new book listing and detail viewing are currently implemented in the mobile app through `NewBooksScreen`, `NewBookDetailScreen`, and `NewBookService`.
- Librarian management for news, announcements, new books, and categories is currently implemented in the web portal through `NotificationManage.jsx`, `NewCreate.jsx`, `NewBookManage.jsx`, and `NewBookCreate.jsx`.
- `FE-109a` to `FE-109c` follow the current backend flow in `NewsController` and `NewsService`, including admin list/detail, create, update, delete, publish now, pin, schedule publication, and notification dispatch when content becomes published.
- `FE-108a` to `FE-108c` follow the current backend flow in `NewBookController` and `NewBookService`, including admin list/detail, preview import from OPAC URL, create, update, toggle active, toggle pin, and delete operations.
- `FE-110a` to `FE-110c` are aligned to the current implementation, where category management supports list, create, and delete. A dedicated update endpoint for news categories does not currently exist in the backend, so `FE-110a` documents the current view flow and the existing limitation for update behavior.
- `FE-111` is based on the existing scheduled publishing behavior where the web form submits a future `publishedAt` value and the backend schedules publication through `NewsScheduler`.
- `FE-112` is based on the current web implementation in `NewCreate.jsx`, where draft content is auto-saved and restored from browser `localStorage` using the `news_draft` key. There is no dedicated backend draft persistence API in the current system.
- `FE-104` reflects the current mobile implementation, where the detail screen renders the selected news item immediately and triggers the backend detail endpoint mainly to increment view count.
- `FE-107` reflects the current mobile implementation, where the detail screen opens with the selected book and then refreshes the displayed data by calling the public new book detail endpoint.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.
