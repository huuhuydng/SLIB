# Unit Test Report - FE-86: Verify Complaint

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-86</td><td><b>Function Name</b></td><td>Verify Complaint</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~15</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>ComplaintService.accept(...)</code> and <code>deny(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify complaint verification/denial, librarian resolution fields, and dashboard side effects.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>6</td><td>0</td><td>0</td><td>1 / 5 / 0</td><td>6</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Complaint id and librarian id are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Verification depends on complaint and user repositories</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Complaint and librarian exist</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Complaint or librarian lookup fails</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Action is accept or deny with optional note</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Complaint status, resolvedBy, resolvedAt, and resolution note are updated correctly</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Dashboard and librarian pending-count broadcasts are triggered</td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Missing complaint or librarian triggers exception path</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>save()</code> is called once for accept/deny flow</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
