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

- `FE-42` Manage HCE station registration
- `FE-48` Manage NFC Tag UID mapping

## Feature Mapping

### Area Management

- `FE-21`: View area map
- `FE-22a`: View and Update area
- `FE-22b`: Create area
- `FE-22c`: Delete area
- `FE-23`: Change area status
- `FE-24`: Lock area movement

### Zone Management

- `FE-25`: View zone map
- `FE-26a`: View and Update zone
- `FE-26b`: Create zone
- `FE-26c`: Delete zone
- `FE-27a`: View and Update zone attribute
- `FE-27b`: Create zone attribute
- `FE-27c`: Delete zone attribute
- `FE-28`: View zone details
- `FE-29`: Lock zone movement

### Seat Management

- `FE-30`: View seat map
- `FE-31a`: View and Update seat
- `FE-31b`: Create seat
- `FE-31c`: Delete seat
- `FE-32`: Change seat status

### Reputation Rule Management

- `FE-33`: View list of reputation rules
- `FE-34a`: View and Update reputation rule
- `FE-34b`: Create reputation rule
- `FE-34c`: Delete reputation rule
- `FE-35`: Set deducted point for each reputation rule

### Library Configuration Management

- `FE-36`: Set library operating hours
- `FE-37`: Configure booking rules
- `FE-38`: Turn on/Turn off automatic check-out when time exceeds
- `FE-39`: Enable/Disable library

### HCE Scan Station Management

- `FE-40`: View HCE scan stations
- `FE-41`: View HCE scan station details
- `FE-42a`: View and Update HCE station registration
- `FE-42b`: Create HCE station registration
- `FE-42c`: Delete HCE station registration

### AI Configuration Management

- `FE-43a`: View and Update material
- `FE-43b`: Create material
- `FE-43c`: Delete material
- `FE-44`: View list of materials
- `FE-45a`: View and Update knowledge store
- `FE-45b`: Create knowledge store
- `FE-45c`: Delete knowledge store
- `FE-46`: View list of knowledge stores
- `FE-47`: Test AI chat

### NFC Tag Management

- `FE-48a`: View and Update NFC Tag UID mapping
- `FE-48b`: Create NFC Tag UID mapping
- `FE-48c`: Delete NFC Tag UID mapping
- `FE-49`: View NFC Tag mapping list
- `FE-50`: View NFC Tag mapping details

### Kiosk Management

- `FE-51`: View list of kiosk images or kiosk slideshow assets
- `FE-52a`: View and Update kiosk image
- `FE-52b`: Create kiosk image
- `FE-52c`: Delete kiosk image
- `FE-53`: Change image status
- `FE-54`: Preview kiosk display

### Others

- `FE-55`: Config system notification
- `FE-56`: View system overview information
- `FE-57`: View system log
- `FE-58`: Backup data manually
- `FE-59`: Set automatic backup schedule

## Current Working Assumptions

- Diagrams are based on the current project implementation first.
- When the feature breakdown and current code differ slightly, the sequence diagrams will prioritize the current code flow while keeping the FE numbering above.
- `Kiosk Management` in this module follows the current slideshow image implementation through `KioskSlideshowController`, because that is the implementation that matches `FE-51` to `FE-54`.
- `Zone attribute` flows are mapped to the current amenity-based implementation through `AmenityController`.
- `Lock zone movement` is reflected through the current zone update flow using the `isLocked` field.
