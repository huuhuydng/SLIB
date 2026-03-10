# Unit Test Report - FE-42: CRUD Material

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-42</td><td><b>Function Name</b></td><td>CRUD Material</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~30</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>MaterialService.createMaterial(...)</code>, <code>updateMaterial(...)</code>, <code>deleteMaterial(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify material creation, update, delete, and not-found behavior.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>7</td><td>0</td><td>0</td><td>2 / 5 / 0</td><td>7</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th><th>UTCID07</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Material request data is provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Material CRUD depends on material repository</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Repository returns existing material for update/delete</td><td></td><td></td><td>O</td><td>O</td><td>O</td><td></td><td></td></tr>
  <tr><td>Repository returns empty result for missing material</td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Create/update/delete material requests are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Created or updated material maps name, description, active flag, and metadata</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Delete completes for existing material id</td><td></td><td></td><td></td><td></td><td>O</td><td></td><td></td></tr>
  <tr><td rowspan="2"><b>Exception</b></td><td>Throws material-not-found exception for missing id</td><td></td><td></td><td></td><td></td><td></td><td>O</td><td>O</td></tr>
  <tr><td>Repository failure during save/delete is propagated</td><td></td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>save()</code> and <code>deleteById()</code> are called according to CRUD branch</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
