# Report 4 Feature Breakdown

This file records the feature grouping used for Report 4.

## Major Features

### Authentication Module

- `FE-01`: Log in via Google Account
- `FE-02`: Log in via SLIB Account
- `FE-03`: Forgot password
- `FE-04`: Log out

### Account Management Module

- `FE-05`: View profile
- `FE-06`: Change basic profile
- `FE-07`: Change password
- `FE-08`: View Barcode
- `FE-09`: View history of activities
- `FE-10`: View account setting
- `FE-11`: Turn on/Turn off notification
- `FE-12`: Turn on/Turn off AI suggestion
- `FE-13`: Turn on/Turn off HCE feature

### User Management Module

- `FE-14`: View list of users in the system
- `FE-15`: Import Student and Teacher via file
- `FE-16`: Download template of the file upload
- `FE-17`: Add Librarian to the system
- `FE-18`: View user details
- `FE-19`: Change user status
- `FE-20`: Delete user account

### System Configuration Module

#### Area Management

- `FE-21`: View area map
- `FE-22`: CRUD area
- `FE-23`: Change area status
- `FE-24`: Lock area movement

#### Zone Management

- `FE-25`: View zone map
- `FE-26`: CRUD zone
- `FE-27`: CRUD zone attribute
- `FE-28`: View zone details
- `FE-29`: Lock zone movement

#### Seat Management

- `FE-30`: View seat map
- `FE-31`: CRUD seat
- `FE-32`: Change seat status

#### Reputation Rule Management

- `FE-33`: View list of reputation rules
- `FE-34`: CRUD reputation rule
- `FE-35`: Set the deducted point for each reputation rule

#### Library Configuration Management

- `FE-36`: Set library operating hours
- `FE-37`: Configure booking rules
- `FE-38`: Turn on/Turn off automatic check-out when time exceeds
- `FE-39`: Enable/Disable library

#### HCE Scan Station Management

- `FE-40`: View HCE scan stations
- `FE-41`: View HCE scan stations details
- `FE-42`: Manage HCE station registration

#### AI Configuration Management

- `FE-43`: CRUD material
- `FE-44`: View list of materials
- `FE-45`: CRUD knowledge store
- `FE-46`: View list of knowledge stores
- `FE-47`: Test AI chat

#### NFC Tag Management

- `FE-48`: Manage NFC Tag UID mapping
- `FE-49`: View NFC Tag mapping list
- `FE-50`: View NFC Tag mapping details

#### Kiosk Management

- `FE-51`: View list of Kiosk images
- `FE-52`: CRUD Kiosk image
- `FE-53`: Change image status
- `FE-54`: Preview Kiosk display

#### Others

- `FE-55`: Config system notification
- `FE-56`: View system overview information
- `FE-57`: View system log
- `FE-58`: Backup data manually
- `FE-59`: Set automatic backup schedule

### Booking Seat Module

- `FE-60`: View real time seat map
- `FE-61`: Filter seat map
- `FE-62`: View map density
- `FE-63`: Booking seat
- `FE-64`: Preview booking information
- `FE-65`: Confirm booking via NFC
- `FE-66`: View history of booking
- `FE-67`: Cancel booking
- `FE-68`: Ask AI for recommending seat
- `FE-69`: View list of user bookings
- `FE-70`: Search and Filter user booking
- `FE-71`: View booking details and status

### Library Access Module

- `FE-72`: Check-in/Check-out library via HCE
- `FE-73`: Check-in/Check-out library via QR code
- `FE-74`: View history of check-ins/check-outs
- `FE-75`: View list of users access to library

### Reputation & Violation Module

#### Reputation Score Management

- `FE-76`: View reputation score
- `FE-77`: View history of changed reputation points
- `FE-78`: View detailed reason for deducting point
- `FE-79`: View list of users violation
- `FE-80`: View user violation details

#### Complaint Management

- `FE-81`: Create complaint
- `FE-82`: View history of sending complaint
- `FE-83`: View list of complaints
- `FE-84`: View complaint details
- `FE-85`: Verify complaint

### Feedback Module

#### Feedback System Management

- `FE-86`: Create feedback after check-out
- `FE-87`: View list of feedbacks
- `FE-88`: View feedback details

#### Seat Status Management

- `FE-89`: Create seat status report
- `FE-90`: View history of sending seat status report
- `FE-91`: View list of seat status reports
- `FE-92`: View seat status report details
- `FE-93`: Verify seat status report

#### Report Seat Violation Management

- `FE-94`: Create report seat violation
- `FE-95`: View history of sending report seat violation
- `FE-96`: View list of seat violation reports
- `FE-97`: View report seat violation details
- `FE-98`: Verify seat violation report

### Notification Module

- `FE-99`: View and delete list of notifications
- `FE-100`: View notification details
- `FE-101`: Filter notification
- `FE-102`: Mark notification as read

### News & Announcement Module

- `FE-103`: View list of news & announcements
- `FE-104`: View news & announcement details
- `FE-105`: View list of news & announcement categories
- `FE-106`: View list of new books
- `FE-107`: View basic information of new book
- `FE-108`: CRUD new book
- `FE-109`: CRUD news & announcement
- `FE-110`: CRUD news & announcement category
- `FE-111`: Set time to post news & announcement
- `FE-112`: Save news & announcement draft

### Chat & Support Module

- `FE-113`: Chat with AI virtual assistant
- `FE-114`: Chat with Librarian
- `FE-115`: Send request for support
- `FE-116`: View list of support requests
- `FE-117`: View history of chat
- `FE-118`: View list of chats
- `FE-119`: View chat details
- `FE-120`: Response to user manually
- `FE-121`: Response to user with AI suggestion

### Statistics & Report Module

#### Statistics Management

- `FE-122`: View general analytics dashboard
- `FE-123`: View violation statistics
- `FE-124`: View statistics of density forecast by using AI
- `FE-125`: View check-in/check-out statistics (Daily/Weekly/Monthly)
- `FE-126`: View seat booking statistics

#### Report Management

- `FE-127`: Export seat & maintenance report
- `FE-128`: Export general analytical report

## Use Case Actor Mapping

This section is the actor-based use case reference for the next modules and diagrams in Report 4.

| Use Case | Actors |
| --- | --- |
| Log in via Google Account | Admin, Student, Teacher, Librarian |
| Log in via SLIB Account | Admin, Student, Teacher, Librarian |
| Forgot password | Admin, Student, Teacher, Librarian |
| Log out | Admin, Student, Teacher, Librarian |
| View profile | Admin, Student, Teacher, Librarian |
| Change basic profile | Admin, Student, Teacher, Librarian |
| Change password | Admin, Student, Teacher, Librarian |
| View Barcode | Student, Teacher |
| View history of activities | Student, Teacher |
| View account setting | Student, Teacher |
| Turn on/Turn off notification | Student, Teacher |
| Turn on/Turn off AI suggestion | Student, Teacher |
| Turn on/Turn off HCE feature | Student, Teacher |
| View list of users in the system | Admin |
| Import Student and Teacher via file | Admin |
| Download template of file upload | Admin |
| Add Librarian to the System | Admin |
| View user details | Admin |
| Change user status | Admin |
| Delete user account | Admin |
| Create area | Admin |
| Update area | Admin |
| Delete area | Admin |
| View area map | Admin |
| Change area status | Admin |
| Lock area movement | Admin |
| Create zone | Admin |
| Update zone | Admin |
| Delete zone | Admin |
| View zone map | Admin |
| Create zone attribute | Admin |
| Update zone attribute | Admin |
| Delete zone attribute | Admin |
| View zone details | Admin |
| Lock zone movement | Admin |
| Create seat | Admin |
| Update seat | Admin |
| Delete seat | Admin |
| View seat map | Admin |
| Change seat status | Admin |
| Create reputation rule | Admin |
| Update reputation rule | Admin |
| Delete reputation rule | Admin |
| View list of reputation rules | Admin |
| Set deducted point for each rule | Admin |
| Set library operating hours | Admin |
| Configure booking rules | Admin |
| Set automatic backup schedule | Admin |
| Turn on/Turn off automatic check-out | Admin |
| Enable/Disable Library | Admin |
| View HCE scan station | Admin |
| Register HCE station | Admin |
| Update HCE station | Admin |
| Delete HCE station | Admin |
| View HCE scan station details | Admin |
| Create material | Admin |
| View list of materials | Admin |
| Update material | Admin |
| Delete material | Admin |
| Create knowledge store | Admin |
| View list of knowledge stores | Admin |
| Update knowledge store | Admin |
| Delete knowledge store | Admin |
| Create NFC Tag UID mapping | Admin |
| Update NFC Tag UID mapping | Admin |
| Delete NFC Tag UID mapping | Admin |
| View NFC Tag mapping list | Admin |
| View NFC Tag mapping details | Admin |
| Test AI chat | Admin |
| Create Kiosk image | Admin |
| View list of Kiosk images | Admin |
| Update Kiosk image | Admin |
| Delete Kiosk image | Admin |
| Change image status | Admin |
| Preview Kiosk display | Admin |
| Config system notification | Admin |
| View system overview information | Admin |
| Backup data manually | Admin |
| View system log | Admin |
| View real time seat map | Librarian, Student, Teacher |
| Filter seat map | Librarian, Student, Teacher |
| View map density | Librarian, Student, Teacher |
| Booking seat | Student, Teacher |
| Preview booking information | Student, Teacher |
| Confirm booking via NFC | Student, Teacher |
| View history of booking | Student, Teacher |
| Cancel booking | Student, Teacher |
| Ask AI for recommending seat | Student, Teacher |
| View list of user bookings | Librarian |
| Search & Filter user booking | Librarian |
| View user booking details | Librarian |
| Check-in library via HCE | Student, Teacher |
| Check-out library via HCE | Student, Teacher |
| Check-in library via QR code | Student, Teacher |
| Check-out library via QR code | Student, Teacher |
| View history of check-ins/check-outs | Student, Teacher |
| View list of Students access to library | Admin, Librarian |
| View reputation score | Student, Teacher |
| View history of changed reputation score | Student, Teacher |
| View detailed reason of deducting points | Student, Teacher |
| Create complaint | Student, Teacher |
| View history of sending complaint | Student, Teacher |
| View list of users violation | Librarian |
| View user violation details | Librarian |
| View list of complaints | Librarian |
| View complaint details | Librarian |
| Accept/Deny complaint | Librarian |
| Create feedback after check-out | Student, Teacher |
| Create seat status report | Student, Teacher |
| View history of sending seat status report | Student, Teacher |
| Create report seat violation | Student, Teacher |
| View history of sending report seat violation | Student, Teacher |
| View list of seat status reports | Admin, Librarian |
| View seat status report details | Admin, Librarian |
| View list of seat violation reports | Admin, Librarian |
| View report seat violation details | Admin, Librarian |
| View list of feedbacks | Librarian |
| View feedback details | Librarian |
| Verify seat status report | Librarian |
| Verify seat violation report | Librarian |
| View list of notifications | Librarian, Student, Teacher |
| Delete notification | Librarian, Student, Teacher |
| View notification details | Librarian, Student, Teacher |
| Mark notification as read | Librarian, Student, Teacher |
| Create new book | Librarian |
| Update new book | Librarian |
| Delete new book | Librarian |
| Create news & announcement | Librarian |
| Update news & announcement | Librarian |
| Delete news & announcement | Librarian |
| View list of news & announcement categories | Librarian |
| Create news & announcement category | Librarian |
| Update news & announcement category | Librarian |
| Delete news & announcement category | Librarian |
| Set time to post news & announcement | Librarian |
| Save news & announcement draft | Librarian |
| View list of news & announcements | Librarian, Student, Teacher |
| View news & announcement details | Librarian, Student, Teacher |
| View list of new books | Librarian, Student, Teacher |
| View basic information of new book | Librarian, Student, Teacher |
| Chat with AI virtual assistant | Student |
| View history of chat | Student |
| Chat with Librarian | Student |
| Send request for support | Student |
| View list of chats | Librarian |
| View chat details | Librarian |
| Response to user manually | Librarian |
| Response to user with AI suggestion | Librarian |
| View general analytics dashboard | Librarian |
| View violation statistics | Librarian |
| View statistics of density forecast via AI | Librarian |
| View check-in/check-out statistics | Librarian |
| View seat booking statistics | Librarian |
| Export general analytical report | Librarian |
| Export seat & maintenance report | Librarian |
