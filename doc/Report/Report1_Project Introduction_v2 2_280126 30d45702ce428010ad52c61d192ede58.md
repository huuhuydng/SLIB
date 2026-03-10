# Report1_Project Introduction_v2.2_280126

![](Report1_Project%20Introduction_v2%202_280126/image1.png)

**CAPSTONE PROJECT REPORT**

**Report 1 – Project Introduction**

> – Da Nang, January 2026 –
> 

**Table of Contents**

[I. Record of Changes 5](about:blank#i.-record-of-changes)

[II. Project Introduction 6](about:blank#ii.-project-introduction)

> [1. Overview 6](about:blank#overview)
> 
> 
> [1.1 Project Information 6](about:blank#project-information)
> 
> [1.2 Project Team 6](about:blank#project-team)
> 
> [2. Product Background 6](about:blank#product-background)
> 
> [3. Existing Systems 7](about:blank#existing-systems)
> 
> [3.1 Columbia University Libraries 7](about:blank#columbia-university-libraries)
> 
> [3.2 LibCal (Springshare) 9](about:blank#libcal-springshare)
> 
> [4. Business Opportunity 10](about:blank#business-opportunity)
> 
> [5. Software Product Vision 11](about:blank#software-product-vision)
> 
> [6. Project Scope & Limitations 11](about:blank#project-scope-limitations)
> 
> [6.1 Major Features 11](about:blank#major-features)
> 
> [6.2 Limitations & Exclusions 14](about:blank#limitations-exclusions)
> 

**Table of Tables**

Table 1. Record of Changes 5

Table 2. Project Team 6

Table 3. Limitations & Exclusions 14

**Table of Figures**

Figure 1. Columbia University Libraries 7

Figure 2. LibCal (Springshare) 9

# I. Record of Changes

- A - Added M - Modified D - Deleted

| **Date** | **A*
M, D** | **In charge** | **Change Description** |
| --- | --- | --- | --- |
| 2025/12/12 | A | ThongDV | Complete Project Introduction (Exclude group name and phone number of Lecturer) version 1.0 |
| 2025/12/17 | M | ThongDV | Modify 6.1 Major Features |
| 2025/12/21 | M | ThongDV | Add more features in 6.1 Major Features |
| 2025/12/22 | M | ThongDV | Complete Report1_Project Introduction version 2.0 |
| 2026/01/11 | M | ThongDV | Complete Report1_Project Introduction version 2.1 |
| 2026/01/28 | M | ThongDV | Complete Report1_Project Introduction version 2.2 |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |

***Table 1. Record of Changes***

# II. Project Introduction

## 1. Overview

### 1.1 Project Information

- Project name: Building a smart library ecosystem SLIB with HCE application, smart booking, reputation score and AI analysis using Flutter, PostgreSQL Database, Spring Boot or FastAPI technology
- Project code: SLIB – Smart Library Ecosystem
- Group name: SE_30
- Software type: Website Application, Mobile Application

### 1.2 Project Team

| **Full Name** | **Role** | **Email** | **Mobile** |
| --- | --- | --- | --- |
| Le Cong Vo | Lecturer | [volc@fpt.edu.vn](mailto:volc@fpt.edu.vn) | 0935552277 |
| Nguyen Huu Huy | Leader | [huynhde180295@fpt.edu.vn](mailto:huynhde180295@fpt.edu.vn) | 0833100904 |
| Nguyen Hoang Phuc | Member | [phucnhde170706@fpt.edu.vn](mailto:phucnhde170706@fpt.edu.vn) | 0941887045 |
| Dinh Viet Thong | Member | [thongdvde180296@fpt.edu.vn](mailto:thongdvde180296@fpt.edu.vn) | 0935297204 |
| Trinh Ba Hoang Huy | Member | [huytbhde180057@fpt.edu.vn](mailto:huytbhde180057@fpt.edu.vn) | 0347734665 |
| Bui Duc Hung | Member | [hungbdde180435@fpt.edu.vn](mailto:hungbdde180435@fpt.edu.vn) | 0339923302 |

***Table 2. Project Team***

## 2. Product Background

The current operation of the FPT University Library is very open and lacks space control. At present, any student or staff member can enter and leave the library freely without checking in or checking out, and there is no system for reserving seats in advance. Because of this, it is difficult to track how the library is actually used, student behaviour cannot be monitored effectively, and library rules related to seating and study time cannot be enforced.

To solve these problems, a new management solution is needed to turn the library into a more organized and controlled space. The SLIB (Smart Library Ecosystem) project, initiated by **Lecturer Le Cong Vo** and the **Capstone Student Group (BIT_SE_18B_KL)**, aims to improve library management by introducing mandatory access control using Host Card Emulation (HCE) and an intelligent seat booking system. In addition, SLIB will apply a Reputation Score system to encourage students to follow library rules and to penalize inappropriate behaviour. This approach helps ensure fair, responsible, and efficient use of the library facilities.

## 3. Existing Systems

### 3.1 Columbia University Libraries

![](Report1_Project%20Introduction_v2%202_280126/image3.png)

***Figure 1. Columbia University Libraries***

**Columbia University Libraries** is an academic library web platform designed to support students, faculty, and researchers in accessing library resources and services efficiently. The system provides a centralized interface for searching academic materials, exploring digital and physical collections, and utilizing various library services. In addition, the platform offers an online **study space and seat reservation feature**, allowing users to conveniently book seats or study areas in advance and manage their study schedules effectively. The reservation system is integrated with a **check-in and check-out mechanism** to ensure fair usage of library spaces and optimize seat availability. The system is available on **web browsers** and supports online access to library resources and services.

- System Actors:
    - **Visitors / Guests:** External users with limited access who may browse public library information or use specific services according to library policies.
    - **Students:** Individuals who use the web platform to search for library resources, reserve study seats or spaces, check in and check out of reserved seats, and access library services for academic purposes.
    - **Faculty and Staff:** University employees who use the system to access academic materials, reserve library spaces, and utilize research and teaching support services provided by the library.
    - **Librarians:** Library personnel responsible for managing library resources, monitoring seat and space reservations, assisting users, and enforcing library usage policies.
    - **System Administrators:** Technical staff who maintain the web platform, manage user authentication and permissions, configure reservation rules, ensure system availability, and protect data security.
- Core Features:
    - **Library Resource Search:** Search and access a wide range of academic resources including books, journals, articles, and digital collections through a centralized search interface.
    - **Study Seat Reservation:** Allow users to reserve individual study seats or designated study spaces online based on availability, location, and time slots.
    - **Check-In / Check-Out Management:** Enable users to check in and check out of reserved seats to confirm actual usage, helping ensure fair access and efficient utilization of library spaces.
    - **Room and Space Booking:** Support reservations for group study rooms or specialized library spaces with configurable booking rules and time limits.
    - **Library Information & Services:** Provide up-to-date information on library hours, locations, policies, workshops, and events across different library branches.
    - **User Account Management:** Allow users to view their reservation history, manage active bookings, and access personalized library services through their accounts.
- Pros:
    - **Comprehensive Academic Resource Access:** The platform provides centralized access to a vast collection of academic books, journals, databases, and digital materials, supporting learning and research activities effectively.
    - **Restricted Access Through Authentication:** Core library features such as seat reservations, check-in/check-out, and personalized services are restricted to authenticated users, ensuring that library resources are used only by authorized students and staff.
    - **Online Seat and Space Reservation:** The integrated reservation system allows users to book study seats and library spaces in advance, helping reduce overcrowding and improve study planning.
    - **Check-In / Check-Out Mechanism:** The check-in and check-out feature ensures fair usage of library seats by verifying actual attendance and automatically releasing unused reservations.
    - **Institution-Level Integration:** The system is tightly integrated with the university’s authentication and library infrastructure, ensuring secure access and consistent user management.
    - **Clear Information and Policies:** Users can easily find information about library hours, locations, services, and usage policies across different library branches.
- Cons:
    - **No Native HCE/NFC Support:** Check-in and check-out processes rely on manual actions such as QR codes or web links, without direct support for advanced technologies like HCE-based contactless check-in.
    - **Fragmented User Experience:** Some services, such as seat reservations, are hosted on separate sub-systems, which may require users to navigate between multiple interfaces.
    - **Lack of Personalization Features:** Compared to modern web platforms, the system offers limited personalized recommendations or adaptive features based on user behavior.
    - **Learning Curve for New Users:** Due to the wide range of services and academic tools available, first-time users may need time to become familiar with the system’s structure and functionalities.

### 3.2 LibCal (Springshare)

![](Report1_Project%20Introduction_v2%202_280126/image2.png)

***Figure 2. LibCal (Springshare)***

**LibCal** is a web-based library service management platform developed by Springshare, designed to help academic libraries manage bookings, events, and study spaces efficiently. The system provides online tools for **seat and space reservation**, allowing users to book individual seats, group study rooms, or library facilities in advance. LibCal also supports a **check-in and check-out mechanism** to confirm user attendance and ensure fair utilization of library resources. Accessible through **web browsers and optimized for mobile devices**, LibCal is widely adopted by university libraries to improve space management and user experience.

- System Actors:
    - **Students:** Primary users who access the web platform to reserve study seats or spaces, check in and check out of reservations, and manage their booking schedules.
    - **Faculty and Staff:** University members who use the system to book library spaces for academic, teaching, or research purposes.
    - **Visitors / Guests:** External users who may access limited booking features or public event registrations, depending on library policies.
    - **Librarians:** Library staff responsible for configuring seat layouts, managing reservation rules, monitoring check-in/check-out activity, and assisting users.
    - **System Administrators:** Technical administrators who manage system configuration, authentication integration, access control, and ensure system stability and data security.
- Core Features:
    - **Seat and Space Reservation:** Allow users to reserve individual study seats, group study rooms, or library spaces based on availability, time slots, and location.
    - **Check-In / Check-Out Management:** Enable users to confirm attendance through check-in and check-out when leaving, automatically releasing unused or expired reservations.
    - **Interactive Seat Maps:** Provide visual layouts of library spaces, allowing users to select seats or rooms directly from a map-based interface.
    - **Reservation Rules and Limits:** Support configurable policies such as maximum booking duration, daily limits, and no-show penalties.
    - **Event and Room Booking Integration:** Combine seat reservations with event scheduling and room bookings within a single web platform.
    - **User Account Management:** Allow users to view upcoming reservations, booking history, and manage cancellations through their personal accounts.
- Pros:
    - **Specialized Library Booking System:** LibCal is specifically designed for libraries, making it highly suitable for managing study spaces and academic environments.
    - **Online Seat Reservation with Access Control:** Core features such as seat booking and check-in/check-out are restricted to authenticated users, ensuring controlled and fair access to library resources.
    - **Efficient Check-In / Check-Out Process:** The attendance confirmation mechanism helps reduce no-shows and improves overall seat utilization.
    - **Flexible Configuration:** Librarians can easily customize booking rules, seat layouts, and access permissions without extensive technical expertise.
    - **Web-Based and Mobile-Friendly:** Users can access the system through web browsers on both desktop and mobile devices without requiring a dedicated mobile app.
- Cons:
    - **No Native HCE/NFC Support:** LibCal does not natively support HCE-based contactless check-in; attendance confirmation typically relies on QR codes or web-based actions.
    - **Dependence on External Authentication:** Integration with institutional login systems may require additional configuration and technical support.
    - **Fragmented User Experience Across Modules:** Seat booking, room booking, and event management are separate modules, which may require users to navigate between different sections.
    - **Commercial Licensing Cost:** As a commercial product, LibCal requires paid subscriptions, which may be a barrier for smaller institutions.

## 4. Business Opportunity

The primary business opportunity for SLIB (Smart Library Ecosystem) is to address the functional gap left by the existing FPT University Library System, specifically concerning uncontrolled physical space usage and the lack of data-driven governance. While the existing system excels in book and resource management, it lacks mandatory access control, real-time spatial awareness, and behavioural monitoring capabilities. SLIB is designed as a supplementary ecosystem for the Da Nang campus, providing a critical Value Proposition through four core functional pillars:

- **Improved Operational Efficiency:** By implementing HCE-based access control and a structured Seat Booking System, SLIB directly eliminates administrative inefficiencies and the 'ghost seating' problem, ensuring the library's physical space is utilized responsibly and to its maximum capacity.
- **Enhanced Student Experience:** The system provides students with the convenience of real-time seat reservation, personalized AI time recommendations based on past behaviour and predicted occupancy, and instant support via the AI Chatbot. This significantly saves student time and ensures a predictable study environment.
- **Data-Driven Decision Making:** The platform integrates AI analytics and a real-time dashboard. This capability provides librarians and administrators with valuable insights, such as peak hour prediction and violation reports, which are currently unavailable. This aligns with the strategic direction of modern smart university campuses focused on technology integration and behavioural analysis.
- **Enforcement of Governance:** The implementation of the Reputation Score mechanism is a critical feature that solves the problem of non-compliance (no-show violations) which cannot be solved by existing manual processes.

SLIB transforms the library from a passively managed facility into an Intelligent, Proactive, and Accountable educational resource.

## 5. Software Product Vision

For FPT University students and library staff who need efficient, predictable access to dedicated study space and stringent behavioural management, the **Smart Library Ecosystem (SLIB)** is a mobile-first, AI-driven platform that enables students to find and secure their ideal study spot through mandatory, contactless check-in/out using HCE, coupled with a real-time intelligent seat booking system and a behavioural Reputation Score. Unlike the current system of unrestricted access and lack of accountability, SLIB transforms the library into a fully optimized, data-driven smart space, guaranteeing fair, reliable resource allocation for students while empowering librarians with AI insights and automated governance to detect, monitor, and enforce compliance against non-compliant behaviours.

## 6. Project Scope & Limitations

### Major Features

- **Authentication Module**
- FE-01: Log in via Google Account
- FE-02: Log in via SLIB Account
- FE-03: Log out
- **Account Management Module**
    - FE-04: View profile
    - FE-05: Change basic profile
    - FE-06: Change password
    - FE-07: View Barcode
    - FE-08: View history of activities
    - FE-09: View account setting
    - FE-10: Turn on/Turn off notification
    - FE-11: Turn on/Turn off AI suggestion
    - FE-12: Turn on/Turn off HCE feature
- **User Management Module**
    - FE-13: View list of users in the system
    - FE-14: Import Student via file
    - FE-15: Download template of the Student upload file
    - FE-16: Add Librarian to the system
    - FE-17: View user details
    - FE-18: Change user status
    - FE-19: Delete user account
- **System Configuration Module**
    - Area Management:
        - FE-20: View area map
        - FE-21: CRUD area
        - FE-22: Change area status
        - FE-23: Lock area movement
    - Zone Management:
        - FE-24: View zone map
        - FE-25: CRUD zone
        - FE-26: CRUD zone attribute
        - FE-27: View zone details
        - FE-28: Lock zone movement
    - Seat Management:
        - FE-29: View seat map
        - FE-30: CRUD seat
        - FE-31: Change seat status
    - Reputation Rule Management:
        - FE-32: View list of reputation rules
        - FE-33: CRUD reputation rule
        - FE-34: Set the deducted point for each reputation rule
    - Library Configuration Management:
        - FE-35: Set library operating hours
        - FE-36: Configure booking rules
        - FE-37: Turn on/Turn off automatic check-out when time exceeds
        - FE-38: Enable/Disable library
    - HCE Device Management:
        - FE-39: View list of HCE devices
        - FE-40: CRUD HCE device
        - FE-41: View HCE device details
    - AI Configuration Management:
        - FE-42: CRUD material
        - FE-43: View list of materials
        - FE-44: CRUD knowledge store
        - FE-45: View list of knowledge stores
        - FE-46: Test AI chat
    - NFC Device Management:
        - FE-47: CRUD NFC device
        - FE-48: View list of NFC devices
        - FE-49: View NFC device details
    - Kiosk Management:
        - FE-50: View list of Kiosk images
        - FE-51: CRUD Kiosk image
        - FE-52: Change image status
        - FE-53: Preview Kiosk display
    - Others:
        - FE-54: Config system notification
        - FE-55: View system overview information
        - FE-56: View system log
        - FE-57: Backup data manually
        - FE-58: Set automatic backup schedule
- **Booking Seat Module**
    - FE-59: View real time seat map
    - FE-60: Filter seat map
    - FE-61: View map density
    - FE-62: Booking seat
    - FE-63: Preview booking information
    - FE-64: Confirm booking via NFC
    - FE-65: View history of booking
    - FE-66: Cancel booking
    - FE-67: Ask AI for recommending seat
    - FE-68: Request seat duration
    - FE-69: View list of Student bookings
    - FE-70: Search and Filter Student booking
    - FE-71: View booking details and status
    - FE-72: Cancel invalid booking
- **Library Access Module**
    - FE-73: Check-in/Check-out library via HCE
    - FE-74: Check-in/Check-out library via QR code
    - FE-75: View history of check-ins/check-outs
    - FE-76: View list of Students access to library
- **Reputation & Violation Module**
    - Reputation Score Management
        - FE-77: View reputation score
        - FE-78: View history of changed reputation points
        - FE-79: View detailed reason for deducting point
        - FE-80: View list of Students violation
        - FE-81: View Student violation details
    - Complaint Management
        - FE-82: Create complaint
        - FE-83: View history of sending complaint
        - FE-84: View list of complaints
        - FE-85: View complaint details
        - FE-86: Verify complaint
- **Feedback Module**
    - Feedback System Management
        - FE-87: Create feedback after check-out
        - FE-88: View list of feedbacks
        - FE-89: View feedback details
    - Seat Status Management
        - FE-90: Create seat status report
        - FE-91: View history of sending seat status report
        - FE-92: View list of seat status reports
        - FE-93: View seat status report details
        - FE-94: Verify seat status report
    - Report Seat Violation Management
        - FE-95: Create report seat violation
        - FE-96: View history of sending report seat violation
        - FE-97: View list of seat violation reports
        - FE-98: View report seat violation details
        - FE-99: Verify seat violation report
- **Notification Module**
    - FE-100: View and delete list of notifications
    - FE-101: View notification details
    - FE-102: Filter notification
    - FE-103: Mark notification as read
- **News & Announcement Module**
    - FE-104: View list of news & announcements
    - FE-105: View news & announcement details
    - FE-106: View list of news & announcement categories
    - FE-107: View list of new books
    - FE-108: View basic information of new book
    - FE-109: CRUD new book
    - FE-110: CRUD news & announcement
    - FE-111: CRUD news & announcement category
    - FE-112: Set time to post news & announcement
    - FE-113: Save news & announcement draft
- **Chat & Support Module**
    - FE-114: Chat with AI virtual assistant
    - FE-115: Chat with Librarian
    - FE-116: View history of chat
    - FE-117: View list of chats
    - FE-118: View chat details
    - FE-119: Response to Student manually
    - FE-120: Response to Student with AI suggestion
- **Statistics & Report Module**
    - Statistics Management
        - FE-121: View general analytics dashboard
        - FE-122: View violation statistics
        - FE-123: View statistics of density forecast by using AI
        - FE-124: View check-in/check-out statistics (Daily/Weekly/Monthly)
        - FE-125: View seat booking statistics
        - FE-126: View statistics of analyzing feedback
    - Report Management
        - FE-121: Export seat & maintenance report
        - FE-122: Export general analytical report

### 6.2 Limitations & Exclusions

| **ID** | **Major Features** |
| --- | --- |
| LE-01 | The SLIB system is scoped exclusively for the FPT University Da Nang campus; multi-campus integration is excluded. |
| LE-02 | SLIB will not integrate with the existing FPTU Library System for managing books, documents, or digital resources. |
| LE-03 | The system will not support the reservation of high-value physical resources such as meeting rooms or specialized equipment. |
| LE-04 | SLIB will not include integration with any external payment gateway to process monetary fines related to Reputation Score deductions. |

***Table 3. Limitations & Exclusions***