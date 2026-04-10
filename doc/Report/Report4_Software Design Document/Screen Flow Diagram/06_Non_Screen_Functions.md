# 3.1.4 Non-Screen Functions

| # | Feature | System Function | Description |
|---|---|---|---|
| 1 | Authentication Module | Session Validation | The system checks whether a valid login session already exists and redirects users to the correct entry flow. |
| 2 | Authentication Module | Token Expiration Handling | The system automatically detects expired tokens and forces users to re-authenticate when necessary. |
| 3 | Authentication Module | Role-Based Redirect | The system redirects authenticated Admin and Librarian users to the correct dashboard based on their role. |
| 4 | Booking Seat Module | Automatic Booking Timeout | The system automatically cancels or invalidates incomplete booking flows when confirmation does not finish in time. |
| 5 | Booking Seat Module | Real-Time Seat Update | The system synchronizes seat availability and seat status in real time through WebSocket updates. |
| 6 | Booking Seat Module | Ongoing Slot Validation | The system warns users when they attempt to book a seat in a time slot that has already started. |
| 7 | Booking Seat Module | Library Closed Validation | The system prevents seat booking flows when the library is closed or when the selected day is not a working day. |
| 8 | Booking Seat Module | AI Seat Suggestion | The system generates AI-based seat recommendations and can redirect users to the related booking flow. |
| 9 | Library Access Module | QR Access Processing | The system validates kiosk QR sessions and processes check-in or check-out actions. |
| 10 | Library Access Module | HCE Access Processing | The system processes HCE-based access events and updates access logs accordingly. |
| 11 | Notification Module | Notification Routing | The system maps notifications to related modules and opens the relevant management or detail flow when selected. |
| 12 | Feedback Module | Pending Feedback Detection | The system checks whether a completed booking still requires feedback and prompts the user automatically. |
| 13 | Chat & Support Module | Queue Position Monitoring | The system tracks librarian support queue state and updates the waiting experience for students in real time. |
| 14 | Chat & Support Module | Conversation Status Polling | The system monitors whether a conversation remains in AI mode, waiting mode, human support mode, or resolved mode. |
| 15 | Chat & Support Module | WebSocket Message Synchronization | The system synchronizes librarian chat messages and read-related events in real time. |
| 16 | Chat & Support Module | Post-Chat Feedback Trigger | The system shows a feedback prompt after a librarian conversation ends when feedback has not yet been submitted. |
| 17 | User Management Module | Bulk Import Processing | The system validates uploaded import files, stages imported data, and summarizes successful and failed records. |
| 18 | System Configuration Module | Backup Schedule Execution | The system stores and executes automatic backup schedules according to configured timing rules. |
| 19 | System Configuration Module | Manual Backup Execution | The system allows administrators to trigger an on-demand backup process and records the result in backup history. |
| 20 | HCE Scan Station Management | Heartbeat Monitoring | The system continuously tracks station heartbeat information and updates station availability states. |
| 21 | NFC Tag Management | NFC Bridge Assistance | The system supports external bridge-assisted NFC scanning and verification flows for seat-tag management. |
| 22 | Kiosk Device Management | Kiosk Activation Flow | The system creates activation codes or tokens and binds devices securely during kiosk activation. |
| 23 | News & Announcement Module | Draft Persistence | The system preserves draft content during the news creation or editing process before final publication. |
| 24 | News & Announcement Module | Scheduled Publishing | The system stores scheduled publication time and applies delayed publishing logic for news content. |
| 25 | Statistics & Report Module | AI Prioritized Student Analysis | The system identifies priority students from analytics signals and exposes them to the AI analytics panel. |
| 26 | Statistics & Report Module | Warning Dispatch | The system sends warning notifications to target students from the AI analytics workflow. |
| 27 | Mobile App | Deep Link and Startup Navigation | The system initializes app services, restores user state, and routes the user to onboarding or the main application. |
| 28 | Mobile App | Local Cache Restoration | The system restores cached news, new books, and conversation state to improve startup continuity. |
| 29 | Mobile App | Camera Permission Handling | The system requests and validates camera permission before QR scanning can begin. |
| 30 | Report Module | History Filter Processing | The system applies time-based or status-based filters when users view booking, violation, complaint, and report histories. |

## Notes

- These functions are derived from the latest web and mobile UI flows but do not exist as independent named screens.
- They are still important to the user experience because they control navigation, conditions, automation, synchronization, and system-driven transitions.
- The list focuses on currently visible non-screen behaviors that influence how users move through the system.
