# Unit Test Report - FE-71: View Booking Details

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-71</td><td><b>Function Name</b></td><td>View Booking Details</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~10</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>BookingService.getUpcomingBooking(...)</code> and booking history/detail lookup paths</td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify booking-detail style retrieval and mapped reservation information.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Booking detail request is made</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Details depend on reservation repository lookup and mapping</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Booking exists with related seat/zone info</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Booking does not exist or query fails</td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Target booking id or user scope is valid/invalid</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Returns booking detail mapping with seat, time, and status data</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Empty/no-content path is used when no target booking exists</td><td></td><td></td><td></td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Repository/query failure is propagated</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td>Detail lookup is read-only and performs one booking query path</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
