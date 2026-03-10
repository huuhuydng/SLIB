# Unit Test Report - FE-01: Login with Google Account

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr>
    <td><b>Function Code</b></td>
    <td>FE-01</td>
    <td><b>Function Name</b></td>
    <td>FE-01_Google Login</td>
  </tr>
  <tr>
    <td><b>Created By</b></td>
    <td>Hadi</td>
    <td><b>Executed By</b></td>
    <td>Hadi</td>
  </tr>
  <tr>
    <td><b>Lines of code</b></td>
    <td>86</td>
    <td><b>Lack of test cases</b></td>
    <td>0</td>
  </tr>
  <tr>
    <td><b>Class Under Test</b></td>
    <td colspan="3">`AuthService.loginWithGoogle(String googleIdToken, String fullNameFromClient, String fcmToken, String deviceInfo)`</td>
  </tr>
  <tr>
    <td><b>Test requirement</b></td>
    <td colspan="3">Verify Google token validation, user creation or lookup, refresh-token rotation, notification-device handling, and exception behavior at backend service level.</td>
  </tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr>
    <th>Passed</th>
    <th>Failed</th>
    <th>Untested</th>
    <th>N/A/B</th>
    <th>Total Test Cases</th>
  </tr>
  <tr>
    <td>6</td>
    <td>0</td>
    <td>0</td>
    <td>3 / 3 / 0</td>
    <td>6</td>
  </tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr style="background: #001a8d; color: #ffffff;">
    <th>Section</th>
    <th>Category</th>
    <th>Item</th>
    <th>UTCID01</th>
    <th>UTCID02</th>
    <th>UTCID03</th>
    <th>UTCID04</th>
    <th>UTCID05</th>
    <th>UTCID06</th>
  </tr>
  <tr>
    <td rowspan="10" style="background: #001a8d; color: #ffffff;"><b>Condition</b></td>
    <td rowspan="2"><b>Precondition</b></td>
    <td>Google ID token string is provided</td>
    <td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td>`googleClientId` is configured</td>
    <td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td rowspan="5"><b>Mock State (Dependencies)</b></td>
    <td>`verifyGoogleToken()` returns a valid payload</td>
    <td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td>
  </tr>
  <tr>
    <td>`UserRepository.findByEmail()` returns an active user</td>
    <td>O</td><td></td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>`UserRepository.findByEmail()` returns empty result</td>
    <td></td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>`UserRepository.findByEmail()` returns a locked user</td>
    <td></td><td></td><td></td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Non-empty FCM token is supplied</td>
    <td></td><td></td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="3"><b>Input</b></td>
    <td>Email belongs to `@fpt.edu.vn`</td>
    <td>O</td><td>O</td><td>O</td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Email is non-FPT and not whitelisted</td>
    <td></td><td></td><td></td><td>O</td><td></td><td></td>
  </tr>
  <tr>
    <td>Client full name is provided</td>
    <td></td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="9" style="background: #001a8d; color: #ffffff;"><b>Confirm</b></td>
    <td rowspan="3"><b>Return</b></td>
    <td>`AuthResponse` contains generated access token, refresh token, and mapped user fields</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>New user is created with role `STUDENT` and `passwordChanged = true`</td>
    <td></td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Existing user receives updated notification device data</td>
    <td></td><td></td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="3"><b>Exception</b></td>
    <td>Throws invalid email-domain exception</td>
    <td></td><td></td><td></td><td>O</td><td></td><td></td>
  </tr>
  <tr>
    <td>Throws locked-account exception</td>
    <td></td><td></td><td></td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Throws Google token verification exception</td>
    <td></td><td></td><td></td><td></td><td></td><td>O</td>
  </tr>
  <tr>
    <td rowspan="3"><b>Log message / Interaction</b></td>
    <td>`revokeAllByUserId()` is executed before storing a new refresh token</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Hashed refresh token is persisted with device information</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>`clearNotiDeviceForOtherUsers()` is called before assigning duplicated FCM token</td>
    <td></td><td></td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="4" style="background: #001a8d; color: #ffffff;"><b>Result</b></td>
    <td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td>
    <td>N</td><td>N</td><td>N</td><td>A</td><td>A</td><td>A</td>
  </tr>
  <tr>
    <td colspan="2">Passed/Failed</td>
    <td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td>
  </tr>
  <tr>
    <td colspan="2">Executed Date</td>
    <td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td>
  </tr>
  <tr>
    <td colspan="2">Defect ID</td>
    <td></td><td></td><td></td><td></td><td></td><td></td>
  </tr>
</table>
