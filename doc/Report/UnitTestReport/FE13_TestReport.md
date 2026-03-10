# Unit Test Report - FE-13: View List of Users

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-13</td><td><b>Function Name</b></td><td>View List of Users</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~15</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>UserService.getAllUsers()</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify user-list retrieval from repository and list return behavior at backend service level.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="7" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Admin/Librarian access context is assumed</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>User list comes from repository lookup</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Repository returns populated user list</td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Repository returns empty list</td><td></td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3"><b>Input</b></td><td>At least one user exists</td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>No user exists</td><td></td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Repository throws runtime exception</td><td></td><td></td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Returns full user list unchanged</td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Returns empty list when repository is empty</td><td></td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Propagates repository exception for list retrieval failure</td><td></td><td></td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>findAll()</code> is called once and no save/delete occurs</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
