# Unit Test Report - FE-24: View Zone Map

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-24</td><td><b>Function Name</b></td><td>View Zone Map</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~10</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>ZoneService.getAllZones()</code> and <code>getZonesByAreaId(Long areaId)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify zone-map retrieval with and without area filter.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>3</td><td>0</td><td>0</td><td>1 / 2 / 0</td><td>3</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th></tr>
  <tr><td rowspan="5" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Zone request is made with or without area id</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Zone list comes from zone repository</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="2"><b>Mock State (Dependencies)</b></td><td>Repository returns zones for target area or all zones</td><td>O</td><td>O</td><td></td></tr>
  <tr><td>Repository returns empty result</td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Area filter is present or absent</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>Returns mapped zone list with geometry and area id</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>Chooses area-filtered or global repository path correctly</td><td>O</td><td>O</td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Repository/runtime failure is propagated</td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td><code>findByArea_AreaId()</code> or <code>findAll()</code> is called once</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td></tr>
</table>
