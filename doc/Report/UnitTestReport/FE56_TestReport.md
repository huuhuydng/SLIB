# Unit Test Report - FE-56: View System Log

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-56</td><td><b>Function Name</b></td><td>View System Log</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~10</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>CheckInService.getAllAccessLogs()</code> or closest current operational-log listing path</td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify current backend log-style listing through access-log retrieval, since no dedicated generic system-log service is present.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>6</td><td>0</td><td>0</td><td>1 / 5 / 0</td><td>6</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th></tr>
  <tr><td rowspan="6" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Log retrieval request is made</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Current closest runtime log source is access-log service</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Access-log repository returns populated logs</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Repository returns empty list</td><td></td><td></td><td></td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Repository throws failure</td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Operational logs are requested without mutation</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Returns mapped access logs in DTO form as closest system-log view</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Returns empty list when no logs exist</td><td></td><td></td><td></td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Repository failure is propagated/handled by controller</td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>getAllAccessLogs()</code> performs read-only repository access</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
