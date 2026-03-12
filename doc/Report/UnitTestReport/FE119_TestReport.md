# Unit Test Report - FE-119: Manual Response

<table class="report-meta">
  <tr>  <td class="label">Function Code</td>  <td class="value italic">FE-119</td>  <td class="label">Function Name</td>  <td class="value link">Manual Response</td></tr>
  <tr>  <td class="label">Created By</td>  <td class="value italic">Hadi</td>  <td class="label">Executed By</td>  <td class="value italic">Hadi</td></tr>
  <tr>  <td class="label">Lines of code</td>  <td class="value italic center">~12</td>  <td class="label">Lack of test cases</td>  <td class="value center">0</td></tr>
  <tr>  <td class="label">Class Under Test</td>  <td class="value code" colspan="3"><code>SupportRequestService.respond(UUID requestId, String response, UUID librarianId)</code></td></tr>
  <tr>  <td class="label">Test requirement</td>  <td class="value italic" colspan="3">Verify manual librarian response to support request, resolved status update, and notification side effects.</td></tr>
  <tr>  <th class="stats-head center">Passed</th>  <th class="stats-head center">Failed</th>  <th class="stats-head center">Untested</th>  <th class="stats-head center">N / A / B</th>  <th class="stats-head center">Total Test Cases</th></tr>
  <tr>  <td class="value center">5</td>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">1 / 4 / 0</td>  <td class="value center">5</td></tr>
</table>

<table class="matrix-table">
  <tr><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--utc"><span>UTCID01</span></th><th class="matrix-head matrix-head--utc"><span>UTCID02</span></th><th class="matrix-head matrix-head--utc"><span>UTCID03</span></th><th class="matrix-head matrix-head--utc"><span>UTCID04</span></th><th class="matrix-head matrix-head--utc"><span>UTCID05</span></th></tr>
  <tr><td class="section-cell" rowspan="7">Condition</td><td class="category-cell" rowspan="2">Precondition</td><td class="item-cell">Support request id, response text, and librarian id are provided</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="item-cell">Manual response depends on support-request and user repositories</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="2">Mock State (Dependencies)</td><td class="item-cell">Support request and librarian exist</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Request or librarian lookup fails</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">HTTP Method</td><td class="item-cell">POST</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">API Endpoint</td><td class="item-cell">/slib/chat/conversations/{conversationId}/messages</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">Input</td><td class="item-cell">Manual response content is provided</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="section-cell" rowspan="6">Confirm</td><td class="category-cell" rowspan="1">Return</td><td class="item-cell">200: Success</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="5">Log message</td><td class="item-cell">Successfully created Manual Response for Manual response content is provided</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully created Manual Response for Manual response content is provided (UTCID02)</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully created Manual Response for Manual response content is provided (UTCID03)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully created Manual Response for Manual response content is provided (UTCID04)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully created Manual Response for Manual response content is provided (UTCID05)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
</table>
