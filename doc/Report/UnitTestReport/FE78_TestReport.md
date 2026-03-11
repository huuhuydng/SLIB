# Unit Test Report - FE-78: View History Points

<table class="report-meta">
  <tr>  <td class="label">Function Code</td>  <td class="value italic">FE-78</td>  <td class="label">Function Name</td>  <td class="value link">View History Points</td></tr>
  <tr>  <td class="label">Created By</td>  <td class="value italic">Hadi</td>  <td class="label">Executed By</td>  <td class="value italic">Hadi</td></tr>
  <tr>  <td class="label">Lines of code</td>  <td class="value italic center">~10</td>  <td class="label">Lack of test cases</td>  <td class="value center">0</td></tr>
  <tr>  <td class="label">Class Under Test</td>  <td class="value code" colspan="3"><code>StudentProfileService.getProfileByUserId(...) plus point-transaction dependent reputation updates</code></td></tr>
  <tr>  <td class="label">Test requirement</td>  <td class="value italic" colspan="3">Verify current point-history-related retrieval through profile and transaction-linked reputation state.</td></tr>
  <tr>  <th class="stats-head center">Passed</th>  <th class="stats-head center">Failed</th>  <th class="stats-head center">Untested</th>  <th class="stats-head center">N / A / B</th>  <th class="stats-head center">Total Test Cases</th></tr>
  <tr>  <td class="value center">5</td>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">1 / 4 / 0</td>  <td class="value center">5</td></tr>
</table>

<table class="matrix-table">
  <tr><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--utc"><span>UTCID01</span></th><th class="matrix-head matrix-head--utc"><span>UTCID02</span></th><th class="matrix-head matrix-head--utc"><span>UTCID03</span></th><th class="matrix-head matrix-head--utc"><span>UTCID04</span></th><th class="matrix-head matrix-head--utc"><span>UTCID05</span></th></tr>
  <tr><td class="section-cell" rowspan="7">Condition</td><td class="category-cell" rowspan="2">Precondition</td><td class="item-cell">User id is provided</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="item-cell">Point history is indirectly reflected through profile score and transactions</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="2">Mock State (Dependencies)</td><td class="item-cell">Profile exists with updated score/violation counts</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Profile lookup fails or score data is absent</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">HTTP Method</td><td class="item-cell">GET</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">API Endpoint</td><td class="item-cell">/slib/student-profile/me</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">Input</td><td class="item-cell">User has transaction-driven reputation changes</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="section-cell" rowspan="6">Confirm</td><td class="category-cell" rowspan="1">Return</td><td class="item-cell">200: Success</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="5">Log message</td><td class="item-cell">Successfully retrieved History Points for User has transaction-driven reputation changes</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully retrieved History Points for User has transaction-driven reputation changes (UTCID02)</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully retrieved History Points for User has transaction-driven reputation changes (UTCID03)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully retrieved History Points for User has transaction-driven reputation changes (UTCID04)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully retrieved History Points for User has transaction-driven reputation changes (UTCID05)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
</table>
