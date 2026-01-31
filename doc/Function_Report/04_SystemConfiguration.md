# Module 04: System Configuration

## 4.1 Area Management

### FE-20: View area map

- **Actors:** Admin
- **Navigation path:** Admin Sidebar -> "Bản đồ thư viện"
- **Purpose:** View library area floor plan
- **Interface:** Canvas displaying areas with coordinates, names, status (shows "X Khu vực" count)
- **Data:** areaId, name, position, status, zonesCount
- **Business rules:** Real-time update when changes occur

### FE-21: CRUD area

- **Actors:** Admin
- **Purpose:** Create/Read/Update/Delete areas ("+ Khu vực" button)
- **Interface:** Form with name, description, position, status
- **Validation:** Name must be unique, valid position
- **Business rules:** Deleting area will delete all zones and seats inside

### FE-22: Change area status

- **Actors:** Admin
- **Purpose:** Change area status (Hoạt động/Không hoạt động/Bảo trì)
- **Interface:** Status dropdown in area details
- **Business rules:** Inactive areas don't allow booking

### FE-23: Lock area movement

- **Actors:** Admin
- **Purpose:** Lock/unlock area dragging on canvas
- **Interface:** Lock icon on each area
- **Business rules:** Locked areas cannot be dragged

---

## 4.2 Zone Management

### FE-24: View zone map

- **Actors:** Admin
- **Purpose:** View zone layout within an area
- **Interface:** Canvas with zones ("Khu vực ghế"), colored by zone type
- **Data:** zoneId, name, type, capacity, currentOccupancy

### FE-25: CRUD zone

- **Actors:** Admin
- **Purpose:** Create/Read/Update/Delete zones
- **Interface:** Form with name, type (Quiet/Discussion/Self-study), capacity
- **Placeholder:** "VD: Khu vực yên tĩnh, không nói chuyện..."
- **Validation:** Capacity > 0, unique name within area

### FE-26: CRUD zone attribute

- **Actors:** Admin
- **Purpose:** Manage zone attributes (amenities, rules)
- **Interface:** Attribute list with add/edit/remove
- **Data:** hasWifi, hasPowerOutlet, hasAircon, noiseLevel

### FE-27: View zone details

- **Actors:** Admin
- **Purpose:** View zone details with seat list
- **Interface:** Panel with info, stats, seat grid
- **Data:** Zone info, occupancy rate, seats list

### FE-28: Lock zone movement

- **Actors:** Admin
- **Purpose:** Lock/unlock zone dragging
- **Interface:** Lock icon
- **Business rules:** Locked zones cannot be dragged

---

## 4.3 Seat Management

### FE-29: View seat map

- **Actors:** Admin, Librarian
- **Navigation path:** Librarian Sidebar -> "Quản lý chỗ ngồi"
- **Purpose:** View real-time seat map with status
- **Interface:** Seat grid with color-coded status (shows "Chỗ ngồi đã có người" / "Chỗ ngồi đang được sử dụng")
- **Legend:** Available (green), Booked (yellow), Occupied (red), Maintenance (gray)

### FE-30: CRUD seat

- **Actors:** Admin
- **Purpose:** Create/Read/Update/Delete seats ("Ghế / Chỗ ngồi" option)
- **Interface:** Click on grid to add, context menu to edit/delete
- **Validation:** Unique seat code (A1, A2, B1...)

### FE-31: Change seat status

- **Actors:** Admin, Librarian
- **Purpose:** Manually change seat status
- **Interface:** Context menu or details panel
- **Business rules:** Maintenance status cancels active bookings

---

## 4.4 Reputation Rule Management

### FE-32: View list of reputation rules

- **Actors:** Admin
- **Purpose:** View list of reputation scoring rules
- **Interface:** Table with name, description, points, status
- **Data:** ruleId, name, description, points (+ or -), isActive

### FE-33: CRUD reputation rule

- **Actors:** Admin
- **Purpose:** Create/Update/Delete point rules
- **Interface:** Form with rule name, description, trigger condition
- **Validation:** Points must be integer

### FE-34: Set the deducted point for each reputation rule

- **Actors:** Admin
- **Purpose:** Set points deducted/added for each rule
- **Interface:** Points input in rule details
- **Examples:**
    - Check-in đúng giờ: +5 điểm
    - Không đến (No-show): -10 điểm
    - Vi phạm: -15 điểm

---

## 4.5 Library Configuration Management

### FE-35: Set library operating hours

- **Actors:** Admin
- **Navigation path:** Admin Sidebar -> "Cấu hình hệ thống"
- **Purpose:** Set library open/close hours
- **Interface:** Time pickers for open/close time per day
- **Validation:** Close time > Open time
- **Business rules:** No booking outside operating hours

### FE-36: Configure booking rules

- **Actors:** Admin
- **Purpose:** Configure booking rules
- **Interface:** Form with fields:
    - Slot duration: 30-180 minutes
    - Max bookings per day: 1-10
    - Advance booking days: 1-7
    - Cancel before: 15-60 minutes

### FE-37: Turn on/Turn off automatic check-out when time exceeds

- **Actors:** Admin
- **Purpose:** Enable/disable auto check-out when overtime
- **Interface:** Toggle switch
- **Business rules:** If enabled, system auto-frees seat after X minutes overtime

### FE-38: Enable/Disable Library

- **Actors:** Admin
- **Purpose:** Close/open library (holidays, maintenance)
- **Interface:** Toggle with reason field
- **Business rules:** Disabled library cancels all bookings and notifies users

---

## 4.6 HCE Device Management

### FE-39: View list of HCE devices

- **Actors:** Admin
- **Navigation path:** Admin Sidebar -> "Quản lý thiết bị"
- **Purpose:** View list of NFC reader devices (title: "Quản lý thiết bị", subtitle: "Quản lý các đầu đọc NFC và thiết bị IoT trong thư viện")
- **Interface:** Table with device info and status
- **Data:** deviceId, name, location, zoneMapping, status, lastPing

### FE-40: CRUD HCE device

- **Actors:** Admin
- **Purpose:** Add/Update/Delete devices
- **Interface:** Form with device info and zone mapping (options: "Ghế / Chỗ ngồi", "Khu vực")
- **Validation:** Device ID must be unique

### FE-41: View HCE device details

- **Actors:** Admin
- **Purpose:** View device details and activity history
- **Interface:** Panel with device info, ping history, activity log

---

## 4.7 AI Configuration Management

### FE-42: CRUD materials

- **Actors:** Admin
- **Navigation path:** Admin Sidebar -> "Cấu hình AI"
- **Purpose:** Manage documents for AI knowledge base
- **Interface:** Upload area, document list
- **Supported formats:** PDF, DOCX, TXT, MD

### FE-43: CRUD knowledge store

- **Actors:** Admin
- **Purpose:** Manage categories and tags for knowledge
- **Interface:** Category tree with add/edit/delete

### FE-44: Test AI chat

- **Actors:** Admin
- **Purpose:** Test AI assistant with new knowledge
- **Interface:** Chat interface for testing questions
- **Business rules:** Debug mode shows sources

---

## 4.8 Kiosk Management

### FE-45: View Kiosk homepage

- **Actors:** System (Kiosk display)
- **Purpose:** Kiosk main screen
- **Interface:** Welcome screen with QR code, current stats

### FE-46: Show QR code for check-in/check-out

- **Actors:** System
- **Purpose:** Display QR code for student check-in scanning
- **Interface:** Large QR code with instructions

### FE-47: View list of Kiosk images

- **Actors:** Admin
- **Purpose:** View slideshow images for Kiosk
- **Interface:** Gallery view with thumbnails

### FE-48: CRUD Kiosk image

- **Actors:** Admin
- **Purpose:** Upload/Edit/Delete slideshow images
- **Interface:** Upload area, edit metadata
- **Validation:** Image format: JPG/PNG, max 5MB

### FE-49: Change image status

- **Actors:** Admin
- **Purpose:** Enable/disable image display on Kiosk
- **Interface:** Toggle status

### FE-50: Preview Kiosk display

- **Actors:** Admin
- **Purpose:** Preview Kiosk interface
- **Interface:** Preview window with slideshow

---

## 4.9 Others

### FE-51: Config system notification

- **Actors:** Admin
- **Purpose:** Configure templates and triggers for system notifications
- **Interface:** Notification templates editor

### FE-52: View system overview information

- **Actors:** Admin
- **Navigation path:** Admin Sidebar -> "Tổng quan"
- **Purpose:** View system overview (version, uptime, memory)
- **Interface:** System info dashboard

### FE-53: View system log

- **Actors:** Admin
- **Purpose:** View system logs
- **Interface:** Log viewer with filters
- **Data:** Timestamp, level, message, source

### FE-54: Backup data manually

- **Actors:** Admin
- **Purpose:** Manual database backup
- **Interface:** Backup button with progress
- **Business rules:** Download backup file when complete

### FE-55: Set automatic backup schedule

- **Actors:** Admin
- **Purpose:** Set automatic backup schedule
- **Interface:** Schedule picker (daily/weekly)

---

