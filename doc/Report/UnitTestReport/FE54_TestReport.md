# Unit Test Report - FE-54: Config Notification

<table class="report-meta">
  <tr>  <td class="label">Function Code</td>  <td class="value italic">FE-54</td>  <td class="label">Function Name</td>  <td class="value link">Config Notification</td></tr>
  <tr>  <td class="label">Created By</td>  <td class="value italic">Hadi</td>  <td class="label">Executed By</td>  <td class="value italic">Hadi</td></tr>
  <tr>  <td class="label">Lines of code</td>  <td class="value italic center">~20</td>  <td class="label">Lack of test cases</td>  <td class="value center">0</td></tr>
  <tr>  <td class="label">Class Under Test</td>  <td class="value code" colspan="3"><code>NotificationController.updateSettings(UUID userId, NotificationSettingsRequest request) and getSettings(UUID userId)</code></td></tr>
  <tr>  <td class="label">Test requirement</td>  <td class="value italic" colspan="3">Verify per-user notification setting retrieval, partial update, default-true fallback, and user-not-found handling.</td></tr>
  <tr>  <th class="stats-head center">Passed</th>  <th class="stats-head center">Failed</th>  <th class="stats-head center">Untested</th>  <th class="stats-head center">N / A / B</th>  <th class="stats-head center">Total Test Cases</th></tr>
  <tr>  <td class="value center">5</td>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">1 / 4 / 0</td>  <td class="value center">5</td></tr>
</table>

<table class="matrix-table">
  <tr><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--utc"><span>UTCID01</span></th><th class="matrix-head matrix-head--utc"><span>UTCID02</span></th><th class="matrix-head matrix-head--utc"><span>UTCID03</span></th><th class="matrix-head matrix-head--utc"><span>UTCID04</span></th><th class="matrix-head matrix-head--utc"><span>UTCID05</span></th></tr>
  <tr><td class="section-cell" rowspan="7">Condition</td><td class="category-cell" rowspan="2">Precondition</td><td class="item-cell">User id and settings request are provided</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="item-cell">Notification config depends on user repository lookup</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="2">Mock State (Dependencies)</td><td class="item-cell">User exists with nullable or non-null notification fields</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">User lookup returns empty result</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">HTTP Method</td><td class="item-cell">PUT</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">API Endpoint</td><td class="item-cell">/settings/{userId}</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">Input</td><td class="item-cell">Request contains booking/reminder/news toggle fields</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="section-cell" rowspan="7">Confirm</td><td class="category-cell" rowspan="2">Return</td><td class="item-cell">200: Success</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">400: Bad Request</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="5">Log message</td><td class="item-cell">Successfully updated Config Notification for Request contains booking/reminder/news toggle fields</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully updated Config Notification for Request contains booking/reminder/news toggle fields (UTCID02)</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully updated Config Notification for Request contains booking/reminder/news toggle fields (UTCID03)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully updated Config Notification for Request contains booking/reminder/news toggle fields (UTCID04)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to update Config Notification: Invalid request parameters for Request contains booking/reminder/news toggle fields</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
</table>
