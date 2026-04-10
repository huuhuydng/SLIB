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
- `FE-14`: View booking restriction status by reputation

### User Management Module

- `FE-15`: View list of users in the system
- `FE-16`: Import Student and Teacher via file
- `FE-17`: Download template of the file upload
- `FE-18`: Add Librarian to the system
- `FE-19`: View user details
- `FE-20`: Change user status
- `FE-21`: Delete user account

### System Configuration Module

#### Area Management

- `FE-22`: View area map
- `FE-23`: CRUD area
- `FE-24`: Change area status
- `FE-25`: Lock area movement

#### Zone Management

- `FE-26`: View zone map
- `FE-27`: CRUD zone
- `FE-28`: CRUD zone attribute
- `FE-29`: View zone details
- `FE-30`: Lock zone movement

#### Seat Management

- `FE-31`: View seat map
- `FE-32`: CRUD seat
- `FE-33`: Change seat status

#### Reputation Rule Management

- `FE-34`: View list of reputation rules
- `FE-35`: CRUD reputation rule
- `FE-36`: Set the deducted point for each reputation rule

#### Library Configuration Management

- `FE-37`: Set library operating hours
- `FE-38`: Configure booking rules
- `FE-39`: Setting time for automatic check-out when time exceeds
- `FE-40`: Enable/Disable library

#### HCE Scan Station Management

- `FE-41`: View HCE scan stations
- `FE-42`: View HCE scan stations details
- `FE-43`: Manage HCE station registration

#### AI Configuration Management

- `FE-44`: CRUD material
- `FE-45`: View list of materials
- `FE-46`: CRUD knowledge store
- `FE-47`: View list of knowledge stores
- `FE-48`: Test AI chat

#### NFC Tag Management

- `FE-49`: Manage NFC Tag UID mapping
- `FE-50`: View NFC Tag mapping list
- `FE-51`: View NFC Tag mapping details

#### Kiosk Management

- `FE-52`: View list of Kiosk devices
- `FE-53`: View Kiosk device details
- `FE-54`: CRUD Kiosk device
- `FE-55`: Activate Kiosk device

#### Others

- `FE-56`: Config system notification
- `FE-57`: View system overview information
- `FE-58`: View system log
- `FE-59`: Backup data manually
- `FE-60`: Set automatic backup schedule

### Booking Seat Module

- `FE-61`: View real time seat map
- `FE-62`: Filter seat map
- `FE-63`: View map density
- `FE-64`: Booking seat
- `FE-65`: Preview booking information
- `FE-66`: Confirm booking via NFC
- `FE-67`: View history of booking
- `FE-68`: Cancel booking
- `FE-69`: Ask AI for recommending seat
- `FE-70`: View list of user bookings
- `FE-71`: Search and Filter user booking
- `FE-72`: View user booking details and status
- `FE-73`: Leave seat via NFC
- `FE-74`: Release occupied seat by Librarian
- `FE-75`: View actual seat end time

### Library Access Module

- `FE-76`: Check-in/Check-out library via HCE
- `FE-77`: Check-in/Check-out library via QR code
- `FE-78`: View history of check-ins/check-outs
- `FE-79`: View list of users access to library

### Reputation & Violation Module

#### Reputation Score Management

- `FE-80`: View reputation score
- `FE-81`: View history of changed reputation points
- `FE-82`: View detailed reason for deducting point
- `FE-83`: View list of users violation
- `FE-84`: View user violation details

#### Complaint Management

- `FE-85`: Create complaint
- `FE-86`: View history of sending complaint
- `FE-87`: View list of complaints
- `FE-88`: View complaint details
- `FE-89`: Verify complaint

### Feedback Module

#### Feedback System Management

- `FE-90`: Create feedback after check-out
- `FE-91`: View list of feedbacks
- `FE-92`: View feedback details

#### Seat Status Management

- `FE-93`: Create seat status report
- `FE-94`: View history of sending seat status report
- `FE-95`: View list of seat status reports
- `FE-96`: View seat status report details
- `FE-97`: Verify seat status report

#### Report Seat Violation Management

- `FE-98`: Create report seat violation
- `FE-99`: View history of sending report seat violation
- `FE-100`: View list of seat violation reports
- `FE-101`: View report seat violation details
- `FE-102`: Verify seat violation report

### Notification Module

- `FE-103`: View and delete list of notifications
- `FE-104`: View notification details
- `FE-105`: Filter notification
- `FE-106`: Mark notification as read

### News & Announcement Module

- `FE-107`: View list of news & announcements
- `FE-108`: View news & announcement details
- `FE-109`: View list of news & announcement categories
- `FE-110`: View list of new books
- `FE-111`: View basic information of new book
- `FE-112`: CRUD new book
- `FE-113`: CRUD news & announcement
- `FE-114`: Create news & announcement category
- `FE-115`: Set time to post news & announcement
- `FE-116`: Save news & announcement draft
- `FE-117`: View list of Kiosk images
- `FE-118`: CRUD Kiosk image
- `FE-119`: Change image status
- `FE-120`: Preview Kiosk display

### Chat & Support Module

- `FE-121`: Chat with AI virtual assistant
- `FE-122`: Chat with Librarian
- `FE-123`: Send request for support
- `FE-124`: View list of support requests
- `FE-125`: View history of chat
- `FE-126`: View list of chats
- `FE-127`: View chat details
- `FE-128`: Response to user manually

### Statistics & Report Module

#### Statistics Management

- `FE-129`: View general analytics dashboard
- `FE-130`: View violation statistics
- `FE-131`: View statistics of density forecast by using AI
- `FE-132`: View check-in/check-out statistics (Daily/Weekly/Monthly)
- `FE-133`: View seat booking statistics
- `FE-134`: View AI prioritized students
- `FE-135`: Send warning to student from AI analytics dashboard

#### Report Management

- `FE-136`: Export general analytical report

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
| View booking restriction status by reputation | Student, Teacher |
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
| Setting time for automatic check-out | Admin |
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
| View list of Kiosk devices | Admin |
| View Kiosk device details | Admin |
| Create Kiosk device | Admin |
| Update Kiosk device | Admin |
| Delete Kiosk device | Admin |
| Activate Kiosk device | Admin |
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
| Leave seat via NFC | Student, Teacher |
| Release occupied seat by Librarian | Librarian |
| View actual seat end time | Librarian, Student, Teacher |
| Check-in library via HCE | Student, Teacher |
| Check-out library via HCE | Student, Teacher |
| Check-in library via QR code | Student, Teacher |
| Check-out library via QR code | Student, Teacher |
| View history of check-ins/check-outs | Student, Teacher |
| View list of users access to library | Admin, Librarian |
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
| Set time to post news & announcement | Librarian |
| Save news & announcement draft | Librarian |
| View list of news & announcements | Librarian, Student, Teacher |
| View news & announcement details | Librarian, Student, Teacher |
| View list of new books | Librarian, Student, Teacher |
| View basic information of new book | Librarian, Student, Teacher |
| Create Kiosk image | Librarian |
| View list of Kiosk images | Librarian |
| Update Kiosk image | Librarian |
| Delete Kiosk image | Librarian |
| Change image status | Librarian |
| Preview Kiosk display | Librarian |
| Chat with AI virtual assistant | Student |
| View history of chat | Student |
| Chat with Librarian | Student |
| Send request for support | Student |
| View list of support requests | Librarian |
| View list of chats | Librarian |
| View chat details | Librarian |
| Response to user manually | Librarian |
| View general analytics dashboard | Librarian |
| View violation statistics | Librarian |
| View statistics of density forecast via AI | Librarian |
| View check-in/check-out statistics | Librarian |
| View seat booking statistics | Librarian |
| View AI prioritized students | Librarian |
| Send warning to student from AI analytics dashboard | Librarian |
| Export general analytical report | Librarian |
