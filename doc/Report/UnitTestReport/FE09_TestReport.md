# Unit Test Report - FE-09: View Account Setting

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-09</td><td><b>Function Name</b></td><td>FE-09_View Account Setting</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~10</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3">`UserSettingService.getSettings(UUID userId)`</td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify user-setting retrieval, default-setting auto-creation, and repository interaction behavior at backend service level.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>4</td><td>0</td><td>0</td><td>1 / 3 / 0</td><td>4</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr style="background: #001a8d; color: #ffffff;"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th></tr>
  <tr><td rowspan="7" style="background: #001a8d; color: #ffffff;"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>User id is provided</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Setting lookup depends on `UserSettingRepository.findById()`</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Repository returns existing settings record</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Repository returns empty result and default settings must be created</td><td></td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Input</b></td><td>Auto-created default values are expected</td><td></td><td>O</td><td></td><td></td></tr>
  <tr><td>Repository save throws runtime failure during auto-create</td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="6" style="background: #001a8d; color: #ffffff;"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Existing settings are returned without modification</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Default settings are auto-created with HCE, AI, and booking reminders enabled</td><td></td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="2"><b>Exception</b></td><td>Save failure during default creation is propagated</td><td></td><td></td><td>O</td><td></td></tr>
  <tr><td>Repository failure on repeated missing-setting lookup is propagated</td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="2"><b>Log message / Interaction</b></td><td>`save()` is executed only when settings are missing</td><td></td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>No extra save happens when existing settings are already present</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td rowspan="4" style="background: #001a8d; color: #ffffff;"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td></tr>
</table>
