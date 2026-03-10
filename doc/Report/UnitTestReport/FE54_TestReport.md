# Unit Test Report - FE-54: Config Notification

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-54</td><td><b>Function Name</b></td><td>Config Notification</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~20</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>NotificationController.updateSettings(UUID userId, NotificationSettingsRequest request)</code> and <code>getSettings(UUID userId)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify per-user notification setting retrieval, partial update, default-true fallback, and user-not-found handling.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>User id and settings request are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Notification config depends on user repository lookup</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>User exists with nullable or non-null notification fields</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>User lookup returns empty result</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Request contains booking/reminder/news toggle fields</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Confirm</b></td><td rowspan="3"><b>Return</b></td><td>Updated settings reflect only provided boolean fields</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Get settings returns true defaults when values are null</td><td></td><td>O</td><td></td><td>O</td><td></td></tr>
  <tr><td>Save persists new toggle values</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Throws user-not-found exception for missing user</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>userRepository.save()</code> is called once for successful updates</td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
