# FE-05 View Profile

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Web Portal / Mobile App
    participant StudentProfileController as StudentProfileController
    participant StudentProfileService as StudentProfileService
    participant StudentProfileRepo as StudentProfileRepository
    participant ReservationRepo as ReservationRepository
    participant DB as Database

    Users->>Client: 1. Open profile screen
    activate Users
    activate Client
    Client->>Client: 1.1 Load cached session profile for quick display

    alt 2a. Fresh profile synchronization is required
        Client->>StudentProfileController: 2a.1 GET /slib/student-profile/me
        deactivate Client
        activate StudentProfileController
        StudentProfileController->>StudentProfileService: 2a.2 getOrCreateProfile(currentUser)
        deactivate StudentProfileController
        activate StudentProfileService
        StudentProfileService->>StudentProfileRepo: 2a.3 Find profile by userId
        activate StudentProfileRepo
        StudentProfileRepo->>DB: 2a.4 Query student_profiles
        activate DB
        DB-->>StudentProfileRepo: 2a.5 Return existing profile or empty result
        deactivate DB

        alt 3a. Profile already exists
            StudentProfileRepo-->>StudentProfileService: 3a.1 Return existing profile
            deactivate StudentProfileRepo
        else 3b. Profile does not exist yet
            StudentProfileRepo-->>StudentProfileService: 3b.1 Return empty result
            StudentProfileService->>StudentProfileService: 3b.2 Build default profile with reputation and counters
            StudentProfileService->>StudentProfileRepo: 3b.3 Save new profile
            StudentProfileRepo->>DB: 3b.4 Insert student_profiles record
            activate DB
            DB-->>StudentProfileRepo: 3b.5 Persist success
            deactivate DB
            StudentProfileRepo-->>StudentProfileService: 3b.6 Return saved profile
            deactivate StudentProfileRepo
        end

        StudentProfileService->>ReservationRepo: 4. Count bookings and total study minutes
        activate ReservationRepo
        ReservationRepo->>DB: 4.1 Query reservation summary
        activate DB
        DB-->>ReservationRepo: 4.2 Return booking count and study minutes
        deactivate DB
        ReservationRepo-->>StudentProfileService: 4.3 Return aggregated statistics
        deactivate ReservationRepo
        StudentProfileService->>StudentProfileService: 5. Build StudentProfileResponse
        StudentProfileService-->>StudentProfileController: 6. Return profile response
        deactivate StudentProfileService
        activate StudentProfileController
        StudentProfileController-->>Client: 7. Return 200 OK with profile and statistics
        deactivate StudentProfileController
        activate Client

        alt 8a. User is on Web Portal
            Client->>Client: 8a.1 Merge API profile into Account Settings screen
            Client-->>Users: 8a.2 Show profile information for Admin or Librarian
        else 8b. User is on Mobile App
            Client->>Client: 8b.1 Merge API profile into Profile Info screen
            Client-->>Users: 8b.2 Show profile information for Student or Teacher
        end
    else 2b. Cached profile is still usable
        Client-->>Users: 2b.1 Render profile from local session state
    end

    deactivate Client
    deactivate Users
```
