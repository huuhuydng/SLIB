# Unit Test Report - FE-34: Set Deducted Point

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-34</td><td><b>Function Name</b></td><td>Set Deducted Point</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~12</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>SeatViolationReportService.verifyReport(UUID reportId, UUID librarianId)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify deducted-point application through verified violation flow, reputation floor handling, and point-transaction side effects.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Violation report id and librarian id are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Point deduction depends on violation report, user, reservation, and profile services</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Violation report and violator can be resolved</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Rule/report lookup fails or verification path breaks</td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Verified report should deduct configured penalty points</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Verified report response contains updated report status and violator information</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Reputation score is reduced but never below zero</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Missing report or related data triggers runtime exception</td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td>Point transaction, profile update, and dashboard notifications are triggered during verification</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
