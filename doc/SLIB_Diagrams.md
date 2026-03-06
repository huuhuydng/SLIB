# SLIB System Diagrams

This document contains comprehensive Git Flow and Business Flow diagrams for the SLIB Library Management System.

---

## 1. SLIB Git Flow

```mermaid
gitGraph
    commit id: "Initial Setup"
    branch develop
    checkout develop
    commit id: "Base Structure"
    
    branch feature/user-auth
    checkout feature/user-auth
    commit id: "JWT Auth"
    commit id: "Google OAuth"
    checkout develop
    merge feature/user-auth
    
    branch feature/admin-map-config
    checkout feature/admin-map-config
    commit id: "Area Management"
    commit id: "Zone Config"
    commit id: "Seat Config"
    checkout develop
    merge feature/admin-map-config
    
    branch feature/seat-booking
    checkout feature/seat-booking
    commit id: "Booking Flow"
    commit id: "QR/NFC Check-in"
    checkout develop
    merge feature/seat-booking
    
    branch feature/ai-service
    checkout feature/ai-service
    commit id: "AI Chatbot"
    commit id: "RAG Integration"
    checkout develop
    merge feature/ai-service
    
    branch system-aiService
    checkout system-aiService
    commit id: "System Integration"
    commit id: "Full Features" type: HIGHLIGHT
    
    checkout main
    merge develop id: "v1.0 Release"
```

### Current Branch Structure

| Branch | Status | Purpose |
|--------|--------|---------|
| `main` | Production | Stable released code |
| `develop` | Integration | Feature integration |
| `system-aiService` | **Active** | Complete system with all features |
| `feature/forgot-password` | PR Open | Password reset functionality |
| `feature/user-deletion` | PR Open | User deletion with confirmation |
| `feature/account-settings` | PR Open | Account settings UI |
| `feature/violation-history` | PR Open | Violation history screen |

### Branch Naming Convention

```
feature/     - New features
fix/         - Bug fixes
hotfix/      - Emergency production fixes
chore/       - Maintenance tasks
docs/        - Documentation updates
```

---

## 2. SLIB Complete Business Flow

### 2.1 System Setup Flow (Admin)

```mermaid
flowchart TB
    subgraph ADMIN_SETUP["Phase 1: Admin System Setup"]
        direction TB
        A1[Admin Login] --> A2[Dashboard]
        
        subgraph LIBRARY_CONFIG["Library Configuration"]
            A2 --> A3[Create Library Settings]
            A3 --> A4["Set Operating Hours<br/>Opening: 07:00<br/>Closing: 22:00"]
            A4 --> A5["Set Booking Rules<br/>- Max duration: 4h<br/>- Advance booking: 7 days<br/>- Cancel before: 30min"]
        end
        
        subgraph AREA_CONFIG["Area Management"]
            A5 --> B1[Create Areas]
            B1 --> B2["Area: Tang 1 - Khu Doc Sach"]
            B1 --> B3["Area: Tang 2 - Phong Hoc Nhom"]
            B1 --> B4["Area: Tang 3 - Khu Yên Tinh"]
        end
        
        subgraph ZONE_CONFIG["Zone Configuration"]
            B2 --> C1[Create Zones in Area]
            C1 --> C2["Zone A: Ban Doc Ca Nhan<br/>Capacity: 50 seats"]
            C1 --> C3["Zone B: Ban Doc Doi<br/>Capacity: 30 seats"]
            C1 --> C4["Zone C: Phong Hop Nhom<br/>Capacity: 20 seats"]
        end
        
        subgraph SEAT_CONFIG["Seat Setup"]
            C2 --> D1[Add Seats to Zone]
            D1 --> D2["Seat A-01 to A-50<br/>Type: Individual"]
            D1 --> D3["Seat B-01 to B-30<br/>Type: Paired"]
            D1 --> D4["Room C-01 to C-05<br/>Type: Group Room"]
        end
        
        subgraph AMENITY_CONFIG["Amenities"]
            D2 --> E1[Add Amenities]
            E1 --> E2["Power Outlet"]
            E1 --> E3["WiFi Access"]
            E1 --> E4["Reading Lamp"]
            E1 --> E5["Air Conditioning"]
        end
    end
    
    E5 --> F1[Publish Configuration]
    F1 --> F2[System Ready for Students]
    
    style ADMIN_SETUP fill:#E3F2FD
    style F2 fill:#C8E6C9,stroke:#2E7D32,stroke-width:2px
```

### 2.2 User Import Flow (Admin)

```mermaid
flowchart LR
    subgraph IMPORT_FLOW["User Import Process"]
        I1[Prepare Excel File] --> I2["Columns:<br/>- Ma So<br/>- Ho Ten<br/>- Email<br/>- Vai Tro"]
        I2 --> I3[Upload to Admin Panel]
        I3 --> I4{Validate Data}
        I4 -->|Valid| I5[Preview Import]
        I4 -->|Invalid| I6[Show Errors]
        I6 --> I7[Fix Data]
        I7 --> I3
        I5 --> I8[Confirm Import]
        I8 --> I9[Create Users with<br/>Default Password]
        I9 --> I10[Send Welcome Email]
    end
    
    style I9 fill:#C8E6C9
    style I6 fill:#FFCDD2
```

### 2.3 Student Registration & Booking Flow

```mermaid
flowchart TB
    subgraph STUDENT_FLOW["Phase 2: Student Journey"]
        direction TB
        
        subgraph AUTH["Authentication"]
            S1[Open SLIB App] --> S2{Has Account?}
            S2 -->|No| S3[Register with FPT Email]
            S3 --> S4[Google OAuth Login]
            S2 -->|Yes| S4
            S4 --> S5[Verify @fpt.edu.vn Domain]
            S5 -->|Valid| S6[Access Granted]
            S5 -->|Invalid| S7[Registration Denied]
        end
        
        subgraph BROWSE["Browse Library"]
            S6 --> B1[Home Screen]
            B1 --> B2[View Available Areas]
            B2 --> B3[Select Area]
            B3 --> B4[View Zone Map]
            B4 --> B5[See Real-time Availability]
        end
        
        subgraph BOOKING["Booking Process"]
            B5 --> K1[Select Seat]
            K1 --> K2[Choose Time Slot]
            K2 --> K3[Review Booking Details]
            K3 --> K4{Confirm Booking}
            K4 -->|Yes| K5[Create Reservation<br/>Status: PROCESSING]
            K4 -->|No| B5
            K5 --> K6[Payment/Confirm]
            K6 --> K7[Status: BOOKED]
            K7 --> K8[Generate QR Code]
            K8 --> K9[Send Push Notification]
        end
        
        subgraph CHECKIN["Check-in Process"]
            K9 --> C1[Go to Library]
            C1 --> C2[Find Seat Location]
            C2 --> C3{Check-in Method}
            C3 -->|QR Code| C4[Scan QR at Seat]
            C3 -->|NFC| C5[Tap Phone on NFC Tag]
            C4 --> C6[Verify Reservation]
            C5 --> C6
            C6 -->|Match| C7[Status: CONFIRMED<br/>Seat: OCCUPIED]
            C6 -->|No Match| C8[Error: Wrong Seat]
        end
        
        subgraph USAGE["Usage & Checkout"]
            C7 --> U1[Study Session]
            U1 --> U2{Session Complete?}
            U2 -->|Yes| U3[Check-out]
            U2 -->|Extend| U4[Request Extension]
            U4 --> U5{Available?}
            U5 -->|Yes| U1
            U5 -->|No| U3
            U3 --> U6[Status: COMPLETED<br/>Seat: AVAILABLE]
            U6 --> U7[Update User Stats]
        end
    end
    
    style K5 fill:#FFF3E0
    style K7 fill:#E3F2FD
    style C7 fill:#C8E6C9
    style U6 fill:#F3E5F5
```

### 2.4 Librarian Operations Flow

```mermaid
flowchart TB
    subgraph LIBRARIAN_FLOW["Phase 3: Librarian Operations"]
        direction TB
        
        subgraph MONITOR["Real-time Monitoring"]
            L1[Librarian Login] --> L2[Dashboard Overview]
            L2 --> L3[View All Areas Status]
            L3 --> L4["Check Occupancy<br/>Zone A: 35/50<br/>Zone B: 20/30"]
            L4 --> L5[Monitor Check-ins]
        end
        
        subgraph MANAGE["Seat Management"]
            L5 --> M1{Issues Detected?}
            M1 -->|No Show| M2[Mark No-Show]
            M2 --> M3[Auto-release Seat<br/>after 30min]
            M3 --> M4[Issue Violation to User]
            
            M1 -->|Seat Problem| M5[Disable Seat]
            M5 --> M6[Log Maintenance Issue]
            M6 --> M7[Notify Admin]
            
            M1 -->|User Complaint| M8[Handle Complaint]
            M8 --> M9[Update Reservation Status]
        end
        
        subgraph REPORTS["Reporting"]
            M4 --> R1[View Daily Report]
            M7 --> R1
            M9 --> R1
            R1 --> R2[Export Statistics]
            R2 --> R3["- Total Bookings<br/>- Check-in Rate<br/>- Violations<br/>- Peak Hours"]
        end
    end
    
    style M4 fill:#FFCDD2
    style L4 fill:#E3F2FD
```

### 2.5 AI Chatbot Support Flow

```mermaid
flowchart LR
    subgraph AI_FLOW["AI Chatbot Support"]
        U1[User Opens Chat] --> U2[Type Question]
        U2 --> A1[AI Service Receives]
        A1 --> A2[RAG: Search Knowledge Base]
        A2 --> A3[Qdrant Vector Search]
        A3 --> A4[Retrieve Relevant Docs]
        A4 --> A5[Generate Response with LLM]
        A5 --> A6[Send to User]
        
        A6 --> U3{Satisfied?}
        U3 -->|Yes| U4[Rate Response]
        U3 -->|No| U5[Transfer to Librarian]
        U5 --> L1[Librarian Chat Panel]
        L1 --> L2[Human Response]
    end
    
    style A5 fill:#E8F5E9
    style L2 fill:#FFF3E0
```

### 2.6 Violation & Points System

```mermaid
flowchart TB
    subgraph VIOLATION["Violation Management"]
        V1[Violation Detected] --> V2{Type}
        
        V2 -->|No Show| V3["-10 Points"]
        V2 -->|Late Return| V4["-5 Points"]
        V2 -->|Noise| V5["-15 Points"]
        V2 -->|Damage| V6["-20 Points"]
        
        V3 --> V7[Record Violation]
        V4 --> V7
        V5 --> V7
        V6 --> V7
        
        V7 --> V8[Notify User]
        V8 --> V9{Points < 50?}
        V9 -->|Yes| V10[Restrict Booking<br/>for 7 days]
        V9 -->|No| V11[Normal Access]
        
        V10 --> V12[User Appeals?]
        V12 -->|Yes| V13[Admin Review]
        V13 --> V14{Approved?}
        V14 -->|Yes| V15[Restore Points]
        V14 -->|No| V10
    end
    
    style V10 fill:#FFCDD2
    style V15 fill:#C8E6C9
```

---

## 3. System Architecture Overview

```mermaid
flowchart TB
    subgraph CLIENT["Client Layer"]
        M[Mobile App<br/>Flutter]
        W[Web Admin<br/>React + Vite]
    end
    
    subgraph API["API Gateway"]
        BE[Spring Boot Backend<br/>:8080/slib]
    end
    
    subgraph SERVICES["Microservices"]
        AI[AI Service<br/>FastAPI + LangChain]
        VEC[Qdrant Vector DB]
    end
    
    subgraph DATA["Data Layer"]
        DB[(PostgreSQL<br/>Main Database)]
        CLOUD[Cloudinary<br/>Image Storage]
        FCM[Firebase FCM<br/>Push Notifications]
    end
    
    M --> BE
    W --> BE
    BE --> AI
    AI --> VEC
    BE --> DB
    BE --> CLOUD
    BE --> FCM
    
    style BE fill:#E3F2FD
    style AI fill:#E8F5E9
    style DB fill:#FFF3E0
```

---

## 4. Status Codes Reference

### Reservation Status
| Status | Description | Color |
|--------|-------------|-------|
| `PROCESSING` | Booking created, awaiting confirmation | Orange |
| `BOOKED` | Confirmed, waiting for check-in | Blue |
| `CONFIRMED` | User checked-in | Green |
| `COMPLETED` | Session finished | Gray |
| `CANCELLED` | User cancelled | Red |
| `EXPIRED` | No-show after timeout | Dark Red |

### Seat Status
| Status | Description | Color |
|--------|-------------|-------|
| `AVAILABLE` | Ready for booking | Green |
| `HOLDING` | Temporarily reserved | Yellow |
| `BOOKED` | Reserved for time slot | Blue |
| `OCCUPIED` | Currently in use | Orange |
| `MAINTENANCE` | Under repair | Gray |

---

## File Locations

- Diagrams Source: `doc/SLIB_Diagrams.md`
- Generated images will be in `doc/` folder
