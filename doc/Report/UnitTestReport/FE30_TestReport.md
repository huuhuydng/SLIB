# Unit Test Report - FE-30: CRUD Seat

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-30</td><td><b>Function Name</b></td><td>CRUD Seat</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~20</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>SeatService.createSeat(...)</code>, <code>updateSeat(...)</code>, <code>deleteSeat(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify seat creation, code generation, selective update, and deletion behavior.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>8</td><td>0</td><td>0</td><td>2 / 6 / 0</td><td>8</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th><th>UTCID07</th><th>UTCID08</th></tr>
  <tr><td rowspan="8" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Seat request data is provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Seat CRUD depends on seat and zone repositories</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3"><b>Mock State (Dependencies)</b></td><td>Zone exists for seat creation</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Seat exists for update/delete</td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Zone or seat lookup fails</td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3"><b>Input</b></td><td>Seat code is omitted or provided</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Update contains partial seat fields</td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Delete targets missing seat id</td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="6" class="matrix-section"><b>Confirm</b></td><td rowspan="3"><b>Return</b></td><td>Seat is created with generated code when omitted</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Seat is created with provided code when available</td><td></td><td>O</td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Seat update changes only provided fields</td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="2"><b>Exception</b></td><td>Zone-not-found or seat-not-found exception is thrown</td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td>Repository save/delete failure is propagated</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>save()</code> is called for create/update and <code>deleteById()</code> for delete</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
