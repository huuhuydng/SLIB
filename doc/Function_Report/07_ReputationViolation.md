# Module 07: Reputation & Violation

## 7.1 Reputation Score Management

### FE-75: View reputation score

- **Actors:** Student
- **Navigation path:** Home -> Profile section (shows stats with reputation points)
- **Purpose:** View personal reputation score
- **Interface:** Score card with progress bar, history link
- **Data:** currentScore, maxScore, level, trend

### FE-76: View history of changed reputation points

- **Actors:** Student
- **Navigation path:** "Tài khoản & Cài đặt" -> "Lịch sử vi phạm"
- **Purpose:** View point change history (title: "Lịch sử vi phạm")
- **Interface:** Timeline with each point change
- **Data:** Date, reason, points (+/-), balance

### FE-77: View detailed reason for deducting point

- **Actors:** Student
- **Purpose:** View detailed deduction reason
- **Interface:** Detail modal with evidence if any
- **Data:** Violation type, date, evidence, deducted points

### FE-78: View list of Students violation

- **Actors:** Librarian
- **Navigation path:** Sidebar -> Violation management
- **Purpose:** View list of students with violations
- **Interface:** Table with filters
- **Data:** Student list, violation count, current score

### FE-79: View Student violation details

- **Actors:** Librarian
- **Purpose:** View student violation details
- **Interface:** Student profile with violation history
- **Data:** Student info, violation list, total deductions

---

## 7.2 Complaint Management

### FE-80: Create complaint

- **Actors:** Student
- **Purpose:** Create complaint about point deduction ("Khiếu nại")
- **Interface:** Form with violation selection, reason, evidence
- **Data:** violationId, reason, attachments

### FE-81: View history of sending complaint

- **Actors:** Student
- **Purpose:** View sent complaint history
- **Interface:** List with status (Đang chờ/Đã duyệt/Từ chối)
- **Data:** Complaint list with status

### FE-82: View list of complaints

- **Actors:** Librarian
- **Purpose:** View all complaints
- **Interface:** Table with priority sorting
- **Data:** All complaints with student info

### FE-83: View complaint details

- **Actors:** Librarian
- **Purpose:** View complaint details
- **Interface:** Detail panel with evidence
- **Data:** Complaint info, student info, violation info, attachments

### FE-84: Verify complaint

- **Actors:** Librarian
- **Purpose:** Verify and process complaint
- **Interface:** Action buttons ("Duyệt"/"Từ chối") with reason
- **Business rules:**
    - Approve: Refund points
    - Reject: Keep deduction, send notification

---

