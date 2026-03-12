# Unit Test Report - FE-27: View Zone Details

<table class="report-meta">
  <tr>  <td class="label">Function Code</td>  <td class="value italic">FE-27</td>  <td class="label">Function Name</td>  <td class="value link">View Zone Details</td></tr>
  <tr>  <td class="label">Created By</td>  <td class="value italic"></td>  <td class="label">Executed By</td>  <td class="value italic"></td></tr>
  <tr>  <td class="label">Lines of code</td>  <td class="value italic center">~15</td>  <td class="label">Lack of test cases</td>  <td class="value center">0</td></tr>
  <tr>  <td class="label">Class Under Test</td>  <td class="value code" colspan="3"><code>ZoneService.getZoneById(Integer id)</code></td></tr>
  <tr>  <td class="label">Test requirement</td>  <td class="value italic" colspan="3">N/A</td></tr>
  <tr>  <th class="stats-head center">Passed</th>  <th class="stats-head center">Failed</th>  <th class="stats-head center">Untested</th>  <th class="stats-head center">N / A / B</th>  <th class="stats-head center">Total Test Cases</th></tr>
  <tr>  <td class="value center">5</td>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">1 / 4 / 0</td>  <td class="value center">5</td></tr>
</table>

<table class="matrix-table">
  <tr><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--utc"><span>UTCID01</span></th><th class="matrix-head matrix-head--utc"><span>UTCID02</span></th><th class="matrix-head matrix-head--utc"><span>UTCID03</span></th><th class="matrix-head matrix-head--utc"><span>UTCID04</span></th><th class="matrix-head matrix-head--utc"><span>UTCID05</span></th></tr>
  <tr><td class="section-cell" rowspan="10">Condition</td><td class="category-cell" rowspan="2">Precondition</td><td class="item-cell">Authorized</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
  <tr><td class="item-cell">Zone exists</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="category-cell" rowspan="1">Mock State (Dependencies)</td><td class="item-cell">N/A</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="category-cell" rowspan="1">HTTP Method</td><td class="item-cell">GET</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">API Endpoint</td><td class="item-cell">/slib/zones/{id}</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="5">Input</td><td class="item-cell">Valid JWT Token</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">No token in request</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Zone not found</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">No permission</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Invalid zone ID format</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
  <tr><td class="section-cell" rowspan="12">Confirm</td><td class="category-cell" rowspan="5">Return</td><td class="item-cell">200: OK</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">401: Unauthorized</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">404: Not Found</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">403: Forbidden</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">400: Bad Request</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="2">Exception</td><td class="item-cell">None</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">RuntimeException</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="5">Log message</td><td class="item-cell">Successfully retrieved Zone Details for Valid JWT Token</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to retrieve Zone Details: Unauthorized request for No token in request</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to retrieve Zone Details: Requested resource not found for Zone not found</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to retrieve Zone Details: User does not have permission for No permission</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to retrieve Zone Details: Invalid request parameters for Invalid zone ID format</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
  <tr><td class="section-cell" rowspan="4">Result</td><td class="category-cell"></td><td class="item-cell item-cell--result">Type (N: Normal, A: Abnormal, B: Boundary)</td><td class="mark-cell">N</td><td class="mark-cell">A</td><td class="mark-cell">A</td><td class="mark-cell">A</td><td class="mark-cell">A</td></tr>
  <tr><td class="category-cell"></td><td class="item-cell item-cell--result">Passed/Failed</td><td class="mark-cell">P</td><td class="mark-cell">P</td><td class="mark-cell">P</td><td class="mark-cell">P</td><td class="mark-cell">P</td></tr>
  <tr><td class="category-cell"></td><td class="item-cell item-cell--result">Executed Date</td><td class="mark-cell">2026-03-07</td><td class="mark-cell">2026-03-07</td><td class="mark-cell">2026-03-07</td><td class="mark-cell">2026-03-07</td><td class="mark-cell">2026-03-07</td></tr>
  <tr><td class="category-cell"></td><td class="item-cell item-cell--result">Defect ID</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
</table>
