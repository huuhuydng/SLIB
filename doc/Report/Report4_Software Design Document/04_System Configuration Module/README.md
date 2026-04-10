# System Configuration Module

This folder contains the Report 4 diagrams for Module 4 - System Configuration.

## Actor Scope

- `Module 4`: `Admin`

## Included Artifacts

- Sequence diagrams grouped into:
  - `Area Management`
  - `Zone Management`
  - `Seat Management`
  - `Reputation Rule Management`
  - `Library Configuration Management`
  - `HCE Scan Station Management`
  - `AI Configuration Management`
  - `NFC Tag Management`
  - `Kiosk Management`
  - `Others`
- Class diagram for the System Configuration Module

## Split Rule for CRUD and Manage Features

For this module, every feature described as `CRUD` or `Manage` is split into:

- `FExxa`: View and Update
- `FExxb`: Create
- `FExxc`: Delete

This rule also applies to:

- `FE-43` Manage HCE station registration
- `FE-49` Manage NFC Tag UID mapping
- `FE-54` CRUD Kiosk device

## Feature Mapping

### Area Management

- `FE-22`: View area map
- `FE-23a`: View and Update area
- `FE-23b`: Create area
- `FE-23c`: Delete area
- `FE-24`: Change area status
- `FE-25`: Lock area movement

### Zone Management

- `FE-26`: View zone map
- `FE-27a`: View and Update zone
- `FE-27b`: Create zone
- `FE-27c`: Delete zone
- `FE-28a`: View and Update zone attribute
- `FE-28b`: Create zone attribute
- `FE-28c`: Delete zone attribute
- `FE-29`: View zone details
- `FE-30`: Lock zone movement

### Seat Management

- `FE-31`: View seat map
- `FE-32a`: View and Update seat
- `FE-32b`: Create seat
- `FE-32c`: Delete seat
- `FE-33`: Change seat status

### Reputation Rule Management

- `FE-34`: View list of reputation rules
- `FE-35a`: View and Update reputation rule
- `FE-35b`: Create reputation rule
- `FE-35c`: Delete reputation rule
- `FE-36`: Set deducted point for each reputation rule

### Library Configuration Management

- `FE-37`: Set library operating hours
- `FE-38`: Configure booking rules
- `FE-39`: Setting time for automatic check-out when time exceeds
- `FE-40`: Enable or disable library

### HCE Scan Station Management

- `FE-41`: View HCE scan stations
- `FE-42`: View HCE scan station details
- `FE-43a`: View and Update HCE station registration
- `FE-43b`: Create HCE station registration
- `FE-43c`: Delete HCE station registration

### AI Configuration Management

- `FE-44a`: View and Update material
- `FE-44b`: Create material
- `FE-44c`: Delete material
- `FE-45`: View list of materials
- `FE-46a`: View and Update knowledge store
- `FE-46b`: Create knowledge store
- `FE-46c`: Delete knowledge store
- `FE-47`: View list of knowledge stores
- `FE-48`: Test AI chat

### NFC Tag Management

- `FE-49a`: View and Update NFC Tag UID mapping
- `FE-49b`: Create NFC Tag UID mapping
- `FE-49c`: Delete NFC Tag UID mapping
- `FE-50`: View NFC Tag mapping list
- `FE-51`: View NFC Tag mapping details

### Kiosk Management

- `FE-52`: View list of kiosk devices
- `FE-53`: View kiosk device details
- `FE-54a`: View and Update kiosk device
- `FE-54b`: Create kiosk device
- `FE-54c`: Delete kiosk device
- `FE-55`: Activate kiosk device

### Others

- `FE-56`: Config system notification
- `FE-57`: View system overview information
- `FE-58`: View system log
- `FE-59`: Backup data manually
- `FE-60`: Set automatic backup schedule

## Current Working Assumptions

- Diagrams are based on the current project implementation first.
- When the feature breakdown and current code differ slightly, the sequence diagrams prioritize the current code flow while keeping the FE numbering above.
- `Kiosk Management` in this module now follows the current device administration implementation through `KioskAdminController`, `KioskTokenService`, `KioskConfigRepository`, and `KioskManagement.jsx`.
- `Kiosk image` and `slideshow preview` flows are no longer treated as Module 4 baseline artifacts. They have been moved into Module 10 because the current code separates kiosk device administration from kiosk slideshow content management.
- `Zone attribute` flows are mapped to the current amenity-based implementation through `AmenityController`.
- `Lock zone movement` is reflected through the current zone update flow using the `isLocked` field.
