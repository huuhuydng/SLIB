# README_USECASE

## 3.5.3.1 View seat map
Function trigger
Navigation path: Admin portal -> Library Map -> Select area/zone
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View visual seat layout for management and monitoring
Interface:
Canvas map: Shows areas, zones, seats, and obstacles
Toolbar: Zoom, pan, fit-to-view, preview/edit mode
Data processing:
System loads area and zone data
System fetches seats by zone and renders positions
Function details
Data: areaId, zoneId, seatId, seatCode, rowNumber, columnNumber, positionX, positionY, width, height, isActive
Validation:
Zone and seat references must be valid
Business rules:
View actions do not persist data
Normal case: Seat map displays correctly
Abnormal case:
No data: Show empty map state
Load error: Partial map or error message/log

## 3.5.3.2 Create seat
Function trigger
Navigation path: Admin portal -> Library Map -> Select zone -> Add seat
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Add new seat(s) into a selected zone
Interface:
Add seat control: Seat quantity/row options
Save button: Persist changes
Data processing:
Admin chooses target zone and seat parameters
System creates seat in local layout state
Admin clicks Save
System sends POST /slib/seats for each new seat
Function details
Data: zoneId, seatCode, rowNumber, columnNumber, seatStatus
Validation:
Zone must exist
seatCode should be non-empty and unique in zone context
Business rules:
New seats are pending until Save is clicked
Normal case: Seat is created and appears on map
Abnormal case:
Invalid zone or payload: Create fails and error is returned

## 3.5.3.3 Update seat
Function trigger
Navigation path: Admin portal -> Library Map -> Select seat -> Properties panel -> Save
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Update seat attributes
Interface:
Seat properties: seatCode, active status, position/dimension controls
Save button: Persist updates
Data processing:
Admin edits seat information
System updates UI state immediately
Admin clicks Save
System sends PUT /slib/seats/{id}
Function details
Data: seatId, seatCode, rowNumber, columnNumber, positionX, positionY, width, height, isActive
Validation:
Seat must exist
Business rules:
Changes are batched and persisted on Save
Normal case: Seat data updated successfully
Abnormal case:
Seat not found: Update fails with backend error

## 3.5.3.4 Delete seat
Function trigger
Navigation path: Admin portal -> Library Map -> Select seat -> Delete -> Confirm
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove seat from zone layout
Interface:
Delete action in properties panel
Confirmation dialog
Save button for final persistence
Data processing:
Admin confirms delete
System removes seat from UI and marks pending delete
Admin clicks Save
System sends DELETE /slib/seats/{id}
Function details
Data: seatId, zoneId
Validation:
Seat must exist before deletion
Business rules:
Delete is final after Save
Normal case: Seat removed from layout and database
Abnormal case:
Seat not found: Delete endpoint returns not found/error

## 3.5.3.5 Change seat
Function trigger
Navigation path: Admin portal -> Library Map -> Select seat -> Drag/Resize or edit seat code
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Change seat placement or identity in the map
Interface:
Drag and resize interactions on canvas
Seat properties for code/row/column edits
Save button
Data processing:
Admin changes seat position/size or code
System updates map preview immediately
Admin clicks Save
System sends PUT /slib/seats/{id} with changed fields
Function details
Data: seatId, seatCode, rowNumber, columnNumber, positionX, positionY, width, height
Validation:
Seat changes must stay within valid layout constraints
Business rules:
Unsaved changes remain local until Save
Normal case: Seat change is persisted and reflected on reload
Abnormal case:
Invalid data or conflict: Backend rejects update

## 3.5.4.1 View list of reputation rule
Function trigger
Navigation path: Admin portal -> Reputation Rules
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View all reputation rules and deducted points configuration
Interface:
Rules table: ruleCode, ruleName, description, points, ruleType, isActive
Data processing:
System requests GET /slib/admin/reputation-rules
System displays returned list
Function details
Data: ruleId, ruleCode, ruleName, description, points, ruleType, isActive
Validation:
Admin authorization required
Business rules:
Only ADMIN can access this module
Normal case: Full rule list loads successfully
Abnormal case:
Unauthorized: Access denied response

## 3.5.4.2 Create reputation rule
Function trigger
Navigation path: Admin portal -> Reputation Rules -> Create Rule
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Add a new reputation rule for violation/reward logic
Interface:
Create form: ruleCode, ruleName, description, points, ruleType, isActive
Create button
Data processing:
Admin submits new rule form
System validates required fields and uniqueness of ruleCode
System sends POST /slib/admin/reputation-rules
Backend creates rule and returns created rule data
Function details
Data: ruleCode, ruleName, description, points, ruleType, isActive
Validation:
ruleCode must be unique
ruleName must not be empty
points must be numeric
ruleType must be valid
Business rules:
Duplicate ruleCode is rejected
Only ADMIN can perform this action
Normal case: Rule is created and appears in list immediately
Abnormal case:
Duplicate code: Backend returns Bad Request (400)
Unauthorized role: Access denied (403)
Invalid payload: Backend returns validation error

## 3.5.4.3 Update reputation rule
Function trigger
Navigation path: Admin portal -> Reputation Rules -> Select rule -> Edit
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Update existing reputation rule fields
Interface:
Edit form and save action
Data processing:
Admin edits a rule
System sends PUT /slib/admin/reputation-rules/{id}
Backend updates changed fields and returns updated rule
Function details
Data: ruleId, ruleName, description, points, ruleType, isActive
Validation:
Rule must exist
points must be numeric when provided
ruleType must be valid when provided
Business rules:
Partial updates are supported
ruleCode is not changed in update flow
Only ADMIN can perform this action
Normal case: Rule is updated successfully
Abnormal case:
Rule not found: 404 response
Unauthorized role: Access denied (403)
Invalid points/ruleType: Backend rejects update

## 3.5.4.4 Delete reputation rule
Function trigger
Navigation path: Admin portal -> Reputation Rules -> Select rule -> Delete
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove a reputation rule no longer needed
Interface:
Delete action and confirmation
Data processing:
Admin confirms delete
System sends DELETE /slib/admin/reputation-rules/{id}
Backend removes the rule and returns no-content response
Function details
Data: ruleId
Validation:
Rule must exist
Business rules:
Only ADMIN can delete rules
Normal case: Rule removed from list and no longer returned by GET list
Abnormal case:
Rule not found: 404 response
Unauthorized role: Access denied (403)

## 3.5.4.5 Set deducted point for each rule
Function trigger
Navigation path: Admin portal -> Reputation Rules -> Edit rule -> Points field
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Configure deducted points value per reputation rule
Interface:
Points input in create/edit rule form
Save button
Data processing:
Admin enters points value
System submits POST or PUT to /slib/admin/reputation-rules
Backend persists points in the selected rule
Function details
Data: ruleId, points
Validation:
Points must be numeric and follow policy limits
Business rules:
Points are persisted in the rule entity and used by violation processing
Point changes are applied per rule (not global)
Only ADMIN can modify deducted points
Normal case: Points updated and reflected in rule list/detail
Abnormal case:
Invalid points value: Request rejected
Rule not found (update mode): 404 response
Unauthorized role: Access denied (403)

## 3.5.5.1 Set library operating hours
Function trigger
Navigation path: Admin portal -> System Settings -> Library Settings
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Configure opening and closing time of the library
Interface:
Time fields: openTime, closeTime
Save settings button
Data processing:
Admin updates operating hours
System sends PUT /slib/settings/library
System can refresh time slots via GET /slib/settings/time-slots
Function details
Data: openTime, closeTime, slotDuration, workingDays
Validation:
Time format must be HH:mm
openTime must be earlier than closeTime
Business rules:
Time slots are generated from operating hours and slotDuration
Normal case: New operating hours are saved
Abnormal case:
Invalid time range/format: Update fails

## 3.5.5.2 Configure booking rules
Function trigger
Navigation path: Admin portal -> System Settings -> Library Settings
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Configure booking constraints for users
Interface:
Rule fields: maxBookingDays, maxBookingsPerDay, maxHoursPerDay, minReputation, autoCancelMinutes
Save settings button
Data processing:
Admin updates booking rule fields
System sends PUT /slib/settings/library
Function details
Data: maxBookingDays, maxBookingsPerDay, maxHoursPerDay, minReputation, autoCancelMinutes, workingDays
Validation:
Values must be positive and within allowed ranges
Business rules:
Booking service enforces these settings for all new bookings
Normal case: Booking rules saved and applied
Abnormal case:
Invalid config value: Backend rejects update

## 3.5.5.3 Turn on/Turn off automatic check-out
Function trigger
Navigation path: Admin portal -> System Settings -> Library Settings
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Enable or adjust automatic check-out/auto-cancel behavior
Interface:
Auto policy field: autoCancelOnLeaveMinutes (and related auto-cancel settings)
Save settings button
Data processing:
Admin sets auto check-out policy value
System sends PUT /slib/settings/library
Function details
Data: autoCancelOnLeaveMinutes, autoCancelMinutes
Validation:
Auto-cancel durations must be valid integers
Business rules:
System scheduler/service uses configured durations to auto end bookings/check-out flows
Normal case: Automatic check-out policy updated successfully
Abnormal case:
Invalid duration: Update rejected

## 3.5.5.4 Enable/Disable library
Function trigger
Navigation path: Admin portal -> System Settings -> Library Settings -> Library status toggle
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Temporarily close or reopen library services
Interface:
Status toggle: libraryClosed
Reason input: closedReason
Data processing:
Admin sets closed/open status
System sends POST /slib/settings/library/toggle-lock with closed and reason
Function details
Data: libraryClosed, closedReason
Validation:
If closed=true, reason should be provided
Business rules:
Closed library state is persisted and used by booking/notification services
Normal case: Library status changed successfully
Abnormal case:
Invalid request body: Status change fails

## 3.5.6.1 View HCE scan stations
Function trigger
Navigation path: Admin portal -> HCE Scan Stations
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View and monitor all registered HCE scan stations
Interface:
Stations table: deviceId, deviceName, deviceType, status, online, areaName, lastHeartbeat
Filter controls: search, status, deviceType
Data processing:
System sends GET /slib/hce/stations?search=&status=&deviceType=
System renders list and filter result
Function details
Data: id, deviceId, deviceName, deviceType, location, status, online, areaId, areaName, lastHeartbeat, todayScanCount, lastAccessTime
Validation:
Filter values must match supported enums when provided
Business rules:
Invalid filter values are ignored in backend filtering flow
Normal case: Station list loads successfully
Abnormal case:
Request error: Backend returns ERROR response

## 3.5.6.2 View HCE scan station details
Function trigger
Navigation path: Admin portal -> HCE Scan Stations -> Select one station
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View full detail of one HCE station before update actions
Interface:
Station detail panel/modal
Detail fields: device info, status, online state, area mapping, heartbeat and scan statistics
Data processing:
System sends GET /slib/hce/stations/{id}
System displays detail payload
Function details
Data: id, deviceId, deviceName, deviceType, location, status, online, areaId, areaName, createdAt, updatedAt, todayScanCount, lastAccessTime
Validation:
Station id must exist
Business rules:
Detail endpoint returns 404 when station is not found
Normal case: Detail data is displayed completely
Abnormal case:
Station not found: NOT_FOUND response (404)

## 3.5.6.3 View and update HCE station
Function trigger
Navigation path: Admin portal -> HCE Scan Stations -> Select station -> Edit/Change status
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View station data and update station information or status
Interface:
Edit form: deviceId, deviceName, deviceType, location, status, areaId
Quick status action: ACTIVE/INACTIVE/MAINTENANCE
Save button
Data processing:
Admin edits station data
System sends PUT /slib/hce/stations/{id} for full update
System can send PATCH /slib/hce/stations/{id}/status for status-only update
Function details
Data: deviceId, deviceName, deviceType, location, status, areaId
Validation:
Target station must exist
deviceId must stay unique
deviceType must be one of ENTRY_GATE, EXIT_GATE, SEAT_READER
status must be one of ACTIVE, INACTIVE, MAINTENANCE
areaId must exist when provided
Business rules:
Changing deviceId triggers uniqueness re-check
Status update can be done via dedicated endpoint
Normal case: Station is updated and reflected in list/detail
Abnormal case:
Duplicate deviceId: Update rejected
Invalid enum value: Update rejected with error message
Station not found: Update fails

## 3.5.6.4 Register HCE station
Function trigger
Navigation path: Admin portal -> HCE Scan Stations -> Register station
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Register a new physical HCE scan station into the system
Interface:
Register form: deviceId, deviceName, deviceType, location, status, areaId
Register button
Data processing:
Admin submits register form
System validates required fields
System sends POST /slib/hce/stations
Backend creates station and returns created resource
Function details
Data: deviceId, deviceName, deviceType, location, status, areaId
Validation:
deviceId is required and unique
deviceName is required
deviceType is required and valid enum
status must be valid enum if provided
areaId must reference existing area if provided
Business rules:
Default status is ACTIVE when status is not provided
Normal case: Station registered successfully (201)
Abnormal case:
Missing required field: Registration rejected
Duplicate deviceId: Registration rejected
Invalid enum/areaId: Registration rejected

## 3.5.6.5 Delete HCE station
Function trigger
Navigation path: Admin portal -> HCE Scan Stations -> Select station -> Delete
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove an HCE station from management list
Interface:
Delete action and confirmation dialog
Data processing:
Admin confirms deletion
System sends DELETE /slib/hce/stations/{id}
Backend deletes the station and returns success message
Function details
Data: id
Validation:
Station must exist before deletion
Business rules:
Deleted station is no longer available for heartbeat/check-in validation flows
Normal case: Station deleted successfully
Abnormal case:
Station not found: NOT_FOUND response (404)

## 3.5.7.1 View list of materials
Function trigger
Navigation path: Admin portal -> AI Config -> Materials tab
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View all materials used as raw sources for AI knowledge management
Interface:
Materials list/table: name, description, createdBy, active, itemCount, createdAt
Expand action: View material items
Data processing:
System sends GET /slib/ai/admin/materials
System renders materials sorted by latest created time
Function details
Data: id, name, description, createdBy, active, createdAt, updatedAt, items, itemCount
Validation:
User must be authenticated with valid token
Business rules:
List endpoint returns materials in descending createdAt order
Normal case: Materials list loads successfully
Abnormal case:
Auth/token error: Request fails and list is not loaded
Server error: Materials data cannot be fetched

## 3.5.7.2 Create material
Function trigger
Navigation path: Admin portal -> AI Config -> Materials tab -> Add material
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Create a new material container for text/file items
Interface:
Create form: name, description
Create button
Data processing:
Admin fills material form and submits
System sends POST /slib/ai/admin/materials
Backend creates material and returns created material payload
Function details
Data: name, description
Validation:
Material name should not be empty
Business rules:
createdBy is assigned by backend flow
Normal case: Material created and appears in materials list
Abnormal case:
Invalid payload: Creation request is rejected
Auth/token error: Creation is denied

## 3.5.7.3 View and update material
Function trigger
Navigation path: Admin portal -> AI Config -> Materials tab -> Select material -> Edit
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View material details and update metadata
Interface:
Material detail/edit form: name, description
Items panel: material items under selected material
Save button
Data processing:
System can load one material by id with GET /slib/ai/admin/materials/{id}
Admin edits material metadata
System sends PUT /slib/ai/admin/materials/{id}
Backend returns updated material data
Function details
Data: id, name, description, createdBy, active, items
Validation:
Material id must exist
Business rules:
Update modifies metadata fields of existing material
Normal case: Material details are shown and update succeeds
Abnormal case:
Material not found: Update/detail request fails
Invalid payload: Update request is rejected

## 3.5.7.4 Delete material
Function trigger
Navigation path: Admin portal -> AI Config -> Materials tab -> Select material -> Delete
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove a material from AI material management
Interface:
Delete action and confirmation
Data processing:
Admin confirms deletion
System sends DELETE /slib/ai/admin/materials/{id}
Backend deletes material and returns success message
Function details
Data: id
Validation:
Material must exist before deletion
Business rules:
Deleted material is no longer available in materials list
Normal case: Material deleted successfully
Abnormal case:
Material not found: Delete request fails
Server error: Delete action does not complete

## 3.5.7.5 View list of knowledge stores
Function trigger
Navigation path: Admin portal -> AI Config -> Knowledge Store tab
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View all knowledge stores and their sync state for RAG
Interface:
Knowledge store list/table: name, description, status, active, itemCount, lastSyncedAt
View action: Open store details and linked material items
Data processing:
System sends GET /slib/ai/admin/knowledge-stores
System renders stores in latest-created order
Function details
Data: id, name, description, createdBy, status (CHANGED/SYNCING/SYNCED/ERROR), active, lastSyncedAt, createdAt, updatedAt, items, itemCount
Validation:
User must be authenticated with valid token
Business rules:
Knowledge store status indicates sync readiness and result
Normal case: Knowledge store list loads successfully
Abnormal case:
Auth/token error: Request fails and list is not loaded
Server error: Knowledge store data cannot be fetched

## 3.5.7.6 Create knowledge store
Function trigger
Navigation path: Admin portal -> AI Config -> Knowledge Store tab -> Create knowledge store
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Create a new knowledge store from selected material items
Interface:
Create form: name, description, item selection (itemIds)
Create button
Data processing:
Admin fills form and selects item IDs
System sends POST /slib/ai/admin/knowledge-stores
Backend creates knowledge store and sets status to CHANGED
Function details
Data: name, description, itemIds
Validation:
Knowledge store name should not be empty
itemIds must reference existing material items when provided
Business rules:
New knowledge store is marked CHANGED and needs sync to vector DB
Normal case: Knowledge store created and appears in list
Abnormal case:
Invalid payload/itemIds: Creation request is rejected
Auth/token error: Creation is denied

## 3.5.7.7 View and update knowledge store
Function trigger
Navigation path: Admin portal -> AI Config -> Knowledge Store tab -> Select store -> Edit
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View knowledge store details and update metadata/item mapping
Interface:
Detail/edit form: name, description, active
Item mapping selector: linked material item IDs
Save button
Sync button: Trigger re-sync to vector DB
Data processing:
System can load one store by id with GET /slib/ai/admin/knowledge-stores/{id}
Admin updates store metadata/items
System sends PUT /slib/ai/admin/knowledge-stores/{id}
Backend marks store status as CHANGED after update
Admin can trigger sync via POST /slib/ai/admin/knowledge-stores/{id}/sync
Function details
Data: id, name, description, active, itemIds, status, lastSyncedAt
Validation:
Knowledge store id must exist
itemIds must map to valid material items
Business rules:
Any update marks store CHANGED and requires re-sync
Sync flow transitions status: SYNCING -> SYNCED or ERROR
Normal case: Store details are shown, update succeeds, and sync can complete
Abnormal case:
Store not found: Update/detail request fails
Sync failure: Store status becomes ERROR
Invalid payload/itemIds: Update request is rejected

## 3.5.7.8 Delete knowledge store
Function trigger
Navigation path: Admin portal -> AI Config -> Knowledge Store tab -> Select store -> Delete
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove knowledge store from system and cleanup vectors
Interface:
Delete action and confirmation dialog
Data processing:
Admin confirms deletion
System sends DELETE /slib/ai/admin/knowledge-stores/{id}
Backend deletes vectors in AI vector DB and removes store from database
Function details
Data: id
Validation:
Knowledge store must exist before deletion
Business rules:
Vector cleanup is attempted before DB deletion
Normal case: Knowledge store deleted successfully
Abnormal case:
Store not found: Delete request fails
Vector cleanup error: Logged, but DB delete may still proceed

## 3.5.7.9 Test AI chat
Function trigger
Navigation path: Admin portal -> AI Config -> Testing tab -> Chat test input
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Test AI responses, debug retrieval, and verify service connectivity
Interface:
Chat input box and Send button
Debug mode panel: action, confidence/retrieval info, sources/chunks
Session controls: clear chat history
Data processing:
Admin enters test message
System sends POST /api/v1/chat/debug with message and session_id
System displays AI reply and debug payload
System can load history via GET /api/v1/chat/history/{sessionId}
System can clear session via DELETE /api/v1/chat/session/{sessionId}
Function details
Data: message, sessionId, reply, action, debug.retrieval, sources/chunks, chat history
Validation:
Message must not be empty
sessionId must be valid format for history/clear operations
Business rules:
Test chat keeps session continuity with persisted history
Debug endpoint is used for admin deep testing
Normal case: AI reply and debug information are returned and displayed
Abnormal case:
AI service unavailable: Show connection/error message
Invalid request/session: History or debug call fails

## 3.5.8.1 View and update NFC Tag UID mapping
Function trigger
Navigation path: Admin portal -> NFC Tag Management -> Select seat -> Detail panel -> Scan again/Update mapping
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View current NFC mapping of a seat and update mapped UID when replacing tag/device
Interface:
Seat detail panel: seatCode, areaName, zoneName, mapping status, masked UID, lastUpdated
Action button: Scan again
Data processing:
System loads seat mapping detail via GET /slib/seats/{seatId}/nfc-info
Admin scans NFC UID from bridge service (localhost:5050/scan-uid)
System sends PUT /slib/seats/{seatId}/nfc-uid with body { nfcTagUid }
Backend normalizes and validates UID, hashes UID, checks uniqueness, then updates seat mapping
Function details
Data: seatId, seatCode, nfcTagUid (raw input), nfcUidMasked, areaName, zoneName, lastUpdated
Validation:
seatId must exist
nfcTagUid must be valid HEX UID format (4, 7, or 10 bytes)
nfcTagUid must not be assigned to another seat
Business rules:
Backend stores hashed UID only and returns masked UID for display
Updating UID refreshes nfcTagUidUpdatedAt
Normal case: Seat mapping detail is shown and UID remap succeeds
Abnormal case:
Seat not found: Detail/update request fails
Invalid UID format: Update is rejected
UID already assigned: Update is rejected with duplicate-mapping error
Bridge unavailable: Scan cannot proceed

## 3.5.8.2 Create NFC Tag UID mapping
Function trigger
Navigation path: Admin portal -> NFC Tag Management -> Select unmapped seat -> Scan & Assign NFC
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Create a new NFC UID mapping for a seat that has no existing mapping
Interface:
Seat map with mapped/unmapped states
Primary action button: Scan & Assign NFC
Data processing:
Admin selects seat without NFC mapping
System scans UID from bridge service (localhost:5050/scan-uid)
System sends PUT /slib/seats/{seatId}/nfc-uid with scanned nfcTagUid
Backend validates UID format, hashes UID, checks uniqueness, and creates mapping
Function details
Data: seatId, nfcTagUid, hasNfcTag, maskedNfcUid, updatedAt
Validation:
Target seat must exist
Raw UID must be non-empty and valid format
UID must be unique across all seats
Business rules:
Create mapping and update mapping use the same endpoint
After successful create, seat is shown as mapped in list/map view
Normal case: NFC UID is assigned and mapping status changes to mapped
Abnormal case:
Duplicate UID: Mapping create is rejected
Invalid seat/UID payload: Request fails
Bridge scan timeout/network error: Create flow cannot continue

## 3.5.8.3 Delete NFC Tag UID mapping
Function trigger
Navigation path: Admin portal -> NFC Tag Management -> Select mapped seat -> Delete NFC
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove NFC UID mapping from a seat
Interface:
Delete action in seat detail panel
Confirmation dialog
Data processing:
Admin confirms delete action
System sends DELETE /slib/seats/{seatId}/nfc-uid
Backend clears stored UID and updates mapping timestamp
Function details
Data: seatId, hasNfcTag, nfcUidMasked, lastUpdated
Validation:
seatId must exist before delete
Business rules:
After deletion, seat is treated as unmapped in NFC mapping list and seat map
Normal case: Mapping is removed successfully and UI refreshes to unmapped state
Abnormal case:
Seat not found: Delete request fails
Auth/token error: Delete action is denied

## 3.5.8.4 View NFC Tag mapping list
Function trigger
Navigation path: Admin portal -> NFC Tag Management
Timing Frequency: On demand and after mapping actions
Function description
Actors/Roles: Admin
Purpose: View all seats with NFC mapping state for monitoring and operation
Interface:
Visual seat map and summary stats: total seats, mapped seats, unmapped seats
Optional list/filter model by area, zone, hasNfc, search seatCode
Data processing:
System sends GET /slib/seats/nfc-mappings?zoneId=&areaId=&hasNfc=&search=
System renders returned mapping data into seat map/list with mapped/unmapped indicators
Function details
Data: seatId, seatCode, zoneId, zoneName, areaId, areaName, hasNfcTag, maskedNfcUid, updatedAt
Validation:
Filter inputs must match expected types (Integer/Boolean/String)
Business rules:
If no filter is provided, system returns all seats
Search is applied on seatCode
Normal case: Mapping list/map loads with correct mapped and unmapped counts
Abnormal case:
Invalid filter payload: Request fails
Server/auth error: Mapping list cannot be loaded

## 3.5.8.5 View NFC Tag mapping details
Function trigger
Navigation path: Admin portal -> NFC Tag Management -> Click a seat
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View detail information of one seat NFC mapping for verification and troubleshooting
Interface:
Detail panel fields: seatCode, areaName, zoneName, mapping status, masked UID, lastUpdated
Support action: Check NFC modal to scan card and identify mapped seat
Data processing:
System sends GET /slib/seats/{seatId}/nfc-info for detail panel
For check flow, system scans UID from bridge then calls GET /slib/seats/by-nfc-uid/{nfcTagUid}
System displays mapped seat info or not-mapped message
Function details
Data: seatId, seatCode, areaName, zoneName, nfcMapped, nfcUidMasked, lastUpdated
Validation:
seatId must exist for seat detail query
nfcTagUid must be valid when using check-by-UID flow
Business rules:
UID value returned to admin UI is masked, not raw persisted hash
Check-by-UID returns 404 when NFC tag has not been mapped to any seat
Normal case: Detail panel shows current NFC mapping info correctly
Abnormal case:
Seat not found: Detail endpoint fails
UID not mapped: Check flow returns not-found message
Bridge/service error: Check flow cannot complete

## 3.5.9.1 View list of Kiosk devices
Function trigger
Navigation path: Admin portal -> Kiosk Management
Timing Frequency: On demand and after kiosk operations
Function description
Actors/Roles: Admin
Purpose: View all registered kiosk devices and token/session status
Interface:
Kiosk table/card list: kioskCode, kioskName, kioskType, location, token status, token expiry, last active
Toolbar: search, filters, sort, refresh
Data processing:
System sends GET /slib/kiosk/admin/sessions
Backend returns kiosk list with token metadata and computed tokenValid flag
System renders list in table/card mode
Function details
Data: id, kioskCode, kioskName, kioskType, location, isActive, hasDeviceToken, deviceTokenIssuedAt, deviceTokenExpiresAt, lastActiveAt, tokenValid
Validation:
User must be authenticated with valid token
Business rules:
Token status is derived from hasDeviceToken and tokenValid (ACTIVE/EXPIRED/INACTIVE)
Search/filter/sort operate on returned kiosk dataset
Normal case: Device list loads with correct status and metadata
Abnormal case:
Auth/token error: Session list request is denied
Server error: Kiosk list cannot be loaded

## 3.5.9.2 View Kiosk device details
Function trigger
Navigation path: Admin portal -> Kiosk Management -> Select kiosk
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: View detailed information of one kiosk device for operation and troubleshooting
Interface:
Detail modal/panel: identity, type, location, active state, token state, created/updated timestamps
Action buttons: activate, revoke token, edit, delete
Data processing:
System can use selected row/session data and/or request detail via GET /slib/kiosk/admin/kiosks/{kioskId}
System displays kiosk detail fields and computed token state
Function details
Data: id, kioskCode, kioskName, kioskType, location, isActive, qrSecretKey, hasDeviceToken, deviceTokenIssuedAt, deviceTokenExpiresAt, lastActiveAt, tokenValid, createdAt, updatedAt
Validation:
kioskId must exist
Business rules:
Detail view supports operational actions without changing kioskCode
Normal case: Kiosk detail opens with complete metadata
Abnormal case:
Kiosk not found: Detail request fails
Auth/server error: Detail cannot be loaded

## 3.5.9.3 View and update Kiosk device
Function trigger
Navigation path: Admin portal -> Kiosk Management -> Select kiosk -> Edit
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Update kiosk device information and active state
Interface:
Edit form: kioskName, kioskType, location (kioskCode read-only)
Save button
Data processing:
Admin updates form fields
System sends PUT /slib/kiosk/admin/kiosks/{kioskId}
Backend validates values and updates kiosk record
Function details
Data: kioskId, kioskName, kioskType, location, isActive
Validation:
kioskId must exist
kioskName must not be empty
kioskType must be INTERACTIVE or MONITORING
Business rules:
kioskCode is immutable in update flow
Partial updates are supported for allowed fields
Normal case: Kiosk update succeeds and list/detail refreshes
Abnormal case:
Kiosk not found: Update request fails
Invalid kioskType/payload: Update request is rejected

## 3.5.9.4 Create Kiosk device
Function trigger
Navigation path: Admin portal -> Kiosk Management -> Add kiosk
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Register a new kiosk device in system
Interface:
Create form: kioskCode, kioskName, kioskType, location
Create button
Data processing:
Admin submits create form
System sends POST /slib/kiosk/admin/kiosks
Backend validates fields, checks kioskCode uniqueness, creates kiosk record
Function details
Data: kioskCode, kioskName, kioskType, location, isActive
Validation:
kioskCode is required and unique
kioskName is required
kioskType must be INTERACTIVE or MONITORING
Business rules:
New kiosk is created active by default
qrSecretKey is generated by backend during creation
Normal case: Kiosk device is created and appears in management list
Abnormal case:
Duplicate kioskCode: Creation request is rejected
Missing/invalid field: Creation request fails

## 3.5.9.5 Delete Kiosk device
Function trigger
Navigation path: Admin portal -> Kiosk Management -> Select kiosk -> Delete
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Remove kiosk device from management
Interface:
Delete action and confirmation dialog
Data processing:
Admin confirms deletion
System sends DELETE /slib/kiosk/admin/kiosks/{kioskId}
Backend revokes existing token (if any) and deletes kiosk record
Function details
Data: kioskId, kioskCode
Validation:
kioskId must exist before deletion
Business rules:
Token revocation is executed before deleting kiosk record
Deleted kiosk is no longer returned in kiosk sessions list
Normal case: Kiosk device deleted successfully
Abnormal case:
Kiosk not found: Delete request fails
Auth/server error: Delete action does not complete

## 3.5.9.6 Activate Kiosk device
Function trigger
Navigation path: Admin portal -> Kiosk Management -> Select kiosk -> Activate
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Generate device token and activation code for kiosk activation flow
Interface:
Activate button per kiosk
Activation result modal: activationCode, activationUrl, token expiry
Optional revoke token action
Data processing:
System sends POST /slib/kiosk/admin/token/{kioskId}
Backend generates device token, creates 6-character activation code, sets expiry, and returns activation data
System can revoke token using DELETE /slib/kiosk/admin/token/{kioskId}
Function details
Data: kioskId, kioskCode, token, activationCode, activationUrl, expiresAt
Validation:
kioskId must exist
Current admin authentication is required
Business rules:
Activation code is short-lived (15 minutes)
Token status transitions based on token validity (ACTIVE/EXPIRED/INACTIVE)
Revoke action invalidates current device token immediately
Normal case: Activation code and URL are generated and shown successfully
Abnormal case:
Kiosk not found: Activation request fails
Token generation/server error: Activation process fails
Revoke token error: Token remains active until successful revoke

## 3.5.10.1 Config system notification
Function trigger
Navigation path: Admin portal -> System Config -> Notifications tab
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Configure system-level notification switches for booking, reminder, violation, report, and device alerts
Interface:
Notification toggle list: notifyBookingSuccess, notifyCheckinReminder, notifyTimeExpiry, notifyViolation, notifyWeeklyReport, notifyDeviceAlert
Save button: Persist configuration
Data processing:
System loads current settings via GET /slib/settings/library
Admin changes notification toggles
System sends PUT /slib/settings/library with updated notification fields
Backend updates library settings and returns latest configuration
Function details
Data: notifyBookingSuccess, notifyCheckinReminder, notifyTimeExpiry, notifyViolation, notifyWeeklyReport, notifyDeviceAlert
Validation:
Toggle values must be boolean
User must be authenticated with valid token
Business rules:
Weekly report jobs follow notifyWeeklyReport flag
Push/device alert flows follow corresponding notification flags
Normal case: Notification settings saved and reflected in system config
Abnormal case:
Auth/token error: Save request is denied
Invalid payload type: Update request fails
Server error: Configuration is not persisted

## 3.5.10.2 View system overview information
Function trigger
Navigation path: Admin portal -> System Health -> Overview tab
Timing Frequency: On demand and periodic auto-refresh
Function description
Actors/Roles: Admin
Purpose: Monitor real-time system health metrics and runtime information
Interface:
Metric cards: CPU usage, memory usage, disk usage, processor count
System info panel: uptime, OS info, Java info, database/system runtime details
Refresh button
Data processing:
System sends GET /slib/system/info
Frontend renders metric cards and system information panel
Overview auto-refreshes periodically while tab is active
Function details
Data: cpu, memory, memoryUsedMB, memoryMaxMB, disk, diskUsedGB, diskTotalGB, availableProcessors, uptime, osName, osVersion, osArch, javaVersion, javaVendor
Validation:
Metrics response should contain required numeric/system fields
Business rules:
CPU/memory/disk values are shown as health indicators for admin monitoring
Overview refresh action does not modify any data
Normal case: System overview loads and refreshes successfully
Abnormal case:
System info API error: Overview cannot be loaded
Partial/invalid metrics: UI shows fallback or degraded state

## 3.5.10.3 Backup data manually
Function trigger
Navigation path: Admin portal -> System Health -> Backup tab -> Manual backup -> Backup now
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Trigger immediate backup and record execution result in backup history
Interface:
Manual backup action button
Backup history table: time, file size, duration, status, download action
Data processing:
Admin clicks backup now
System sends POST /slib/system/backup
Backend performs backup and returns status/result metadata
System refreshes history via GET /slib/system/backup/history
Function details
Data: backupId, status, filePath, fileSize, startedAt, completedAt, message, errorMessage
Validation:
Admin must be authenticated before backup trigger
Business rules:
Only successful backups can be downloaded
Backup history stores both success and failure records
Normal case: Manual backup completes and appears in history with SUCCESS status
Abnormal case:
Backup process error: Response returns FAILED with error message
Auth/token/server error: Backup trigger does not complete

## 3.5.10.4 Set automatic backup schedule
Function trigger
Navigation path: Admin portal -> System Health -> Backup tab -> Auto backup schedule
Timing Frequency: On demand
Function description
Actors/Roles: Admin
Purpose: Configure automatic backup time, retention policy, and active state
Interface:
Schedule status toggle: active/inactive
Schedule fields: time, retainDays
Save schedule button
Data processing:
System loads schedule via GET /slib/system/backup/schedule
Admin updates time/retainDays/isActive
System sends PUT /slib/system/backup/schedule
Backend recalculates nextBackupAt and stores schedule
Function details
Data: scheduleName, time, retainDays, isActive, nextBackupAt, lastBackupAt
Validation:
time must be valid HH:mm format
retainDays must be valid positive integer from allowed policy options
isActive must be boolean
Business rules:
If configured time is earlier than current time, next backup shifts to next day
Schedule remains disabled when isActive=false
Normal case: Schedule configuration is saved and next backup time is updated
Abnormal case:
Invalid time/retainDays payload: Schedule update fails
Server error: Schedule is not persisted

## 3.5.10.5 View system log
Function trigger
Navigation path: Admin portal -> System Health -> System Logs tab
Timing Frequency: On demand and when filters/search/page change
Function description
Actors/Roles: Admin
Purpose: View and investigate system logs with filters, search, and pagination
Interface:
Log filter controls: level, category, search text
Log list: level badge, category, service, message, timestamp, details
Statistics badges: errors/warnings in recent period
Pagination controls
Data processing:
System sends GET /slib/system/logs with query params (level, category, search, page, size)
System sends GET /slib/system/logs/stats for summary counts
Frontend renders log entries and pagination metadata
Function details
Data: content[], totalElements, totalPages, page, size, level, category, service, message, createdAt, details
Validation:
Filter values should match supported level/category enums
page and size must be valid non-negative numeric values
Business rules:
Logs API is admin-only and supports keyword/date/filter queries
Stats endpoint provides aggregated counts for monitoring severity trends
Normal case: Logs and stats load correctly with selected filters
Abnormal case:
Unauthorized role: Access denied to logs endpoint
Invalid query params: Request fails or returns empty result set
Server error: Logs/statistics cannot be fetched
