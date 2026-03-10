# Unit Test Report - FE-06: Change Password

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-06</td><td><b>Function Name</b></td><td>FE-06_Change Password</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~30</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3">`AuthService.changePassword(String email, String currentPassword, String newPassword)`</td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify password lookup, current-password verification, password-strength validation, and persistence behavior at backend service level.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>8</td><td>0</td><td>0</td><td>1 / 7 / 0</td><td>8</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr style="background: #001a8d; color: #ffffff;"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th><th>UTCID07</th><th>UTCID08</th></tr>
  <tr><td rowspan="9" style="background: #001a8d; color: #ffffff;"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>User email is provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>User lookup depends on `UserRepository.findByEmail()`</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3"><b>Mock State (Dependencies)</b></td><td>Repository returns an existing user</td><td>O</td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Repository returns empty result</td><td></td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Password encoder comparison returns `false`</td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td rowspan="4"><b>Input</b></td><td>Current password is correct and new password is valid</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>New password is shorter than 8 characters</td><td></td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>New password misses uppercase / digit / special character rules</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>User has no existing password and sets a new valid password</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="8" style="background: #001a8d; color: #ffffff;"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Password is encoded and `passwordChanged` becomes `true`</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td>`UserRepository.save()` is executed for valid password updates</td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="5"><b>Exception</b></td><td>Throws user-not-found exception</td><td></td><td>O</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Throws current-password mismatch exception</td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Throws minimum-length exception</td><td></td><td></td><td></td><td>O</td><td></td><td></td><td></td><td></td></tr>
  <tr><td>Throws uppercase / digit / special-character validation exception</td><td></td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>No current-password check is required when stored password is empty</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td><b>Log message / Interaction</b></td><td>Password encoder `matches()` is invoked only when stored password exists</td><td>O</td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="4" style="background: #001a8d; color: #ffffff;"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
