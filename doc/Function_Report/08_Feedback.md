# Module 08: Feedback

## 8.1 Feedback System Management

### FE-85: Create feedback after check-out (booking seat time expires)

- **Actors:** Student
- **Purpose:** Submit feedback after seat usage
- **Interface:** Rating stars + comment form (popup after check-out)
- **Data:** rating (1-5), comment, bookingId
- **Timing:** Displayed immediately after check-out

### FE-86: View list of feedbacks

- **Actors:** Librarian
- **Purpose:** View all feedback
- **Interface:** Table with rating filter
- **Data:** Feedback list with student, rating, date

### FE-87: View feedback details

- **Actors:** Librarian
- **Purpose:** View feedback details
- **Interface:** Detail modal
- **Data:** Full feedback with context (seat, zone, time)

### FE-88: Categorize feedback using AI

- **Actors:** System (AI), Librarian review
- **Purpose:** Auto-categorize feedback using AI
- **Interface:** AI-suggested categories, Librarian confirms
- **Categories:** Facility, Service, Noise, Comfort, Other

---

## 8.2 Seat Status Management

### FE-89: Create seat status report

- **Actors:** Student
- **Purpose:** Report seat condition (broken, dirty...)
- **Interface:** Form with seat selection, issue type, photo
- **Data:** seatId, issueType, description, photos

### FE-90: View history of sending seat status report

- **Actors:** Student
- **Purpose:** View sent report history
- **Interface:** List with status tracking
- **Data:** Reports with status (Pending, In Progress, Resolved)

### FE-91: View list of seat status reports

- **Actors:** Librarian
- **Purpose:** View all seat status reports
- **Interface:** Table with priority sorting
- **Data:** All reports with location, issue, urgency

### FE-92: View seat status report details

- **Actors:** Librarian
- **Purpose:** View report details
- **Interface:** Detail panel with photos
- **Data:** Full report with media attachments

### FE-93: Verify seat status report

- **Actors:** Librarian
- **Purpose:** Confirm and process report
- **Interface:** Action buttons + status update
- **Actions:** Mark as fixed, Set maintenance, Assign to staff

---

## 8.3 Report Seat Violation Management

### FE-94: Create report seat violation

- **Actors:** Student
- **Purpose:** Report another student's violation
- **Interface:** Form with seat, violation type, evidence
- **Data:** seatId, violationType, description, photos

### FE-95: View history of sending report seat violation

- **Actors:** Student
- **Purpose:** View sent violation report history
- **Interface:** List with status
- **Data:** Reports with verification status

### FE-96: View list of seat violation reports

- **Actors:** Librarian
- **Purpose:** View violation report list
- **Interface:** Table with filters
- **Data:** Reports with alleged violator info

### FE-97: View report seat violation details

- **Actors:** Librarian
- **Purpose:** View violation report details
- **Interface:** Detail panel
- **Data:** Full report, reporter info, evidence

### FE-98: Verify seat violation report

- **Actors:** Librarian
- **Purpose:** Verify and process violation report
- **Interface:** Investigation panel with actions
- **Actions:**
    - Confirm violation: Deduct violator points
    - Reject: Dismiss report
    - Need more info: Request from reporter

---

