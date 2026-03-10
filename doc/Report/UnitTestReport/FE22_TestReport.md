# Unit Test Report - FE-22: Change Area Status

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-22</td><td><b>Function Name</b></td><td>Change Area Status</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~12</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>AreaService.updateAreaIsActive(Long id, AreaResponse req)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify area active-status updates and missing-area behavior.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Area id and target active state are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Status change depends on repository lookup</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Repository returns existing area</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Repository returns empty result</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Toggle active true/false</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Updated area returns persisted active flag</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Only active state is changed by this method</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Throws area-not-found exception when id does not exist</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>save()</code> is called once for existing area</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
