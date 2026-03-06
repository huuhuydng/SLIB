# Report3_Software Requirement Specification

![](Report3_Software%20Requirement%20Specification/image20.png)

**CAPSTONE PROJECT REPORT**

**Report 3 – Software Requirement Specification**

> – Da Nang, January 2026 –
> 

**Table of Contents**

I. Record of Changes 6

II. Software Requirement Specification 7

> 1. Product Overview 7
> 
> 
> 2. User Requirements 7
> 
> 2.1 Actors 7
> 
> 2.2 Use Cases 9
> 
> 2.2.1 Diagram(s) 9
> 
> 2.2.2 Descriptions 19
> 
> 3. Functional Requirements 26
> 
> 3.1 System Functional Overview 26
> 
> 3.1.1 Screens Flow 26
> 
> 3.1.2 Screen Descriptions 27
> 
> 3.1.3 Screen Authorization 31
> 
> 3.1.4 Non-Screen Functions 32
> 
> 3.1.5 Entity Relationship Diagram 33
> 
> 3.2 Authentication 34
> 
> 3.2.1 Log in via Google Account 34
> 
> 3.2.2 Log in via SLIB Account 38
> 
> 3.2.3 Log out 40
> 
> 3.3 Account Management 43
> 
> 3.3.1 View profile 43
> 
> 3.3.2 Change basic profile 43
> 
> 3.3.3. Change password 43
> 
> 3.3.4. View Barcode 43
> 
> 3.4 User Management Module 45
> 
> 3.4.1 View list of users in the system 45
> 
> 3.4.2 Import Student via file and download template 46
> 
> 3.4.3 Add Librarian to the system 48
> 
> 3.4.4 View user details 49
> 
> 3.4.5 Change user status 49
> 
> 3.4.5 Delete user account 51
> 
> 3.5 System Configuration 51
> 
> 3.5.1 Area Management 51
> 
> 3.5.1.1 View area map 51
> 
> 3.5.1.2 Create and update area 52
> 
> 3.5.1.3 Delete area 54
> 
> 3.5.1.4 Change area status 55
> 
> 3.5.1.5 Lock area movement 56
> 
> 3.5.2 Zone Management 58
> 
> 3.5.2.1 View zone map 58
> 
> 3.5.2.2 Create and update zone 59
> 
> 3.5.2.3 Delete zone 61
> 
> 3.5.2.4 Create zone attribute 62
> 
> 3.5.2.5 Delete zone attribute 64
> 
> 3.5.2.6 Lock zone movement 65
> 
> 4. Non-Functional Requirements 67
> 
> 4.1 External Interfaces 67
> 
> 4.2 Quality Attributes 67
> 
> 4.2.1 Usability 67
> 
> 4.2.2 Reliability 67
> 
> 4.2.3 Performance 67
> 
> 4.2.4 … 67
> 
> 5. Requirement Appendix 68
> 
> 5.1 Business Rules 68
> 
> 5.2 Common Requirements 68
> 
> 5.3 Application Messages List 68
> 
> 5.4 Other Requirements… 69
> 

**Table of Tables**

Table 1. Actor Descriptions 8

Table 2. Use Case Descriptions 25

Table 3. Screen Descriptions 31

Table 4. Screen Authorization 32

Table 5. Non-Screen Functions 33

**Table of Figures**

Figure 1. System Context Diagram 7

Figure 2. Use Case Diagram for Authentication 9

Figure 3. Use Case Diagram for Account Management 9

Figure 4. Use Case Diagram for User Management 10

Figure 5. Use Case Diagram for System Configuration (1) 11

Figure 6. Use Case Diagram for System Configuration (2) 12

Figure 7. Use Case Diagram for Booking Seat Management 13

Figure 8. Use Case Diagram for Library Access Management 13

Figure 9. Use Case Diagram for Reputation & Violation Management 14

Figure 10. Use Case Diagram for Feedback Management 15

Figure 11. Use Case Diagram for Notification Management 16

Figure 12. Use Case Diagram for News & Announcements Management 17

Figure 13. Use Case Diagram for Chat & Support Management 18

Figure 14. Use Case Diagram for Statistics & Report Management 19

Figure 15. Screen Flow Diagram of Librarian (Website) 26

Figure 16. Screen Flow Diagram of Student (Mobile) 27

Figure 17. Screen Flow Diagram of Admin (Website) 27

Figure 18. Entity Relationship Diagram 33

Figure 19. Login via Google Account Screen Layout (For Web) 35

Figure 20. Login via Google Account Screen Layout (For Mobile) 36

Figure 21. Not Auth Screen Layout 37

Figure 22. Log in via SLIB Account Screen Layout 39

Figure 23. Login out Screen Layout (For Web) 41

Figure 24. Login out Screen Layout (For Mobile) 42

Figure 25. View Barcode Screen Layout 44

Figure 26. View list of users in the system Screen Layout 46

Figure 27. Import Student via file Screen Layout 47

Figure 28. Add Librarian to the system Screen Layout 49

Figure 29. Change user status Screen Layout 50

Figure 30. View area map Screen Layout 52

Figure 31. Create and update area Screen Layout 53

Figure 32. Delete area Screen Layout 55

Figure 33. Change area status Screen Layout 56

Figure 34. Lock area movement Screen Layout 57

Figure 35. View zone map Screen Layout 59

Figure 36. Create and update zone Screen Layout 60

Figure 37. Delete zone 62

Figure 38. Create zone attribute 63

Figure 39. Delete zone attribute 65

Figure 40. Delete zone attribute 66

# I. Record of Changes

| **Date** | **A*
M, D** | **In charge** | **Change Description** |
| --- | --- | --- | --- |
| 2026/01/15 | A | ThongDV | Create Product Overview |
| 2026/01/17 | A | ThongDV | Create User Requirements |
| 2026/01/24 | A | All members | Create Functional Requirements |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
- A - Added M - Modified D - Deleted

# II. Software Requirement Specification

## 1. Product Overview

The Smart Library System (SLIB) is an automated solution designed to optimize the process of seat reservation and library access control. This system replaces traditional manual attendance tracking with a streamlined, digital workflow to improve seat utilization and enhance the library experience for students and staff.

The SLIB system acts as a central hub connecting four primary external entities:

- **Students:** The primary users who interact with the system to provide login credentials, booking requests, and NFC/HCE data for identity verification. In return, students receive real-time seat maps, booking confirmations, notifications, and AI-driven recommendations for optimal study spots.
- **Librarians:** Responsible for day-to-day operational monitoring. They receive dashboard metrics, check-in/out logs, and statistical reports from the system. Librarians provide inputs such as seat status updates, reputation adjustments for students, and responses to feedback or inquiries.
- **Admins:** Oversee the system's technical configuration. They manage user data imports, system settings, library map layouts, and NFC device mapping. The system provides admins with detailed system logs and backup status reports to ensure operational stability.
- **Google System:** An external authentication provider that supplies user identity information and receives auth token requests to ensure secure access to the platform.

![](Report3_Software%20Requirement%20Specification/image23.jpg)

***Figure 1. System Context Diagram***

## 2. User Requirements

### 2.1 Actors

| **#** | **Actor** | **Description** |
| --- | --- | --- |
| 1 | Admin | The Admin holds the highest authority to manage the technical infrastructure and core configurations of the ecosystem. This role is responsible for managing all user accounts, defining roles, and setting system permissions. Admins configure essential system settings, including seat layout designs, smart booking rules, and the parameters for the reputation scoring system. They are also in charge of designing the digital seat map layout and managing technical aspects like system logs and data backups. |
| 2 | Librarian | Librarian acts as the operational staff responsible for managing daily library activities through a dedicated web portal. Their role involves monitoring real-time check-in and check-out records to track student presence and library occupancy. They are responsible for handling student feedback that has been categorized and analyzed by the AI system. Librarians communicate directly with students to resolve issues and regularly monitor library usage statistics to ensure efficient space management. |
| 3 | Student | Students are the primary user of the library services through the SLIB mobile application. To access the system, students from Batch K18 and older use their campus-provided email ending in @fe.edu.vn, while students from Batch K19 and later utilize their FEID (Personal email) via the modernized authentication system. These users book specific seats and perform check-in or check-out activities using Host Card Emulation (HCE) technology. Students can view their personal usage history, track their Reputation Score, and receive system notifications. Furthermore, they can submit feedback, interact with the AI chatbot for support, and receive personalized study time recommendations based on AI analytics. |

***Table 1. Actor Descriptions***

### 2.2 Use Cases

### 2.2.1 Diagram(s)

![](Report3_Software%20Requirement%20Specification/image38.jpg)

***Figure 2. Use Case Diagram for Authentication***

![](Report3_Software%20Requirement%20Specification/image41.jpg)

***Figure 3. Use Case Diagram for Account Management***

![](Report3_Software%20Requirement%20Specification/image40.jpg)

***Figure 4. Use Case Diagram for User Management***

![](Report3_Software%20Requirement%20Specification/image39.jpg)

***Figure 5. Use Case Diagram for System Configuration (1)***

![](Report3_Software%20Requirement%20Specification/image7.jpg)

***Figure 6. Use Case Diagram for System Configuration (2)***

![](Report3_Software%20Requirement%20Specification/image28.jpg)

***Figure 7. Use Case Diagram for Booking Seat Management***

![](Report3_Software%20Requirement%20Specification/image34.jpg)

***Figure 8. Use Case Diagram for Library Access Management***

![](Report3_Software%20Requirement%20Specification/image19.jpg)

***Figure 9. Use Case Diagram for Reputation & Violation Management***

![](Report3_Software%20Requirement%20Specification/image24.jpg)

***Figure 10. Use Case Diagram for Feedback Management***

![](Report3_Software%20Requirement%20Specification/image3.jpg)

***Figure 11. Use Case Diagram for Notification Management***

![](Report3_Software%20Requirement%20Specification/image6.jpg)

***Figure 12. Use Case Diagram for News & Announcements Management***

![](Report3_Software%20Requirement%20Specification/image33.jpg)

***Figure 13. Use Case Diagram for Chat & Support Management***

![](Report3_Software%20Requirement%20Specification/image9.jpg)

***Figure 14. Use Case Diagram for Statistics & Report Management***

### 2.2.2 Descriptions

| **ID** | **Use Case** | **Actors** | **Use Case Description** |
| --- | --- | --- | --- |
| UC001 | Log in via Google Account | Admin, Student, Librarian | Allows users to log in using Google accounts |
| UC002 | Log in via SLIB Account | Student | Allows students to log in using SLIB accounts |
| UC003 | Log out | Admin, Student, Librarian | Allows users to log out of the system |
| UC004 | View profile | Admin, Librarian, Student | Allows users to view their personal profile information |
| UC005 | Change basic profile | Admin, Librarian, Student | Allows users to update basic profile information such as phone number, DOB, avatar |
| UC006 | Change password | Admin, Librarian, Student | Allows users to change their account password |
| UC007 | View Barcode | Student | Allows students to view their personal barcode |
| UC008 | View history of activities | Student | Allows students to view their activity history such as booking seat, violation |
| UC009 | View account setting | Student | Allows students to view account settings |
| UC010 | Turn on/Turn off notification | Student | Allows students to enable or disable notifications |
| UC011 | Turn on/Turn off AI suggestion | Student | Allows students to enable or disable AI suggestions |
| UC012 | Turn on/Turn off HCE feature | Student | Allows students to enable or disable the HCE feature |
| UC013 | View list of users in the system | Admin | Allows Admin to view the list of all users in the system |
| UC014 | Import Student via file | Admin | Allows Admin to import student accounts using an upload file |
| UC015 | Download template of Student upload file | Admin | Allows Admin to download a standard template file for student data upload |
| UC016 | Add Librarian to the System | Admin | Allows Admin to create and add librarian account to the system |
| UC017 | View user details | Admin | Allows Admin to view detailed user information |
| UC018 | Change user status | Admin | Allows Admin to update user account status |
| UC019 | Delete user account | Admin | Allows Admin to delete a user account from the system |
| UC020 | Create area | Admin | Allows Admin to create a new library area |
| UC021 | Update area | Admin | Allows Admin to update information of an existing area |
| UC022 | Delete area | Admin | Allows Admin to delete an area from the system |
| UC023 | View area map | Admin | Allows Admin to view the layout map of a library area |
| UC024 | View area status | Admin | Allows Admin to view the current status of an area |
| UC025 | Lock area movement | Admin | Allows Admin to lock movement within an area |
| UC026 | Create zone | Admin | Allows Admin to create a new zone within a library area |
| UC027 | Update zone | Admin | Allows Admin to update information of an existing zone |
| UC028 | Delete zone | Admin | Allows Admin to delete a zone and its related configurations |
| UC029 | View zone map | Admin | Allows Admin to view the layout map of a zone |
| UC030 | Create zone attribute | Admin | Allows Admin to add attributes to a zone |
| UC031 | Update zone attribute | Admin | Allows Admin to update existing zone attributes |
| UC032 | Delete zone attribute | Admin | Allows Admin to remove attributes from a zone |
| UC033 | View zone details | Admin | Allows Admin to view detailed information of a zone |
| UC034 | Lock zone movement | Admin | Allows Admin to lock movement within a specific zone |
| UC035 | Create seat | Admin | Allows Admin to create a new seat in a specific zone |
| UC036 | Update seat | Admin | Allows Admin to update seat information |
| UC037 | Delete seat | Admin | Allows Admin to delete a seat from the library seat map |
| UC038 | View seat map | Admin | Allows Admin to view the digital seat layout of the library |
| UC039 | Change seat status | Admin | Allows Admin to change the current status of a seat |
| UC040 | Create reputation rule | Admin | Allows Admin to create a new reputation rule for user behaviour |
| UC041 | Update reputation rule | Admin | Allows Admin to update existing reputation rules |
| UC042 | Delete reputation rule | Admin | Allows Admin to delete a reputation rule from the system |
| UC043 | View list of reputation rules | Admin | Allows Admin to view all reputation rules in the system |
| UC044 | Set deducted point for each rule | Admin | Allows Admin to set deducted points for each reputation rule |
| UC045 | Set library operating hours | Admin | Allows Admin to configure library opening and closing hours |
| UC046 | Configure booking rules | Admin | Allows Admin to configure seat booking rules |
| UC047 | Set automatic backup schedule | Admin | Allows Admin to configure automatic system backup schedules |
| UC048 | Turn on/Turn off automatic check-out | Admin | Allows Admin to enable or disable automatic check-out when booking expires |
| UC049 | Enable/Disable Library | Admin | Allows Admin to enable or disable library operations |
| UC050 | View list of HCE devices | Admin | Allows Admin to view the list of registered HCE devices |
| UC051 | Create HCE device | Admin | Allows Admin to create a new HCE device |
| UC052 | Update HCE device | Admin | Allows Admin to update HCE device information |
| UC053 | Delete HCE device | Admin | Allows Admin to delete an HCE device from the system |
| UC054 | View HCE device details | Admin | Allows Admin to view detailed information of an HCE device |
| UC055 | Create material | Admin | Allows Admin to create a new material in the system |
| UC056 | View list of materials | Admin | Allows Admin to view all materials in the system |
| UC057 | Update material | Admin | Allows Admin to update material information |
| UC058 | Delete material | Admin | Allows Admin to delete a material from the system |
| UC059 | Create knowledge store | Admin | Allows Admin to create a knowledge store for system learning or AI support |
| UC060 | View list of knowledge stores | Admin | Allows Admin to view all knowledge stores in the system |
| UC061 | Update knowledge store | Admin | Allows Admin to update knowledge store information |
| UC062 | Delete knowledge store | Admin | Allows Admin to delete a knowledge store |
| UC063 | Test AI chat | Admin | Allows Admin to test AI chat functionality |
| UC064 | Create Kiosk image | Admin | Allows Admin to create an image for kiosk display |
| UC065 | View list of Kiosk images | Admin | Allows Admin to view all kiosk images |
| UC066 | Update Kiosk image | Admin | Allows Admin to update kiosk image content |
| UC067 | Delete Kiosk image | Admin | Allows Admin to delete a kiosk image |
| UC068 | Change image status | Admin | Allows Admin to change kiosk image status |
| UC069 | Preview Kiosk display | Admin | Allows Admin to preview kiosk display layout |
| UC070 | Config system notification | Admin | Allows Admin to configure system notification settings |
| UC071 | View system overview information | Admin | Allows Admin to view system overview information |
| UC072 | Backup data manually | Admin | Allows Admin to perform manual data backup |
| UC073 | View real time seat map | Librarian, Student | Allows users to view the real time seat map |
| UC074 | Filter seat map | Librarian, Student | Allows users to filter the seat map |
| UC075 | View map density | Librarian, Student | Allows users to view seat density information |
| UC076 | Booking seat | Student | Allows students to book a specific seat for a selected time slot |
| UC077 | View booking confirmation details | Student | Allows students to view booking confirmation details |
| UC078 | Confirm booking manually | Student | Allows students to confirm booking manually |
| UC079 | Confirm booking via NFC | Student | Allows students to confirm booking using |
| UC080 | View history of booking | Student | Allows Student to review their past seat reservations and usage history. |
| UC081 | Cancel booking | Student | Allows Student to cancel their reservation before the scheduled time to avoid reputation point deductions. |
| UC082 | Ask AI for recommending seat | Student | Allows Student to ask AI for seat recommendations |
| UC083 | Request seat duration | Student | Allows Student to request an extension for current seat usage if no subsequent bookings exist |
| UC084 | View list of Student bookings | Librarian | Allows Librarian to view the list of student seat bookings |
| UC085 | Search & Filter Student booking | Librarian | Allows Librarian to search and filter student bookings |
| UC086 | View Student booking details | Librarian | Allows Librarian to view detailed information of a student booking |
| UC087 | Cancel invalid booking | Librarian | Allows Librarian to cancel invalid or suspicious bookings |
| UC088 | Check-in library via HCE | Student | Allows Student to check in by tapping a smartphone using HCE at the library gate |
| UC089 | Check-out library via HCE | Student | Allows Student to check out by tapping a smartphone using HCE when leaving the library |
| UC090 | Check-in library via QR code | Student | Allows Student to check in by scanning a QR code at the library |
| UC091 | Check-out library via QR code | Student | Allows Student to check out by scanning a QR code when leaving the library |
| UC092 | View history of check-ins/check-outs | Student | Allows Student to view check-in and check-out history |
| UC093 | View list of Students access to library | Admin, Librarian | Allows Admin and Librarian to view a list of all real-time and historical student check-in/check-out records managed by the system. |
| UC094 | View reputation score | Student | Allows Student to view current reputation score |
| UC095 | View history of changed reputation score | Student | Allows Student to view reputation score change history |
| UC096 | View detailed reason of deducting points | Student | Allows Student to view detailed reasons for reputation deduction |
| UC097 | Create complaint | Student | Allows Student to submit a complaint regarding reputation deduction or system issues |
| UC098 | View history of sending complaint | Student | Allows Student to view history of submitted complaints |
| UC099 | View list of Students violation | Librarian | Allows Librarian to view the list of student violations |
| UC100 | View Student violation details | Librarian | Allows Librarian to view detailed information of a student violation |
| UC101 | View list of complaints | Librarian | Allows Librarian to view the list of student complaints |
| UC102 | View complaint details | Librarian | Allows Librarian to view detailed information of a complaint |
| UC103 | Accept/Deny complaint | Librarian | Allows Librarian to accept or deny a student complaint |
| UC104 | Create feedback after check-out | Student | Allows Student to create feedback after checking out |
| UC105 | Create seat status report | Student | Allows Student to report seat status issues |
| UC107 | View history of sending seat status report | Student | Allows Student to view history of submitted seat status reports |
| UC108 | Create report seat violation | Student | Allows Student to report seat violations |
| UC109 | View history of sending report seat violation | Student | Allows Student to view history of submitted seat violation reports |
| UC110 | View list of seat status reports | Admin, Librarian | Allows Admin and Librarian to view the list of seat status reports |
| UC111 | View seat status report details | Admin, Librarian | Allows Admin and Librarian to view detailed information of a seat status report |
| UC112 | View list of seat violation reports | Admin, Librarian | Allows Admin and Librarian to view the list of seat violation reports |
| UC113 | View report seat violation details | Admin, Librarian | Allows Admin and Librarian to view detailed information of a seat violation report |
| UC114 | View list of feedbacks | Librarian | Allows Librarian to view the list of student feedbacks |
| UC115 | View feedback details | Librarian | Allows Librarian to view detailed feedback content |
| UC116 | Categorize feedback via AI | Librarian | Allows Librarian to categorize feedback using AI |
| UC117 | Verify seat status report | Librarian | Allows Librarian to verify seat status reports submitted by students |
| UC118 | Verify seat violation report | Librarian | Allows Librarian to verify seat violation reports submitted by students |
| UC119 | View list of notifications | Librarian, Student | Allows Librarian and Student to view the list of system notifications |
| UC120 | Delete notification | Librarian, Student | Allows Librarian and Student to delete notifications |
| UC121 | View notification details | Librarian, Student | Allows Librarian and Student to view detailed notification content |
| UC122 | Mark notification as read | Librarian, Student | Allows Librarian and Student to mark notifications as read |
| UC123 | Create new book | Librarian | Allows Librarian to create a new book announcement with basic information |
| UC124 | Update new book | Librarian | Allows Librarian to update information of a new book announcement |
| UC125 | Delete new book | Librarian | Allows Librarian to delete a new book announcement |
| UC126 | Create news & announcement | Librarian | Allows Librarian to create news and announcements |
| UC127 | Update news & announcement | Librarian | Allows Librarian to update news and announcements |
| UC128 | Delete news & announcement | Librarian | Allows Librarian to delete news and announcements |
| UC129 | View list of news & announcement categories | Librarian | Allows Librarian to view news and announcement categories |
| UC130 | Create news & announcement category | Librarian | Allows Librarian to create a news and announcement category |
| UC131 | Update news & announcement category | Librarian | Allows Librarian to update a news and announcement category |
| UC132 | Delete news & announcement category | Librarian | Allows Librarian to delete a news and announcement category |
| UC133 | Set time to post news & announcement | Librarian | Allows Librarian to schedule news and announcements |
| UC134 | Save news & announcement draft | Librarian | Allows Librarian to save news and announcements as drafts |
| UC135 | View list of news & announcements | Librarian, Student | Allows users to view the list of news and announcements |
| UC136 | View news & announcement details | Librarian, Student | Allows users to view detailed news and announcement content |
| UC137 | View list of new books | Librarian, Student | Allows Librarian and Student to view the list of newly added books |
| UC138 | View basic information of new book | Librarian, Student | Allows Librarian and Student to view basic information of a new book |
| UC139 | Chat with AI virtual assistant | Student | Allows Student to chat with an AI virtual assistant |
| UC140 | View history of chat | Student | Allows Student to view chat history with the AI assistant |
| UC141 | Chat with Librarian | Student | Allows Student to chat with a Librarian |
| UC142 | View list of chats | Librarian | Allows Librarian to view the list of student chats |
| UC143 | View chat details | Librarian | Allows Librarian to view detailed chat conversations |
| UC144 | Response to Student manually | Librarian | Allows Librarian to respond to students manually |
| UC145 | Response to Student with AI suggestion | Librarian | Allows Librarian to respond to students using AI suggestions |
| UC146 | View general analytics dashboard | Librarian | Allows Librarian to view general analytics data |
| UC147 | View violation statistics | Librarian | Allows Librarian to view statistics of student violations |
| UC148 | View statistics of density forecast via AI | Librarian | Allows Librarian to view AI-based density forecast statistics |
| UC149 | View check-in/check-out statistics | Librarian | Allows Librarian to view check-in and check-out statistics |
| UC150 | View seat booking statistics | Librarian | Allows Librarian to view seat booking statistics |
| UC151 | View statistics of analyzing feedback | Librarian | Allows Librarian to view feedback analysis statistics |
| UC152 | Export general analytical report | Librarian | Allows Librarian to export general analytics reports |
| UC153 | Export feedback summary report via AI | Librarian | Allows Librarian to export AI-generated feedback summary reports |
| UC154 | Export seat & maintenance report | Librarian | Allows Librarian to export seat usage and maintenance reports |

***Table 2. Use Case Descriptions***

## 3. Functional Requirements

### 3.1 System Functional Overview

### 3.1.1 Screens Flow

![](Report3_Software%20Requirement%20Specification/image29.png)

***Figure 15. Screen Flow Diagram of Librarian (Website)***

![](Report3_Software%20Requirement%20Specification/image15.png)

***Figure 16. Screen Flow Diagram of Student (Mobile)***

![](Report3_Software%20Requirement%20Specification/image31.png)

***Figure 17. Screen Flow Diagram of Admin (Website)***

### 3.1.2 Screen Descriptions

| **#** | **Feature** | **Screen** | **Description** |
| --- | --- | --- | --- |
| 1 | Authentication Module | Login Page | Allows Librarian to log in to the system through school Google account. |
| 2 | Authentication Module | Dashboard | Main screen after login, displaying shortcuts to modules and logout function. |
| 3 | Account Management Module | Profile & Settings | Personal information management including viewing profile, updating basic information and changing password. |
| 4 | User Management Module | User List | View the list of users in the system, import students from file, download template file and add new librarian. |
| 5 | User Management Module | User Details | View detailed user information, change account status and delete account. |
| 6 | System Configuration Module | Area Management | Infrastructure management including viewing map, CRUD area, changing status and locking movement. |
| 7 | System Configuration Module | Zone Management | Zone management including viewing zone map, CRUD zone, managing attributes, viewing details and locking movement. |
| 8 | System Configuration Module | Seat Management | Seat management including viewing seat map, CRUD seat and changing seat status. |
| 9 | System Configuration Module | Reputation Rule Management | Reputation rule management including viewing list, CRUD rules and setting deducted points. |
| 10 | System Configuration Module | Library Configuration | Set operating hours, configure booking rules, automatic check-out and enable/disable library. |
| 11 | System Configuration Module | Hardware & AI Config | Manage HCE device list, manage materials and AI knowledge store and test AI chat. |
| 12 | System Configuration Module | Kiosk & Others | Manage Kiosk images and status, configure system notifications, view logs and manage data backups. |
| 13 | Booking Seat Module | Student Booking List | View student booking list, perform search/filter, view details and cancel invalid bookings. |
| 14 | Booking Seat Module | Monitoring | Monitor real-time map, filter map and view density map. |
| 15 | Library Access Module | Access Management | Monitor the list of students currently accessing the library and check-in/out history. |
| 16 | Reputation & Violation Module | Violation & Complaints | Monitor the list of student violations, manage complaints, view details and verify complaints. |
| 17 | Feedback Module | Feedback & Reports | Manage feedback, seat status reports and seat violation reports. |
| 18 | Notification Module | Notifications | Manage notification list, view details, filter and mark as read. |
| 19 | News & Announcement Module | Content Management | Manage news and announcements, manage new books and configure posting schedule. |
| 20 | Chat & Support Module | Communication | Manage chat list, conversation details, manual response and response with AI suggestions. |
| 21 | Statistics & Report Module | Analytics Dashboard | View general analytics dashboard, violation statistics, density forecast, check-in/out statistics and booking statistics. |
| 22 | Statistics & Report Module | Export Management | Export maintenance reports, general analytical reports and AI-enhanced feedback summary reports. |
| 23 | Authentication Module | Authentication Module | Provides students with options to sign in using school Google accounts or SLIB internal credentials. |
| 24 | Bottom Navigation | Home | The main landing page providing quick access to news, new book arrivals, and system notifications. |
| 25 | Home & Content | News List | Displays a chronological list of library news, updates, and general announcements. |
| 26 | Home & Content | News Details | Shows the full content of a selected news item or announcement. |
| 27 | Home & Content | New Arrivals | Displays information about the latest books added to the library collection. |
| 28 | Booking Seat Module | Real-time Map | A digital representation of library floors showing current seat occupancy and availability. |
| 29 | Booking Seat Module | Map Filter | Allows students to filter seats by specific criteria such as availability, room type, or available utilities. |
| 30 | Booking Seat Module | Density Map | Provides a visual heat map showing library crowd levels and seat concentration. |
| 31 | Booking Seat Module | AI Seat Recommendation | Provides personalized seat suggestions based on student habits and current library status. |
| 32 | Booking Seat Module | Seat Details | Shows detailed information about a selected seat including its specific location and features. |
| 33 | Booking Seat Module | Booking Confirmation | Displays the summary of a successful reservation before the student proceeds to check-in. |
| 34 | Library Access Module | Check-in/Check-out | Entry point for students to verify their presence in the library at the gate. |
| 35 | Library Access Module | HCE Authentication | Enables contactless check-in/out using smartphone NFC technology. |
| 36 | Library Access Module | QR Code Authentication | Enables check-in/out by scanning a dynamic QR code provided at the library entrance. |
| 37 | Account Management – Card | Personal Barcode | Displays the digital student ID card used for traditional scanning or identification. |
| 38 | Account Management – Card | Expand Barcode | Shows an enlarged version of the student barcode for better readability by scanners. |
| 39 | Chat & Support Module | Chat with AI Assistant | Direct messaging interface for students to receive immediate answers from a virtual bot. |
| 40 | Chat & Support Module | Chat with Librarian | Direct messaging interface to connect students with library staff for specific support. |
| 41 | Chat & Support Module | Chat History | Allows students to review previous conversations with library staff or the AI assistant. |
| 42 | Account & Settings | Account & Settings Menu | The central menu for managing profile, viewing history, and adjusting application settings. |
| 43 | Account & Settings | Student Profile | Allows students to view and update their personal profile information and avatar. |
| 44 | Account & Settings | Booking History | Provides a complete record of all past seat reservations and usage. |
| 45 | Account & Settings | Violation History | Displays a log of any library policy violations associated with the student's account. |
| 46 | Account & Settings | Reputation Points | Shows the current reputation score and a detailed history of point changes. |
| 47 | Account & Settings | App Settings | Allows configuration of app-wide settings such as notification toggles and AI suggestions. |
| 48 | Account & Settings | Logout | Terminates the current session and signs the student out of the application. |
| 49 | Notification Module | Notification List | Displays all system alerts, booking reminders, and personal messages. |
| 50 | Notification Module | Notification Details | Shows the full details and timestamps of a specific notification message. |
| 51 | Feedback & Seat Reports | Post-Check-out Feedback | A form for students to rate their library experience after their booking session ends. |
| 52 | Feedback & Seat Reports | Reputation Complaint Form | Allows students to submit a complaint regarding reputation point deductions. |
| 53 | Feedback & Seat Reports | Seat Status Report | Allows students to report physical issues or maintenance needs for library seats. |
| 54 | Feedback & Seat Reports | Seat Violation Report | Allows students to report policy violations such as people leaving items on seats or making noise. |
| 55 | Authentication Module | Login Page | Shared web login portal for administrative access using Google accounts. |
| 56 | Authentication Module | Admin Dashboard | High-level system overview showing total active users, occupancy rates, and critical system alerts. |
| 57 | User Management Module | User Management Hub | Comprehensive tool to view all users, import student data via files, and add new Librarian accounts. |
| 58 | User Management Module | Account Authorization | Screen to manage account statuses, assign roles, and delete accounts when necessary. |
| 59 | System Configuration Module | Infrastructure Canvas | Interactive workspace for creating, updating, and designing the physical library map (Area/Zone/Seat). |
| 60 | System Configuration Module | Object Properties | Detailed configuration screen for zones and seats, including attribute management and movement locking. |
| 61 | System Configuration Module | Policy & Rules Setup | Interface to define reputation point deduction rules and general library operating hours. |
| 62 | System Configuration Module | System Logic Config | Screen for configuring automatic check-out behaviors, booking rules, and library-wide enable/disable toggles. |
| 63 | Hardware & AI Config | Device & Knowledge Hub | Management of HCE hardware IDs, AI training materials, and knowledge base testing environments. |
| 64 | Kiosk Management | Kiosk Control Center | View and manage Kiosk homepage content, check-in QR display, and promotional image carousels. |
| 65 | System Maintenance | System Overview & Logs | Displays system-wide logs for audit purposes and overall technical health metrics. |
| 66 | Data Management | Backup & Recovery | Controls for manual data backups and setting up automated cloud storage schedules. |

***Table 3. Screen Descriptions***

### 3.1.3 Screen Authorization

| **Screen** | **Admin** | **Librarian** | **Student** |
| --- | --- | --- | --- |
| Login Page | X | X |  |
| Login Screen |  |  | X |
| Log in via Google Account | X | X | X |
| Log in via SLIB Account |  |  | X |
| Admin Dashboard | X |  |  |
| Dashboard (Librarian) |  | X |  |
| Home (Student) |  |  | X |
| Logout | X | X | X |
| Profile & Settings |  | X |  |
| Student Profile |  |  | X |
| Account & Settings Menu |  |  | X |
| Personal Barcode / Expand Barcode |  |  | X |
| Query Own Identification Data |  |  | X |
| User Management Hub | X |  |  |
| Query All User Data | X |  |  |
| Import student data via files | X |  |  |
| Add new Librarian accounts | X |  |  |
| Account Authorization | X |  |  |
| Update User Status (Lock/Unlock) | X |  |  |
| Delete account | X |  |  |
| User List | X |  |  |
| Query Managed User Data | X |  |  |
| User Details | X |  |  |
| Infrastructure Canvas | X |  |  |
| Create/Update/Delete Layout (Area/Zone/Seat) | X |  |  |
| Object Properties | X |  |  |
| Manage Attribute & Lock Movement | X |  |  |
| Area Management | X |  |  |
| Zone Management | X |  |  |
| Seat Management | X |  |  |
| Change Area/Zone/Seat Status | X |  |  |
| Policy & Rules Setup | X |  |  |
| System Logic Config | X |  |  |
| Device & Knowledge Hub | X |  |  |
| Manage HCE IDs & AI Knowledge Store | X |  |  |
| Kiosk Control Center | X |  |  |
| Real-time Map / Monitoring | X | X | X |
| View Occupancy Status | X | X | X |
| Map Filter / Density Map |  | X | X |
| AI Seat Recommendation |  |  | X |
| Seat Details |  |  | X |
| Booking Confirmation |  |  | X |
| Create New Booking |  |  | X |
| Student Booking List |  | X |  |
| Query Managed Booking Data |  | X |  |
| Cancel invalid booking |  | X |  |
| Booking History |  |  | X |
| Check-in/Check-out |  |  | X |
| HCE / QR Authentication |  |  | X |
| Access Management | X | X |  |
| Violation & Complaints |  | X |  |
| Violation History |  |  | X |
| Reputation Points |  |  | X |
| Query Own Reputation Details |  |  | X |
| Reputation Complaint Form |  |  | X |
| Verify Complaints (Accept/Deny) |  | X |  |
| Post-Check-out Feedback |  |  | X |
| Seat Status/Violation Report |  |  | X |
| Feedback & Reports / Report Detail | X | X |  |
| Verify Seat Status Reports |  | X |  |
| Notifications / List / Detail | X | X | X |
| CRUD News/Books & Posting Schedule |  | X |  |
| New Arrivals |  |  | X |
| Communication |  | X |  |
| Chat with AI Assistant |  |  | X |
| Chat with Librarian |  |  | X |
| Response with AI suggestions |  | X |  |
| Chat History |  |  | X |
| Analytics Dashboard | X | X |  |
| Export Management |  | X |  |
| System Overview & Logs | X |  |  |
| Backup & Recovery | X |  |  |

***Table 4. Screen Authorization***

### 3.1.4 Non-Screen Functions

| **#** | **Feature** | **System Function** | **Description** |
| --- | --- | --- | --- |
| 1 | Library Configuration Management | Automatic Check-out Service | A cron job that runs periodically to identify expired seat bookings and automatically trigger the check-out process or status update. |
| 2 | Data Management | Automated Backup Job | A scheduled background process that performs system data backups to cloud storage based on the frequency configured by the Admin. |
| 3 | Feedback System Management | AI Sentiment Analysis | A backend service using the Gemini API to automatically categorize and analyze the sentiment of student feedback. |
| 4 | Booking Seat Module | AI Recommendation Engine | An API-driven service that analyzes real-time seat density and user preferences to provide personalized seat suggestions. |
| 5 | Notification Module | Notification Dispatcher | A centralized background service that triggers system alerts, booking reminders, and violation notices to user devices. |
| 6 | Authentication Module | Google OAuth API | Integration service for secure user authentication and token management with Google's identity provider. |

***Table 5. Non-Screen Functions***

### 3.1.5 Entity Relationship Diagram

![](Report3_Software%20Requirement%20Specification/image11.jpg)

***Figure 18. Entity Relationship Diagram***

**Entities Description**

| **#** | **Entity** | **Description** |
| --- | --- | --- |
| 1 | User | Central entity representing all system users (Students, Librarians, Admins) for authentication and role management. |
| 2 | Profile | Stores detailed personal information associated with a User, such as full name and contact details. |
| 3 | Token | Manages session or authentication tokens to handle secure user sessions. |
| 4 | Setting | Stores individual user preferences and application-wide configurations for personalizing the experience. |
| 5 | Schedule | Represents the calendar or time-based availability for users or library operational planning. |
| 6 | Activity Log | Records a historical trail of user actions within the system for auditing and activity tracking. |
| 7 | Point Transaction | Records specific changes to a student's reputation score, documenting point deductions or additions. |
| 8 | Reputation Rule | Defines the business logic and criteria used to determine point deductions for library policy violations. |
| 9 | Reservation | Stores details of a seat booking, including the associated User, Seat, and time duration. |
| 10 | Access Log | Records the physical check-in and check-out timestamps at library gates, linked to specific reservations. |
| 11 | Feedback | Stores user ratings and textual reviews submitted after a booking session is completed. |
| 12 | Notification | Stores system-generated alert messages sent to users regarding bookings, news, or violations. |
| 13 | Complaint | Stores formal grievance reports submitted by students to challenge reputation point deductions. |
| 14 | News | Stores informational content, library updates, and announcements published by staff. |
| 15 | Category | Used to organize News and Announcement items into logical groups for better discoverability. |
| 16 | New Book | Stores metadata (title, author, etc.) for recently acquired library materials to be promoted. |
| 17 | Area | Represents large logical sections of the library, such as floors or specific wings. |
| 18 | Zone | Represents a specific cluster or group of seats within an Area for more granular layout management. |
| 19 | Seat | The smallest unit of the layout representing individual desks or chairs available for booking. |
| 20 | HCE Device | Represents physical hardware at library gates used for NFC-based authentication and access control. |
| 21 | Amenity | Stores features or utilities available in specific Zones, such as "Power Sockets" or "Strong WiFi". |
| 22 | Factory | Represents physical layout elements, obstacles, or fixed library furniture within an Area. |
| 23 | Conversation | Represents a dedicated support thread between a Student and a Librarian for assistance. |
| 24 | Message | An individual text entry or reply within a specific support Conversation. |
| 25 | AI Chat | Represents a distinct session between a Student and the AI Virtual Assistant. |
| 26 | AI Message | An individual prompt or response within an AI Chat session. |

### 3.2 Authentication

### 3.2.1 Log in via Google Account

**Function trigger:**

- **Navigation path:**
    - **Web:** Browser -> Access System URL -> Login Page
    - **Mobile**: App Launch -> Not Auth Screen -> Login Screen
- **Timing frequency:** On demand (Whenever a user needs to access the system or the session expires).

**Function description:**

- **Actors/Roles:**
    - **Web:** Admin, Librarian
    - **Mobile:** Student
- **Purpose:** To verify user identity and grant access to the system's specific features based on assigned roles and platforms.
- **Interface:**
    - **Web Login Screen (For Admin/Librarian):**
        1. **“Đăng nhập với Google”:** A button used to redirect to Google login page.
    - **Mobile Login Screen (For Student):**
        1. **“Tiếp tục với Google”:** A button used to redirect to Google login page.
- **Data processing:**
    - **Web Login**
        - System displays the Web Login portal for Admin/Librarian.
        - The user taps on the "Đăng nhập với Google" button.
        - System redirects to Google’s authentication service.
        - Upon successful auth, system verifies if the email exists in the staff database.
        - If success, the system redirects to the Librarian or Admin Dashboard.
        - If failure, the system redirects back to the Login Screen.
    - **Mobile Login**
        - System displays the Mobile Login screen for Students.
        - User taps "Tiếp tục với Google".
        - System opens the Google Sign-In overlay.
        - User selects their @fpt.edu.vn account
        - System verifies the identity and checks the student's information in the database.
        - If the student is from K18 onwards and account is active, system redirects to the Student Home.
        - If failure, the system back to the Login Screen and displays an error message.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image22.png)

***Figure 19. Login via Google Account Screen Layout (For Web)***

![](Report3_Software%20Requirement%20Specification/image18.jpg)

***Figure 20. Login via Google Account Screen Layout (For Mobile)***

![](Report3_Software%20Requirement%20Specification/image4.jpg)

***Figure 21. Not Auth Screen Layout***

**Function details:**

- **Data:** The function retrieves and validates Google account details, including: Identity Token, School Email (@fpt.edu.vn), Student Name, and Student Cohort.
- **Validation:**
    - **Role-Platform Mapping:** System ensures Staff only log in via Web and Students only via Mobile.
    - **Cohort Check:** System validates that the student belongs to K18 or before generations.
    - **Status Check:** Account must be "Active"
- **Business rules:** N/A
- **Normal case:** Valid authorized account (Staff on Web or K18+ Student on Mobile) then the system redirects to the respective dashboard.
- **Abnormal case:** A user logs in with an account that doesn't have staff permissions then the system redirects to the “Login Screen” page.

### 3.2.2 Log in via SLIB Account

**Function trigger:**

- **Navigation path:** App Launch -> Login Screen
- **Timing frequency:** On demand (Whenever a student needs to access the system using internal credentials).

**Function description:**

- **Actors/Roles:** Student
- **Purpose:** To allow students to log in to the mobile application using their internal SLIB credentials (Email/Student ID and Password).
- **Interface:**
    - **Log in via SLIB Account Screen:**
        1. **“Email FPT hoặc MSSV”:** Used to enter the registered university email or Student ID.
        2. **“Mật khẩu”:** Used to enter the account password (includes a show/hide toggle icon).
        3. **“Ghi nhớ đăng nhập” checkbox:** Used to keep the session active for future launches.
        4. **“Đăng nhập” button:** The primary action button used to submit credentials for verification.
- **Data processing:**
    - The student enters their “Email/MSSV” and “Mật khẩu”.
    - The student may select "Ghi nhớ đăng nhập" to store the session locally.
    - Upon clicking "Đăng nhập", the system validates that both fields are not empty.
    - The system encodes the input password to compare it with the stored value in the database.
    - The system verifies if the student belongs to the K18 cohort or later.
    - If the credentials match and the account is active, redirects the student to the Student Home/Seat Map.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image37.jpg)

***Figure 22. Log in via SLIB Account Screen Layout***

**Function details:**

- **Data:** The function processes and validates user identification details, including: FPT Email or Student ID (MSSV), Hashed Password, Cohort Information, and the "Remember Me" session preference.
- **Validation:**
    - Both Email/MSSV and Password fields are mandatory.
    - Only students from the K18 cohort onwards are permitted to use this login method.
- **Business rules:** N/A
- **Normal case:** The student provides valid credentials and belongs to an authorized cohort; the system authenticates successfully and redirects to the home screen.
- **Abnormal case:** In the event of a system failure, the system will display a specific notification message explaining the failure and will clear the password field to allow the user to try again.

### 3.2.3 Log out

**Function trigger:**

- **Navigation path:**
    - **Web:** Header -> Profile Dropdown -> “Đăng xuất” button.
    - **Mobile**: Bottom Nav -> Tab “Thêm” -> “Đăng xuất button.
- **Timing frequency:** On demand (Whenever a user needs to terminate their session or switch accounts).

**Function description:**

- **Actors/Roles:**
    - **Web:** Admin, Librarian
    - **Mobile:** Student
- **Purpose:** To securely terminate the current authenticated session and return to the login portal.
- **Interface:**
    - **Web Login Screen (For Admin/Librarian):**
        1. **Profile Dropdown:** The user identity section in the top right corner.
        2. **“Đăng xuất”:** An option within the profile menu used to sign out.
    - **Mobile Login Screen (For Student):**
        1. **“Thêm”:** The last tab icon in the bottom navigation bar used to access account settings.
        2. **“Đăng xuất” button:** A red button within the "Thêm" screen used to trigger the logout.
- **Data processing:**
    - User clicks "Đăng xuất".
    - System identifies and invalidates the active session/token.
    - System clears all local session data and cached user information.
    - System redirects the user to the starting “Not Auth Screen” (for Mobile) / “Login Screen” (for Web) of the respective platform.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image16.png)

***Figure 23. Login out Screen Layout (For Web)***

![](Report3_Software%20Requirement%20Specification/image5.jpg)

***Figure 24. Login out Screen Layout (For Mobile)***

**Function details:**

- **Data:** The function identifies the active Session Token and processes the termination of the authenticated state for the specific User ID.
- **Validation:**
    - Verify that a valid session exists before executing the logout process.
- **Business rules:** N/A
- **Normal case:** Session is invalidated smoothly and the user is returned to the login screen within 2 seconds.
- **Abnormal case:** In the event of a network failure or server-side error during invalidation, the system will force-clear local storage data to ensure the user is effectively signed out of the current device and redirect them to the Login Screen with a notification message.

### 3.3 Account Management

### 3.3.1 View profile

### 3.3.2 Change basic profile

### 3.3.3. Change password

### 3.3.4. View Barcode

**Function trigger:**

- **Navigation path:** Student Home -> Bottom Navigation Bar -> "Card" tab.
- **Timing frequency:** On demand (Whenever a student needs to show their ID card or scan the barcode for library services).

**Function description:**

- **Actors/Roles:** Student
- **Purpose:** To display the student's digital identification card, including their personal information and a scan-able barcode for physical library interactions.
- **Interface:**
    - **View Barcode Screen:**
        1. **"Mở rộng mã Barcode":** Used to enlarge the barcode for easier scanning.
- **Data processing:**
    - The user taps on the "Card" icon in the bottom navigation bar.
    - The system retrieves the authenticated student's profile information from the database or local session.
    - The system generates the barcode image using the unique Student ID.
    - The system renders the student's ID Card with all retrieved details on the screen.
    - If the user taps "Mở rộng mã Barcode", the system displays an overlay with an enlarged version of the barcode.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image21.jpg)

***Figure 25. View Barcode Screen Layout***

**Function details:**

- **Data:** The function retrieves and displays student identification details, including: Full Name, Student ID, Avatar image, and a digital Barcode generated from the Student ID.
- **Validation:**
    - The system must ensure the user is logged in to access this feature.
    - The barcode must accurately represent the Student ID stored in the system.
- **Business rules:** N/A
- **Normal case:** The system retrieves data successfully and displays the student card with a clear, scan-able barcode.
- **Abnormal case:** Barcode generation fails => Displays a placeholder or error icon in the barcode area.

### 3.4 User Management Module

### 3.4.1 View list of users in the system

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> User Management Icon (3-person icon).
- **Timing frequency:** On demand (Whenever an Admin needs to monitor, filter, or manage accounts within the system).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To provide a comprehensive overview of all registered users in the system, including Administrators, Librarians, and Students, allowing for efficient monitoring and management.
- **Interface:**
    - **View list of users in the system Screen:**
        1. **User List Table:** Displays records of users with columns: “Người dùng”, “Email”, “Vai trò”, “Trạng thái”, “Hoạt động gần nhất”, and “Thao tác”.
        2. **Search Bar:** Input field "Tìm theo tên, email..." used to find specific users.
        3. **Role Filter:** Dropdown menu "Tất cả vai trò" to filter the list by Admin, Librarian, or Student.
        4. **Status Filter:** Dropdown menu “Tất cả trạng thái” to filter the list by “Hoạt động” or “Đã khóa”.
        5. **Permission Button:** “Phân quyền” button for role management.
- **Data processing:**
    - The Admin navigates to the User Management section from the side menu.
    - The system retrieves all user profiles from the database.
    - The system calculates and updates the counts in the “statistic cards” based on current database records.
    - The system renders the “User List Table” with the retrieved data.
    - If the Admin inputs text into the “Search Bar”, the system filters the table in real-time by Name or Email.
    - If the Admin selects a value from the “Role Filter” or “Status Filter”, the system updates the table view to match the selected criteria.
    - The system displays the "Last Activity" time for each user based on their last successful session or interaction timestamp.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image27.png)

***Figure 26. View list of users in the system Screen Layout***

**Function details:**

- **Data:** The function retrieves and displays user account details, including: Full Name, Email address, Student ID (for students - displayed under the name), Account Role (Admin/Librarian/Student), Account Status (Active/Locked), and Last Activity timestamp. It may also include additional information based on applied filtering and sorting options.
- **Validation:**
    - The system must ensure that only users with the Admin role can access this screen.
- **Business rules:** N/A
- **Normal case:** The system displays the full user list and statistics accurately; filters and search function correctly to narrow down results.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining the issue

### 3.4.2 Import Student via file and download template

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> User Management Screen -> Click "Import CSV" button.
- **Timing frequency:** When the Admin needs to add multiple student accounts simultaneously.

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to batch-create student accounts by uploading a structured data file (Excel/CSV), saving time compared to manual entry.
- **Interface:**
    - **View list of users in the system Screen:**
        1. **“Import CSV” button:** A button located on the main user management dashboard used to open the import modal.
    - **Import Student via file Screen:**
        1. **“Kéo thả file CSV/Excel vào đây”:** A designated drop zone area for users to drag and drop their data files for uploading.
        2. **“Tải template mẫu (.xlsx)”:** A hyperlink that allows the Admin to download the standardized Excel template to ensure data compatibility.
        3. **“Chọn file”:** A primary action button used to open the local file explorer to manually select a file from the device.
- **Data processing:**
    - The Admin clicks the "Import CSV" button; the system displays the "Import danh sách sinh viên" modal.
    - The Admin may click the “Template link” to review the required columns and format.
    - The Admin selects or drops a file into the “Upload Area” or via the "Chọn file" button.
    - The system parses the uploaded file and validates the structure and content of each row against system constraints.
    - For each valid record, the system creates a new Student account with a default password and sets the status to "Active".

**Screen layout**

![](Report3_Software%20Requirement%20Specification/image36.png)

> ***Figure 27. Import Student via file Screen Layout***
> 

**Function details:**

- **Data:** The function processes and imports student identification details from the file, including: Full Name, Student ID, School Email, and potentially additional academic data such as Major or Cohort information.
- **Validation:**
    - Verify that the uploaded file is strictly in .csv or .xlsx format.
    - Ensure that the Student ID and Email do not already exist in the system's database to prevent duplicates.
- **Business rules:** N/A
- **Normal case:** The Admin uploads a valid file containing correctly formatted student data; the system processes all records successfully, creates the accounts, and updates the user management list.
- **Abnormal case:** In the event of a system failure, the system will display a comprehensive notification message summarizing the errors encountered and will halt the import of invalid records to maintain data integrity.

### 3.4.3 Add Librarian to the system

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> User Management Screen -> Click "Thêm Thủ thư" button.
- **Timing frequency:** When the Admin needs to manually create a single Librarian account..

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to manually add a new Librarian to the system by providing their essential information and credentials.
- **Interface:**
    - **View list of users in the system Screen:**
        1. **“Thêm thủ thư” button:** The primary action button on the User Management dashboard used to initiate the account creation process
    - **Add Librarian to the system Screen:**
        1. **“Họ và tên”:** An input field used to enter the full name of the new librarian.
        2. **“Email”:** An input field used to enter the university email address (@fpt.edu.vn) for the account.
        3. **“Mật khẩu tạm thời”:** A secure input field used to set an initial password for the librarian.
        4. **“Tạo tài khoản”:** The submission button used to save the information and create the account in the database.
        5. **“Hủy”:** A button used to close the modal and discard any unsaved changes.
- **Data processing:**
    - The Admin clicks the "Thêm Thủ thư" button to open the creation form.
    - The Admin fills in the required fields: “Họ và tên”, “Email”, and “Mật khẩu tạm thời”.
    - Upon clicking "Tạo tài khoản", the system validates that no fields are empty and the email domain is correct.
    - The system checks the database to ensure the email is not already registered.
    - The system encodes the provided temporary password.
    - The system saves the new record with the "Librarian" role and default "Active" status.
    - The system closes the modal and displays a notification confirming the successful creation of the account.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image17.png)

***Figure 28. Add Librarian to the system Screen Layout***

**Function details**

- **Data:** The function processes and manages librarian information, including: Full Name, University Email address, and Temporary Password. It assigns the 'Librarian' role and default 'Active' status to the new user record.
- **Validation:**
    - All input fields (Name, Email, Password) are mandatory and cannot be left blank.
    - The Email must strictly follow the @fpt.edu.vn domain format.
    - The Email address must be unique within the system's database
- **Business rules:** N/A
- **Normal case:** The Admin enters valid and unique information for the new librarian; the system creates the account successfully and updates the user list.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining the specific issue and will prevent the account from being created to ensure data consistency and security.

### 3.4.4 View user details

### 3.4.5 Change user status

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> User Management Screen -> Click "Three-dot" icon.
- **Timing frequency:** On demand (When the Admin needs to suspend a user's access).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to change a user's status to "Locked" by providing a specific reason, ensuring management transparency and system security.
- **Interface:**
    - **Change user status Screen:**
        1. **“Lý do khóa tài khoản”:** A text area input field where the Admin must enter the reason for suspending the account.
        2. **“Khóa tài khoản”:** The primary action button (orange) used to confirm and apply the "Locked" status.
        3. **“Hủy”:** A secondary button used to close the modal and cancel the operation.
- **Data processing:**
    - The Admin clicks the "Three-dot" icon for a specific user and selects "Khóa tài khoản".
    - The system displays a confirmation modal showing the user's name and an input field for the reason.
    - The Admin enters the reason in the "Lý do khóa tài khoản" field.
    - Upon clicking "Khóa tài khoản", the system validates that the reason field is not empty.
    - The system updates the user's status to "Locked" and saves the reason into the database.
    - The system system terminates any active sessions for that user.
    - The system closes the modal, refreshes the user list, and displays a success notification.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image30.png)

***Figure 29. Change user status Screen Layout***

**Function details:**

- **Data:** The function updates the Account Status field (from “Hoạt động” to “Đã khóa”) and records the Reason for Lock in the database for the selected user.
- **Validation:**
    - The "Lý do khóa tài khoản" field is mandatory and must contain text.
    - The Admin is prohibited from locking their own account to prevent system lockout.
- **Business rules:** N/A
- **Normal case:** The Admin provides a valid reason and confirms the action; the system updates the status successfully, and the user table reflects the change immediately.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining the failure and will maintain the user's current status to ensure data integrity.

### 3.4.5 Delete user account

### 3.5 System Configuration

### 3.5.1 Area Management

### *3.5.1.1 View area map*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện).
- **Timing frequency:** On demand (Whenever a staff member needs to design, monitor, or update the physical layout of the library).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To provide a visual representation of the library's layout, including rooms, seating areas, and obstacles, allowing staff to manage the space effectively.
- **Interface:**
    - **View area map Screen:**
        1. **“Xem trước”:** A toggle button used to switch to the student-view perspective to verify how the layout appears on the mobile app.
        2. **Area List Sidebar:** A panel listing all created rooms ("Phòng thư viện") and specific seating zones ("Khu vực ghế").
        3. **Map Canvas:** The central workspace that visually displays the layout, including room boundaries, obstacles ("Vật cản"), and seat arrangements (e.g., A1, A2... A6).
        4. **“Tên phòng thư viện”:** An input/display field for the name of the currently selected room.
        5. **“Trạng thái”:** Status toggle buttons ("Mở khóa" and "Hoạt động") used to set the availability of the room.
        6. **“Thống kê”:** Summary cards displaying the total number of areas ("Khu vực") and total seats ("Tổng ghế") within the selected room.
- **Data processing:**
    - The Admin/Librarian navigates to the Map section; the system fetches the latest layout configuration from the database.
    - The system calculates the statistics for seats and areas based on the current room's data.
    - The system renders all elements (obstacles, seats, zones) onto the “Map Canvas” according to their saved coordinates and dimensions.
    - If the user clicks "Xem trước", the system hides the editing tools and displays the map in a read-only format that mimics the student's mobile interface.
    - If the user selects a different room from the sidebar, the system updates the canvas and statistics to reflect the new selection.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image10.png)

***Figure 30. View area map Screen Layout***

**Function details:**

- **Data:** The function retrieves and displays area configuration details, including: Room Name, Room Status (Unlocked/Active), list of seating areas, individual Seat IDs (A1-A6), total count of areas and seats, and the spatial coordinates for obstacles and furniture
- **Validation:**
    - Access restricted to Admin role.
    - Seat IDs must be unique within a single room layout to avoid booking conflicts.
- **Business rules:** N/A
- **Normal case:** The system successfully loads the library layout, renders all seats and obstacles in their correct positions, and updates the statistics cards accurately.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining that the map data could not be synchronized and will prevent any modifications to ensure the current configuration remains stable.

### *3.5.1.2 Create and update area*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Click “Chỉnh sửa” button.
- **Timing frequency:** On demand (Whenever the Admin needs to add new rooms/zones or modify existing layouts and descriptions).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to create new library areas or update existing ones by managing their names, descriptions, utility tags, and physical placement on the map.
- **Interface:**
    - **Create and update area Screen:**
        1. **“Chỉnh sửa”:** A toggle button to enter/exit the design mode.
        2. **Tool Panel (+Phòng):** Action buttons used to add new area directly onto the map canvas.
        3. **“TÊN PHÒNG THƯ VIỆN”:** An input field used to set or update the name of the selected area or room.
        4. **“TRẠNG THÁI”:** A set of toggle buttons ("Mở khóa" and "Hoạt động") used to define the operational status of the room or area.
        5. **Map Canvas Area:** The interactive central workspace where objects are placed, moved, and resized.
        6. **“Lưu”:** The primary action button used to commit all changes (newly created items or updates) to the database.
- **Data processing:**
    - The Admin enters the editing mode by clicking "Chỉnh sửa".
    - To create a new element, the Admin clicks a tool from the “Tool Panel”. The system places the object onto the “Map Canvas” with default properties.
    - The Admin can drag or resize objects directly on the “Map Canvas” to set their exact position and dimensions
    - To update details, the Admin selects an object and modifies the "TÊN PHÒNG THƯ VIỆN" or toggles the "TRẠNG THÁI" in the right sidebar.
    - The Admin clicks "Lưu", the system validates the layout data and updates the database records for all rooms and seating zones.
    - 

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image13.png)

***Figure 31. Create and update area Screen Layout***

**Function details:**

- **Data:** The function retrieves, modifies, and saves area configuration data, including: Area/Room Name, Description/Regulations, selected Utility Tags (Power outlets, Lamps, High-speed WiFi, Air conditioning, etc.), and spatial attributes (X/Y coordinates, Width, Height, and Object Type).
- **Validation:**
    - Access restricted to Admin role.
    - Area Name and Room Name cannot be empty.
- **Business rules:** N/A
- **Normal case:** The Admin adds or modifies areas and clicks save; the system successfully updates the layout and displays a confirmation message.
- **Abnormal case:** In the event of a system failure, the system will display a comprehensive notification message explaining the failure and will prevent any data from being saved to ensure the integrity of the library configuration.

### *3.5.1.3 Delete area*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Select an Area.
- **Timing frequency:** On demand (Whenever the Admin needs to remove a physical area or seating zone from the library layout).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to remove an existing seating area and its associated objects (seats) from the library layout.
- **Interface:**
    - **View area map Screen:**
        1. **“Xóa phòng”:** The red action button in the right sidebar used to trigger the deletion confirmation modal.
    - **Confirm delete Screen**
1. **“Xóa”:** The primary confirmation button (red) used to confirm the removal of the area and its contents.
2. **“Hủy”:** A secondary button used to close the modal and cancel the deletion process.
- **Data processing:**
    - The Admin selects a specific area on the Map Canvas or Sidebar.
    - The Admin clicks the "Xóa phòng" button.
    - The system displays a confirmation modal with the message: "Xóa phòng sẽ xóa [X] khu vực và [X] ghế bên trong! Thay đổi chỉ được lưu khi bấm nút Lưu."
    - If the Admin clicks "Xóa" button, the system removes the area and all its internal objects from the temporary layout configuration.
    - The Map Canvas and Sidebar statistics update immediately to reflect the removal.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image26.png)

***Figure 32. Delete area Screen Layout***

**Function details:**

- **Data:** The function identifies and processes the removal of area-specific data, including: Area ID, Name, and the collection of Seat IDs contained within the specified spatial boundaries.
- **Validation:**
    - Access restricted to Admin role.
    - The system must verify that the area to be deleted exists in the current session.
- **Business rules:** N/A
- **Normal case:** The Admin confirms the deletion; the area is removed from the canvas, and after clicking "Xóa", the system updates the database and the library layout permanently.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining the error and will restore the area to its previous state if the save operation fails.

### *3.5.1.4 Change area status*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Select an Area.
- **Timing frequency:** On demand **(**When the Admin needs to quickly open or close a specific zone for maintenance or operational adjustments).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to toggle the operational status of a specific area by interacting directly with the visual map.
- **Interface:**
    - **View area map Screen:**
        1. **Click on the specific area:** Clickable area on the map that trigger the sidebar controls.
1. **Status Panel:** A section in the right sidebar containing "Trạng thái" controls with buttons for "Hoạt động" (Open) and "Đóng cửa" (Close).
- **Data processing:**
    - The Admin clicks on a specific area object on the Map Canvas.
    - The system identifies the selected object and displays its respective status configuration panel in the right sidebar
    - The Admin selects the new status (e.g., switching from "Hoạt động" to "Đóng cửa").
    - The system updates the sidebar display to reflect the temporary selection.
    - The Admin clicks the global "Lưu" button at the top to commit the status change to the database.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image8.png)

***Figure 33. Change area status Screen Layout***

**Function details:**

- **Data:** The function identifies and processes the status update of the selected object, including: Area ID, Room Name, and the Status field (Active or Unlocked). It records the state change based on the Admin's selection on the interface.
- **Validation:**
    - Access restricted to Admin role.
    - The system must verify that a valid object is selected before allowing the toggle.
- **Business rules:** N/A
- **Normal case:** The Admin toggles the lock button; the room becomes fixed on the canvas, and the setting is successfully saved to the layout configuration.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining the synchronization failure and will revert the interface to the last known stable state

### *3.5.1.5 Lock area movement*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Select an Area.
- **Timing frequency:** On demand **(**When the Admin needs to quickly open or close a specific zone for maintenance or operational adjustments).

**Function description:**

- **Actors/Roles:** Admin.
- **Purpose:** To enable or disable the movement capability of a library room on the design canvas, ensuring layout precision.
- **Interface:**
    - **"Lock area movement" Screen:**
        1. **Click on the specific area**: Clickable area on the map that trigger the sidebar controls.
        2. **Status Panel:** A section in the right sidebar containing "Trạng thái" controls with buttons for "Mở khóa" (Unlock) and "Đã khóa" (Locked).
- **Data processing:**
    - The Admin clicks on a specific area object on the Map Canvas.
    - The system identifies the selected object and displays its respective status configuration panel in the right sidebar
    - The Admin selects the new status (e.g., switching from "Mở khóa" to "Đã khóa").
    - The system updates the sidebar display to reflect the temporary selection.
    - The Admin clicks the global "Lưu" button at the top to commit the status change to the database.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image2.png)

***Figure 34. Lock area movement Screen Layout***

**Function details:**

- **Data:** The function manages the interaction attributes of the room, specifically the movement lock state and the unique identifier of the selected area.
- **Validation:**
    - Access restricted to Admin role.
    - The system must verify that a valid object is selected before allowing the toggle.
- **Business rules:** N/A
- **Normal case:** The Admin toggles the lock button; the room becomes fixed on the canvas, and the setting is successfully saved to the layout configuration.
- **Abnormal case:** In the event of a system failure, the system will display a notification message explaining that the movement settings could not be updated and will restore the room's previous interaction state to ensure layout stability.

### 3.5.2 Zone Management

### *3.5.2.1 View zone map*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Select an zone box on map
- **Timing frequency:** On demand (When the Admin needs to quickly open or close a specific zone for maintenance or operational adjustments).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To display the detailed configuration of a specific seating zone, allowing the Admin to review its name, description, settings, and utilities.
- **Interface:**
    - **View zone map Screen:**
        1. **"Sidebar Selection":** The specific zone item (e.g., "Khu Vực 1") listed under "KHU VỰC GHẾ" in the left panel.
        2. **"Zone on Canvas"**: The visual box representation of the zone on the map workspace.
        3. **"TÊN KHU VỰC"**: An input field displaying the current name of the selected zone.
        4. **"MÔ TẢ / QUY ĐỊNH"**: A text area displaying the usage rules or description for the zone.
        5. **"CÀI ĐẶT"**: The settings section containing the movement lock toggle ("Vị trí có thể di chuyển").
        6. **"TIỆN ÍCH KHU VỰC"**: A collection of tags indicating available amenities (e.g., WiFi, Power Outlet).
- **Data processing:**
    - The Admin clicks on a zone name in the "Sidebar Selection" or directly on the "Zone on Canvas".
    - The system highlights the selected zone on the map to indicate active focus.
    - The system retrieves the zone's metadata from the temporary session or database.
    - The system populates the right sidebar with the zone's specific details: "TÊN KHU VỰC", "MÔ TẢ", "CÀI ĐẶT", and "TIỆN ÍCH KHU VỰC".
    - The Admin can view or verify these details before deciding to make any edits.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image14.png)

***Figure 35. View zone map Screen Layout***

**Function details:**

- **Data:** The function retrieves and displays specific zone attributes, including: Zone Name, Description text, Movement Lock status, and a list of assigned utilities.
- **Validation:**
    - Access restricted to Admin role.
    - Verify that the selected zone exists within the current room context.
- **Business rules:** N/A
- **Normal case:** The Admin selects a zone; the system highlights it and correctly loads all associated details into the configuration panel.
- **Abnormal case:** In the event of a system failure, the system will display a notification message advising the user to refresh the page and will deselect the current item to prevent editing corrupted data.

### *3.5.2.2 Create and update zone*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Click “Chỉnh sửa” button -> Select a zone box on map
- **Timing frequency:** On demand (Whenever the Admin needs to define a new seating zone or modify an existing zone's properties).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to create new zones or update details of existing seating zones, such as name, description, and available utilities within a room.
- **Interface:**
    - **Create and update zone Screen:**
        1. **"Chỉnh sửa":** The toggle button active in edit mode.
        2. **"Tool Panel"**: Buttons to add new objects, including "+ Khu vực".
        3. **"TÊN KHU VỰC"**: An input field in the right sidebar used to name the zone.
        4. **"MÔ TẢ / QUY ĐỊNH"**: A text area used to describe the zone's purpose or rules.
        5. **"TIỆN ÍCH KHU VỰC"**: A section with buttons to add or remove utility tags (e.g., WiFi, AC, Outlet).
        6. **"Zone on Canvas"**: The visual representation of the zone being edited on the map.
        7. **"Lưu"**: The primary action button used to save changes to the database.
- **Data processing:**
    - The Admin enters edit mode by clicking "Chỉnh sửa".
    - The Admin clicks "+ Khu vực" in the "Tool Panel" to add a new zone to the canvas.
    - The Admin selects an existing zone on the "Zone on Canvas" to update it.
    - The Admin inputs the "TÊN KHU VỰC", enters "MÔ TẢ / QUY ĐỊNH", and toggles items in "TIỆN ÍCH KHU VỰC" to edit details.
    - The Admin drags and resizes the zone box on the canvas to adjust the physical area.
    - The Admin clicks "Lưu"; the system validates the zone data and commits the changes to the database.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image12.png)

***Figure 36. Create and update zone Screen Layout***

**Function details:**

- **Data:** The function processes and saves zone-specific configuration data, including: Zone Name, Description/Rules, List of Utility Tags, and Spatial Coordinates (X, Y, W, H).
- **Validation:**
    - Access restricted to Admin role.
    - The "TÊN KHU VỰC" field is mandatory and cannot be empty.
    - The zone must be placed within the valid boundaries of the room canvas.
- **Business rules:** N/A
- **Normal case:** The Admin enters valid zone details and saves; the system successfully updates the zone configuration and refreshes the map view.
- **Abnormal case:** In the event of a system failure, the system will display a specific notification message explaining the error and will retain the current input data to allow the Admin to make corrections.

### *3.5.2.3 Delete zone*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Click “Chỉnh sửa” button -> Select a zone box on map -> Click “Xóa khu vực” button.
- **Timing frequency:** On demand (Whenever the Admin needs to permanently remove a seating zone from the library layout).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to remove an existing seating zone and all its assigned properties from the current room configuration.
- **Interface:**
    - **Delete area Screen:**
        1. **"Xóa khu vực"**: the red action button in the right sidebar used to trigger the deletion confirmation.
        2. **"Xóa"**: the primary red button within the modal used to confirm the removal of the zone.
        3. **"Hủy"**: the white button within the modal used to cancel the deletion and close the popup.
- **Data processing:**
    - The Admin selects a zone on the map canvas or through the sidebar list.
    - The Admin clicks the "Xóa khu vực" button located at the bottom of the right sidebar.
    - The system displays a confirmation modal with the message: "Xóa khu vực sẽ xóa 0 ghế bên trong! Thay đổi chỉ được lưu khi bấm nút Lưu."
    - The Admin clicks the "Xóa" button to proceed with the deletion.
    - The system removes the zone and its associated seat count from the temporary layout state and updates the sidebar statistics.
    - The Admin clicks the global "Lưu" button to synchronize the removal with the permanent database.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image1.png)

***Figure 37. Delete zone***

**Function details:**

- **Data:** The function identifies the unique Zone ID and the collection of Seat IDs visually contained within the zone's boundaries for processing the removal.
- **Validation:**
    - Access restricted to Admin role.
    - The system must verify that the zone is not currently in a state that prevents deletion (e.g., active student reservations if applicable).
- **Business rules:** N/A
- **Normal case:** The Admin confirms the deletion; the zone disappears from the canvas, and after saving, the library layout is permanently updated.
- **Abnormal case:** In the event of a system failure, the system will display a notification message explaining that the deletion failed and will restore the zone to the canvas to ensure data consistency.

### *3.5.2.4 Create zone attribute*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Click “Chỉnh sửa” button -> Select a zone box on map -> Right Sidebar -> Click "+ Thêm"
- **Timing frequency:** On demand (Whenever the Admin needs to define a new facility or feature for a specific seating zone).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to manually add new utility tags (e.g., "Ổ cắm điện", "WiFi mạnh") to a seating zone's profile.
- **Interface:**
    - **Create zone attribute Screen:**
        1. **"+ Thêm"**: the trigger button located in the "TIỆN ÍCH KHU VỰC" section of the sidebar used to open the creation modal.
        2. **"Tên tiện ích..."**: an input field within the modal used to type the name of the new attribute.
        3. **"Thêm"**: the primary green button used to confirm and add the new utility to the list.
        4. **"Hủy"**: the secondary white button used to close the modal without saving the new attribute.
- **Data processing:**
    - The Admin selects a zone and clicks the "+ Thêm" button in the sidebar.
    - The system displays the "Thêm tiện ích mới" modal.
    - The Admin types the name of the facility into the "Tên tiện ích..." input field.
    - The Admin clicks the "Thêm" button.
    - The system validates that the input is not empty and adds the new tag to the zone's utility list in temporary memory.
    - The new attribute appears as a selectable tag in the "TIỆN ÍCH KHU VỰC" section.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image35.png)

***Figure 38. Create zone attribute***

**Function details:**

- **Data:** The function processes the Utility Name string and associates it with the specific Zone ID.
- **Validation:**
    - Access restricted to Admin role.
    - Verify that the utility name field is not empty.
    - Verify that the utility name does not already exist for the current zone to avoid duplicates.
- **Business rules:** N/A
- **Normal case:** The Admin enters a valid utility name and confirms; the tag is added to the list and displayed in the sidebar.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message explaining the error and will prevent the data from being recorded to maintain system consistency.

### *3.5.2.5 Delete zone attribute*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Click “Chỉnh sửa” button -> Select a zone box on map -> Right Sidebar -> Click the "Icon X" on a utility tag.
- **Timing frequency:** On demand (Whenever the Admin needs to remove an existing facility or feature from a seating zone's profile).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to remove a specific utility tag from a seating zone.
- **Interface:**
    - **Delete zone attribute Screen:**
        1. **"Icon X"**: the small delete icon located next to each utility name (e.g., next to 'a') in the sidebar used to trigger removal.
        2. **"Xóa"**: the primary red button in the confirmation modal used to confirm the deletion.
        3. **"Hủy"**: the secondary white button in the modal used to cancel the deletion.
- **Data processing:**
    - The Admin selects a zone and identifies the utility to be removed in the right sidebar.
    - The Admin clicks the "Icon X" next to the specific utility name.
    - The system displays the "Xóa tiện ích?" confirmation modal.
    - The Admin clicks the "Xóa" button.
    - The system removes the utility tag from the zone's list in temporary memory.
    - The Admin clicks the global "Lưu" button to finalize the removal in the database.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image32.png)

***Figure 39. Delete zone attribute***

**Function details:**

- **Data:** The function identifies the specific Utility ID or Name and the associated Zone ID to process the removal.
- **Validation:** Access is strictly restricted to the Admin role.
- **Business rules:** N/A
- **Normal case:** The Admin confirms the deletion; the utility tag is removed from the sidebar view and the database update is prepared.
- **Abnormal case:** In the event of a system failure, the system will display a notification message explaining the failure and will restore the utility tag to the list to ensure configuration integrity.

### *3.5.2.6 Lock zone movement*

**Function trigger:**

- **Navigation path:** Web Portal (Admin) -> Side Menu -> Map Icon (Bản đồ thư viện) -> Click “Chỉnh sửa” button -> Select a zone box on map
- **Timing frequency:** On demand (When the Admin needs to fix the physical position of a seating zone on the layout).

**Function description:**

- **Actors/Roles:** Admin
- **Purpose:** To allow the Admin to lock or unlock the dragging capability of a specific seating zone box within a library room.
- **Interface:**
    - **Lock zone movement Screen:**
        1. **"Khu vực trên sơ đồ"**: the visual box representation of the seating zone on the map workspace where the Admin clicks to trigger focus.
        2. **"Đã khóa vị trí"**: a toggle button located in the "CÀI ĐẶT" section of the right sidebar used to enable or disable the movement of the selected zone.
- **Data processing:**
    - The Admin enters the editing mode and clicks on a "Khu vực trên sơ đồ" within the central canvas area.
    - The system identifies the selected zone and populates its specific configuration panel in the right sidebar.
    - The Admin clicks the "Đã khóa vị trí" button to toggle the movement state between Locked and Unlocked.
    - The system updates the interaction property of the zone box in the temporary session memory, preventing or allowing drag-and-drop actions.
    - The Admin clicks the global "Lưu" button at the top of the screen to commit the interaction settings to the permanent database.

**Screen layout:**

![](Report3_Software%20Requirement%20Specification/image25.png)

***Figure 40. Delete zone attribute***

**Function details:**

- **Data:** The function manages the interaction attributes of the seating zone, specifically the movement lock state and the associated Zone ID.
- **Validation:** * Access is strictly restricted to the Admin role only.
- The system must verify that a zone is actively selected before enabling the movement lock controls.
- **Business rules:** N/A
- **Normal case:** The Admin toggles the lock button; the zone becomes fixed at its current coordinates on the canvas, and the configuration is successfully saved.
- **Abnormal case:** In the event of a system failure, the system will display a clear notification message informing the Admin that the movement settings could not be updated and will revert the zone to its previously saved interaction state.

## 4. Non-Functional Requirements

### 4.1 External Interfaces

*[This section provides information to ensure that the system will communicate properly with users and with external hardware or software/system elements.]*

### 4.2 Quality Attributes

*[List all the required system characteristics (quality attributes) specification. Some of the possible attributes are provided with the guide/descriptions are mentioned here]*

### 4.2.1 Usability

*[This section includes all those requirements that affect usability. For example, specify the required training time for a normal users and a power user to become productive at particular operations specify measurable task times for typical tasks or base the new system’s usability requirements on other systems that the users know and like specify requirement to conform to common usability standards, such as IBM’s CUA standards Microsoft’s GUI standards]*

### 4.2.2 Reliability

*[Requirements for reliability of the system should be specified here. Some suggestions follow:*

*Availability—specify the percentage of time available ( xx.xx%), hours of use, maintenance access, degraded mode operations, and so on.*

*Mean Time Between Failures (MTBF) — this is usually specified in hours, but it could also be specified in terms of days, months or years.*

*Mean Time To Repair (MTTR)—how long is the system allowed to be out of operation after it has failed?*

*Accuracy—specifies precision (resolution) and accuracy (by some known standard) that is required in the system’s output.*

*Maximum Bugs or Defect Rate—usually expressed in terms of bugs per thousand lines of code (bugs/KLOC) or bugs per function-point( bugs/function-point).*

*Bugs or Defect Rate—categorized in terms of minor, significant, and critical bugs: the requirement(s) must define what is meant by a “critical” bug; for example, complete loss of data or a complete inability to use certain parts of the system’s functionality.]*

### 4.2.3 Performance

*[The system’s performance characteristics are outlined in this section. Include specific response times. Where applicable, reference related Use Cases by name.*

*Response time for a transaction (average, maximum)*

*Throughput, for example, transactions per second*

*Capacity, for example, the number of customers or transactions the system can accommodate*

*Resource utilization, such as memory, disk, communications, and so forth.]*

### 4.2.4 …

## 5. Requirement Appendix

*[Provide business rules, common requirements, or other extra requirements information here]*

### 5.1 Business Rules

*[Provide common business rules that you must follow. The information can be provided in the table format as the sample below]*

| **ID** | **Rule Definition** |
| --- | --- |
| BR-01 | Delivery time windows are 15 minutes, beginning on each quarter hour. |
| BR-02 | Deliveries must be completed between 10:00 A.M. and 2:00 P.M. local time, inclusive. |
| BR-03 | All meals in a single order must be delivered to the same location. |
| BR-04 | All meals in a single order must be paid for by using the same payment method. |
| BR-11 | If an order is to be delivered, the patron must pay by payroll deduction. |
| BR-12 | Order price is calculated as the sum of each food item price times the quantity of that food item ordered, plus applicable sales tax, plus a delivery charge if a meal is delivered outside the free delivery zone. |
| BR-24 | Only cafeteria employees who are designated as Menu Managers by the Cafeteria Manager can create, modify, or delete cafeteria menus. |
| BR-33 | Network transmissions that involve financial information or personally identifiable information require 256-bit encryption. |
| BR-86 | Only regular employees can register for payroll deduction for any company purchase. |
| BR-88 | An employee can register for payroll deduction payment of cafeteria meals if no more than 40 percent of his gross pay is currently being deducted for other reasons. |

### 5.2 Common Requirements

*[Fill all the common requirements here..]*

### 5.3 Application Messages List

| **#** | **Message code** | **Message Type** | **Context** | **Content** |
| --- | --- | --- | --- | --- |
| 1 | MSG01 | In line | There is not any search result | *No search results.* |
| 2 | MSG02 | In red, under the text box | Input-required fields are empty | *The * field is required.* |
| 3 | MSG03 | Toast message | Updating asset(s) information successfully | *Update asset(s) successfully.* |
| 4 | MSG04 | Toast message | Adding new asset successfully | *Add asset successfully.* |
| 5 | MSG05 | Toast message | Confirming email of asset hand-over is sent successfully | *A confirmation email has been sent to {email_address}.* |
| 6 | MSG06 | Toast message | Resetting asset information successfully | *Return asset(s) successfully.* |
| 7 | MSG07 | Toast message | Deleting asset information successfully | *Delete asset(s) successfully.* |
| 8 | MSG08 | In red, under the text box | Input value length > max length | *Exceed max length of {max_length}.* |
| 9 | MSG09 | In line | Username or password is not correct when clicking sign-in | *Incorrect user name or password. Please check again.* |

### 5.4 Other Requirements…

### 

###