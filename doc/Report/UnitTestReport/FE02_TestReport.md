# Unit Test Report - FE-02: Login with SLIB Account

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr>
    <td><b>Function Code</b></td>
    <td>FE-02</td>
    <td><b>Function Name</b></td>
    <td>FE-02_SLIB Account Login</td>
  </tr>
  <tr>
    <td><b>Created By</b></td>
    <td>Hadi</td>
    <td><b>Executed By</b></td>
    <td>Hadi</td>
  </tr>
  <tr>
    <td><b>Lines of code</b></td>
    <td>60</td>
    <td><b>Lack of test cases</b></td>
    <td>0</td>
  </tr>
  <tr>
    <td><b>Class Under Test</b></td>
    <td colspan="3">`AuthService.loginWithPassword(String identifier, String password, String deviceInfo)`</td>
  </tr>
  <tr>
    <td><b>Test requirement</b></td>
    <td colspan="3">Verify credential lookup, password verification, refresh-token rotation, and backend exception behavior at service level.</td>
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
    <td>7</td>
    <td>0</td>
    <td>0</td>
    <td>3 / 4 / 0</td>
    <td>7</td>
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
    <th>UTCID07</th>
  </tr>
  <tr>
    <td rowspan="10" style="background: #001a8d; color: #ffffff;"><b>Condition</b></td>
    <td rowspan="2"><b>Precondition</b></td>
    <td>Identifier and password are provided</td>
    <td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td>Token generation is available through `JwtService`</td>
    <td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td rowspan="5"><b>Mock State (Dependencies)</b></td>
    <td>`findByEmailOrUsername()` returns an active user</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>`findByEmailOrUsername()` returns empty result</td>
    <td></td><td></td><td></td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Returned user is locked</td>
    <td></td><td></td><td></td><td></td><td>O</td><td></td><td></td>
  </tr>
  <tr>
    <td>Returned user has empty password</td>
    <td></td><td></td><td></td><td></td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Password comparison returns `false`</td>
    <td></td><td></td><td></td><td></td><td></td><td></td><td>O</td>
  </tr>
  <tr>
    <td rowspan="3"><b>Input</b></td>
    <td>Identifier is email</td>
    <td>O</td><td></td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td>Identifier is username</td>
    <td></td><td>O</td><td></td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>`deviceInfo` is provided</td>
    <td></td><td></td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="9" style="background: #001a8d; color: #ffffff;"><b>Confirm</b></td>
    <td rowspan="3"><b>Return</b></td>
    <td>`AuthResponse` contains generated token pair and mapped user fields</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Login succeeds when identifier is resolved through username lookup</td>
    <td></td><td>O</td><td></td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Refresh-token device information is preserved in saved token record</td>
    <td></td><td></td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="4"><b>Exception</b></td>
    <td>Throws invalid-credentials exception when user is not found</td>
    <td></td><td></td><td></td><td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Throws locked-account exception</td>
    <td></td><td></td><td></td><td></td><td>O</td><td></td><td></td>
  </tr>
  <tr>
    <td>Throws password-not-configured exception</td>
    <td></td><td></td><td></td><td></td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Throws invalid-credentials exception when password check fails</td>
    <td></td><td></td><td></td><td></td><td></td><td></td><td>O</td>
  </tr>
  <tr>
    <td rowspan="2"><b>Log message / Interaction</b></td>
    <td>`revokeAllByUserId()` is executed before saving a new refresh token</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Hashed refresh token is persisted through `RefreshTokenRepository.save()`</td>
    <td>O</td><td>O</td><td>O</td><td></td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td rowspan="4" style="background: #001a8d; color: #ffffff;"><b>Result</b></td>
    <td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td>
    <td>N</td><td>N</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td>
  </tr>
  <tr>
    <td colspan="2">Passed/Failed</td>
    <td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td>
  </tr>
  <tr>
    <td colspan="2">Executed Date</td>
    <td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td>
  </tr>
  <tr>
    <td colspan="2">Defect ID</td>
    <td></td><td></td><td></td><td></td><td></td><td></td><td></td>
  </tr>
</table>
