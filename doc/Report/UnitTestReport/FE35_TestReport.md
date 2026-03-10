# Unit Test Report - FE-35: Set Library Operating Hours

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-35</td><td><b>Function Name</b></td><td>Set Library Operating Hours</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~15</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>LibrarySettingService.updateSettings(LibrarySettingDTO dto)</code> and <code>generateTimeSlots()</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify library time updates, default-setting auto-create, and generated slot boundaries.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>5</td><td>0</td><td>0</td><td>1 / 4 / 0</td><td>5</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Library settings DTO is provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Settings depend on singleton repository record</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Existing settings record is found or default is auto-created</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Repository save or parse failure occurs</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>DTO contains new openTime, closeTime, and slotDuration</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Confirm</b></td><td rowspan="3"><b>Return</b></td><td>Updated DTO reflects persisted operating hours</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Default settings are created first when record is missing</td><td></td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Generated time slots align with the updated open/close range</td><td></td><td></td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Repository or time-format failure is propagated</td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>save()</code> is called before returning updated DTO when any field changes</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
