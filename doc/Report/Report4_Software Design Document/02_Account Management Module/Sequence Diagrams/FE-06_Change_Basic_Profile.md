# FE-06 Change Basic Profile

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Web Portal / Mobile App
    participant StudentProfileController as StudentProfileController
    participant StudentProfileService as StudentProfileService
    participant UserRepo as UserRepository
    participant StudentProfileRepo as StudentProfileRepository
    participant DB as Database

    Users->>Client: 1. Open edit profile form
    activate Users
    activate Client
    Client->>Client: 1.1 Modify full name, phone number, or date of birth
    Client->>Client: 2. Validate required fields and local format

    alt 3a. User submits from Web Portal
        Client->>StudentProfileController: 3a.1 PUT /slib/student-profile/me with fullName, phone, dob
    else 3b. User submits from Mobile App
        Client->>StudentProfileController: 3b.1 PUT /slib/student-profile/me with phone and dob
    end

    deactivate Client
    activate StudentProfileController
    StudentProfileController->>StudentProfileService: 4. updateUserInfo(userId, request)
    deactivate StudentProfileController
    activate StudentProfileService
    StudentProfileService->>UserRepo: 5. Find current user by userId
    activate UserRepo
    UserRepo->>DB: 5.1 Query users table
    activate DB
    DB-->>UserRepo: 5.2 Return current user record
    deactivate DB

    alt 6a. Request data is invalid or phone number is duplicated
        UserRepo-->>StudentProfileService: 6a.1 Return conflicting user state
        deactivate UserRepo
        StudentProfileService->>StudentProfileService: 6a.2 Reject update request
        StudentProfileService-->>StudentProfileController: 6a.3 Return validation error
        deactivate StudentProfileService
        activate StudentProfileController
        StudentProfileController-->>Client: 6a.4 Return 400 Bad Request
        deactivate StudentProfileController
        activate Client
        Client-->>Users: 6a.5 Show profile update failed message
    else 6b. Request data is valid
        UserRepo-->>StudentProfileService: 6b.1 Return editable user record
        StudentProfileService->>StudentProfileService: 6b.2 Normalize full name, phone, and dob
        StudentProfileService->>UserRepo: 6b.3 Save updated user information
        UserRepo->>DB: 6b.4 Update users table
        activate DB
        DB-->>UserRepo: 6b.5 Persist success
        deactivate DB
        deactivate UserRepo
        StudentProfileService->>StudentProfileRepo: 7. Get or create student profile for response
        activate StudentProfileRepo
        StudentProfileRepo->>DB: 7.1 Query student_profiles by userId
        activate DB
        DB-->>StudentProfileRepo: 7.2 Return existing profile or empty result
        deactivate DB

        alt 8a. Student profile already exists
            StudentProfileRepo-->>StudentProfileService: 8a.1 Return existing profile
            deactivate StudentProfileRepo
        else 8b. Student profile does not exist yet
            StudentProfileRepo-->>StudentProfileService: 8b.1 Return empty result
            StudentProfileService->>StudentProfileRepo: 8b.2 Save default student profile
            StudentProfileRepo->>DB: 8b.3 Insert student_profiles record
            activate DB
            DB-->>StudentProfileRepo: 8b.4 Persist success
            deactivate DB
            StudentProfileRepo-->>StudentProfileService: 8b.5 Return saved profile
            deactivate StudentProfileRepo
        end

        StudentProfileService->>StudentProfileService: 9. Build StudentProfileResponse
        StudentProfileService-->>StudentProfileController: 10. Return updated profile response
        deactivate StudentProfileService
        activate StudentProfileController
        StudentProfileController-->>Client: 11. Return 200 OK with updated profile
        deactivate StudentProfileController
        activate Client

        alt 12a. User is on Web Portal
            Client->>Client: 12a.1 Refresh Account Settings state and stored session
            Client-->>Users: 12a.2 Show profile update success message
        else 12b. User is on Mobile App
            Client->>Client: 12b.1 Refresh AuthService state and profile screen
            Client-->>Users: 12b.2 Show profile update success message
        end
    end

    deactivate Client
    deactivate Users
```
