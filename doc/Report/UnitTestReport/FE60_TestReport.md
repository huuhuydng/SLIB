# Unit Test Report - FE-60: Filter Seat Map

<table class="report-meta">
  <tr>  <td class="label">Function Code</td>  <td class="value italic">FE-60</td>  <td class="label">Function Name</td>  <td class="value link">Filter Seat Map</td></tr>
  <tr>  <td class="label">Created By</td>  <td class="value italic">Hadi</td>  <td class="label">Executed By</td>  <td class="value italic">Hadi</td></tr>
  <tr>  <td class="label">Lines of code</td>  <td class="value italic center">~10</td>  <td class="label">Lack of test cases</td>  <td class="value center">0</td></tr>
  <tr>  <td class="label">Class Under Test</td>  <td class="value code" colspan="3"><code>BookingService.getSeatsByTime(...)</code></td></tr>
  <tr>  <td class="label">Test requirement</td>  <td class="value italic" colspan="3">Verify seat-map filtering by zone/date/time range and branch selection.</td></tr>
  <tr>  <th class="stats-head center">Passed</th>  <th class="stats-head center">Failed</th>  <th class="stats-head center">Untested</th>  <th class="stats-head center">N / A / B</th>  <th class="stats-head center">Total Test Cases</th></tr>
  <tr>  <td class="value center">5</td>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">1 / 4 / 0</td>  <td class="value center">5</td></tr>
</table>

<table class="matrix-table">
  <tr><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--blank"></th><th class="matrix-head matrix-head--utc"><span>UTCID01</span></th><th class="matrix-head matrix-head--utc"><span>UTCID02</span></th><th class="matrix-head matrix-head--utc"><span>UTCID03</span></th><th class="matrix-head matrix-head--utc"><span>UTCID04</span></th><th class="matrix-head matrix-head--utc"><span>UTCID05</span></th></tr>
  <tr><td class="section-cell" rowspan="7">Condition</td><td class="category-cell" rowspan="2">Precondition</td><td class="item-cell">Filter parameters are provided</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="item-cell">Filter uses timed seat lookup branch</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="2">Mock State (Dependencies)</td><td class="item-cell">Filtered zone has seats and reservations</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Invalid or missing filter values break lookup</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">HTTP Method</td><td class="item-cell">GET</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">API Endpoint</td><td class="item-cell">/slib/seats?startTime={iso}&amp;endTime={iso}&amp;zoneId={zoneId}</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="1">Input</td><td class="item-cell">Zone/date/time combinations vary</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="section-cell" rowspan="7">Confirm</td><td class="category-cell" rowspan="2">Return</td><td class="item-cell">200: Success</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">400: Bad Request</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell">O</td></tr>
  <tr><td class="category-cell" rowspan="5">Log message</td><td class="item-cell">Successfully retrieved Filter Seat Map for Zone/date/time combinations vary</td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully retrieved Filter Seat Map for Zone/date/time combinations vary (UTCID02)</td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Successfully retrieved Filter Seat Map for Zone/date/time combinations vary (UTCID03)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to retrieve Filter Seat Map: Invalid request parameters for Zone/date/time combinations vary</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td><td class="mark-cell"></td></tr>
  <tr><td class="item-cell">Failed to retrieve Filter Seat Map: Invalid request parameters for Zone/date/time combinations vary (UTCID05)</td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell"></td><td class="mark-cell">O</td></tr>
</table>
