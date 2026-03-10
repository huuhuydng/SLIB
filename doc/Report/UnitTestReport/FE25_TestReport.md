# Unit Test Report - FE-25: CRUD Zone

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-25</td><td><b>Function Name</b></td><td>CRUD Zone</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~35</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>ZoneService.createZone(...)</code>, <code>updateZoneFull(...)</code>, <code>deleteZone(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify zone creation, full update, area lookup, and cascading delete behavior.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>17</td><td>0</td><td>0</td><td>4 / 13 / 0</td><td>17</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th><th>UTCID07</th><th>UTCID08</th><th>UTCID09</th><th>UTCID10</th><th>UTCID11</th><th>UTCID12</th><th>UTCID13</th><th>UTCID14</th><th>UTCID15</th><th>UTCID16</th><th>UTCID17</th></tr>
  <tr><td rowspan="9" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Zone request data is provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Zone CRUD depends on zone, area, seat, and reservation repositories</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4"><b>Mock State (Dependencies)</b></td><td>Area exists for zone creation</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Zone exists for update/delete</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Zone does not exist for update/delete</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Seats and reservations exist for cascading delete</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td rowspan="3"><b>Input</b></td><td>Create request contains valid areaId and geometry</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Update request contains full zone payload</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Delete request targets missing or existing zone</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="8" class="matrix-section"><b>Confirm</b></td><td rowspan="4"><b>Return</b></td><td>Created zone maps areaId, position, size, and lock flag</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Updated zone persists all mutable fields</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Delete removes reservations, seats, then zone</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Default lock value is false when omitted on create</td><td></td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td rowspan="3"><b>Exception</b></td><td>Create throws area-not-found exception for invalid area</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Update throws zone-not-found exception</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Delete throws zone-not-found exception</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>deleteBySeat_SeatId()</code> and <code>deleteByZone_ZoneId()</code> run before zone delete</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>N</td><td>N</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
