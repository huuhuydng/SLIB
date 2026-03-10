# Unit Test Report - FE-18: Change User Status

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-18</td><td><b>Function Name</b></td><td>Change User Status</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~15</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>UserService.toggleUserActive(UUID userId, boolean isActive)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify active-state toggling, refresh-token revocation on lock, and missing-user handling.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>6</td><td>0</td><td>0</td><td>1 / 5 / 0</td><td>6</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th></tr>
  <tr><td rowspan="6" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>User id and target active status are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Status change depends on user lookup and refresh-token repository</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>User lookup returns existing user</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>User lookup returns empty result</td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="2"><b>Input</b></td><td>Set user active to true</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Set user active to false</td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Updated user is saved with new active flag</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Refresh tokens are revoked only when locking account</td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Throws user-not-found exception when id is missing</td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>revokeAllByUserId()</code> is skipped for unlock and called for lock</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
