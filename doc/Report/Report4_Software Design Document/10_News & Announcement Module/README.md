# Module 10 - News & Announcement Module

This folder contains the Report 4 diagrams for Module 10 - News & Announcement Module.

## Included Artifacts

- Sequence diagrams for:
  - `FE-107` View list of news and announcements
  - `FE-108` View news and announcement details
  - `FE-109` View list of news and announcement categories
  - `FE-110` View list of new books
  - `FE-111` View basic information of new book
  - `FE-112a` View and update new book
  - `FE-112b` Create new book
  - `FE-112c` Delete new book
  - `FE-113a` View and update news and announcement
  - `FE-113b` Create news and announcement
  - `FE-113c` Delete news and announcement
  - `FE-114` Create news and announcement category
  - `FE-115` Set time to post news and announcement
  - `FE-116` Save news and announcement draft
  - `FE-117` View list of kiosk images
  - `FE-118a` View and update kiosk image
  - `FE-118b` Create kiosk image
  - `FE-118c` Delete kiosk image
  - `FE-119` Change image status
  - `FE-120` Preview kiosk display
- Class diagram for the News & Announcement module

## Actor Scope

- `FE-107`: Librarian, Student, Teacher
- `FE-108`: Librarian, Student, Teacher
- `FE-109`: Librarian
- `FE-110`: Librarian, Student, Teacher
- `FE-111`: Librarian, Student, Teacher
- `FE-112a` to `FE-112c`: Librarian
- `FE-113a` to `FE-113c`: Librarian
- `FE-114`: Librarian
- `FE-115`: Librarian
- `FE-116`: Librarian
- `FE-117`: Librarian
- `FE-118a` to `FE-118c`: Librarian
- `FE-119`: Librarian
- `FE-120`: Librarian

## Current Working Assumptions

- Public news listing and detail viewing are currently implemented in the mobile app through `NewsScreen`, `NewsDetailScreen`, and `NewsService`.
- Public new book listing and detail viewing are currently implemented in the mobile app through `NewBooksScreen`, `NewBookDetailScreen`, and `NewBookService`.
- Librarian management for news, announcements, new books, and categories is currently implemented in the web portal through `NotificationManage.jsx`, `NewCreate.jsx`, `NewBookManage.jsx`, and `NewBookCreate.jsx`.
- `FE-113a` to `FE-113c` follow the current backend flow in `NewsController` and `NewsService`, including admin list/detail, create, update, delete, publish now, pin, schedule publication, and notification dispatch when content becomes published.
- `FE-112a` to `FE-112c` follow the current backend flow in `NewBookController` and `NewBookService`, including admin list/detail, preview import from OPAC URL, create, update, toggle active, toggle pin, and delete operations.
- `FE-114` follows the current implementation, where category management supports list, create, and delete. The active baseline for Report 4 documents the actual create flow as requested in the latest breakdown.
- `FE-115` is based on the existing scheduled publishing behavior where the web form submits a future `publishedAt` value and the backend schedules publication through `NewsScheduler`.
- `FE-116` is based on the current web implementation in `NewCreate.jsx`, where draft content is auto-saved and restored from browser `localStorage` using the `news_draft` key. There is no dedicated backend draft persistence API in the current system.
- `FE-108` reflects the current mobile implementation, where the detail screen renders the selected news item immediately and triggers the backend detail endpoint mainly to increment view count.
- `FE-111` reflects the current mobile implementation, where the detail screen opens with the selected book and then refreshes the displayed data by calling the public new book detail endpoint.
- `FE-117` to `FE-120` now belong to this module because the current system implements kiosk slideshow image management and preview through `KioskSlideshowController`, `SlideshowManagement.jsx`, and `SlideshowPreview.jsx`.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.
