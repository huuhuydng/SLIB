# Unit Test Report - FE-33: CRUD Reputation Rule

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-33</td><td><b>Function Name</b></td><td>CRUD Reputation Rule</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~132</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3">`ReputationRuleController.createRule(...)`, `updateRule(...)`, `toggleRuleStatus(...)`, `deleteRule(...)`</td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify that ADMIN can create, update, toggle active status, and delete reputation rules through `/slib/admin/reputation-rules`, with correct persistence, duplicate-code validation, and expected not-found/error behavior.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>7</td><td>0</td><td>0</td><td>3 / 3 / 1</td><td>7</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th><th>UTCID07</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>User is authenticated with ADMIN role</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>CRUD endpoints are mapped in `ReputationRuleController`</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Repository can save, update, toggle, and delete rule entities</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Repository may return duplicate rule code or missing id depending on scenario</td><td></td><td></td><td>O</td><td>O</td><td></td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Create/update/toggle/delete requests are sent to `/slib/admin/reputation-rules`</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3" class="matrix-section"><b>Confirm</b></td><td rowspan="1"><b>Return</b></td><td>Create, update, and toggle return `ReputationRuleResponse`; delete returns `204 No Content`</td><td>O</td><td>O</td><td></td><td></td><td>O</td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Returns 400 for duplicate `ruleCode`, 404 for missing id, and 403 for non-ADMIN access</td><td></td><td></td><td>O</td><td>O</td><td></td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td>Controller persists changes through `ReputationRuleRepository` save/delete methods</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>N</td><td>A</td><td>A</td><td>N</td><td>A</td><td>B</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
