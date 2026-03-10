# Unit Test Report - FE-29: View Seat Map

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-29</td><td><b>Function Name</b></td><td>View Seat Map</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~15</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>SeatService.getSeatsByZoneId(...)</code> and <code>BookingService.getSeatsByTime(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify seat-map retrieval, dynamic status mapping, and time-range occupancy behavior.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="6" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Zone id and optional time range are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Seat map depends on seat and reservation repositories</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3"><b>Mock State (Dependencies)</b></td><td>Seats exist with active and inactive states</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Reservations overlap selected time slot</td><td></td><td>O</td><td></td><td>O</td><td></td></tr>
  <tr><td>Repository or date parsing fails</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Request includes plain zone view or timed availability view</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Confirm</b></td><td rowspan="3"><b>Return</b></td><td>Seat responses include zone info and dynamic status mapping</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Overlapping reservations change seat status to BOOKED/HOLDING as applicable</td><td></td><td>O</td><td></td><td>O</td><td></td></tr>
  <tr><td>Inactive seats become UNAVAILABLE in timed view</td><td></td><td></td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Invalid input or repository failure is propagated</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>findByZone_ZoneId()</code> and overlap queries are called according to branch</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
