# Unit Test Report - FE-23: Lock Area Movement

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-23</td><td><b>Function Name</b></td><td>Lock Area Movement</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~10</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>AreaService.updateAreaLocked(...)</code>, <code>updateAreaPosition(...)</code>, <code>updateAreaDimensions(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify area lock state persistence and movement/resize blocking when locked.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>4</td><td>0</td><td>0</td><td>1 / 3 / 0</td><td>4</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Area id is provided</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Area lock and movement use repository lookup</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Area exists and locked=false before update</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Area exists and locked=true before move/resize</td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Lock area then try move/resize</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Lock flag is updated and persisted</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Move/resize succeeds only when area is not locked</td><td></td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Move or resize throws "Area is locked" runtime exception</td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>save()</code> is not executed for blocked move/resize operations</td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td></tr>
</table>
