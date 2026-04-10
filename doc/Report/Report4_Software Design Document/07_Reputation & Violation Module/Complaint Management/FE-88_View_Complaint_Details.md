# FE-88 View Complaint Details

```mermaid
sequenceDiagram
    participant Users as "Librarian"
    participant WebPortal as Librarian Web Portal

    Users->>WebPortal: 1. Select a complaint from the list
    activate Users
    activate WebPortal
    WebPortal->>WebPortal: 2. Read detail from the already loaded list
    WebPortal-->>Users: 3. Show complaint details (sender, content, status)
    deactivate WebPortal
    deactivate Users
```

