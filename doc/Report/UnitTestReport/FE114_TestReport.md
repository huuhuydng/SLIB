# Unit Test Report - FE-114: Chat with AI

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><td><b>Function Code</b></td><td>FE-114</td><td><b>Function Name</b></td><td>Chat with AI</td></tr>
  <tr><td><b>Created By</b></td><td>Hadi</td><td><b>Executed By</b></td><td>Hadi</td></tr>
  <tr><td><b>Lines of code</b></td><td>~15</td><td><b>Lack of test cases</b></td><td>0</td></tr>
  <tr><td><b>Class Under Test</b></td><td colspan="3"><code>slib.com.example.service.ai.ChatService.sendMessage(...)</code></td></tr>
  <tr><td><b>Test requirement</b></td><td colspan="3">Verify AI-chat processing, empty-message validation, session flow, and persistence side effects.</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr><th>Passed</th><th>Failed</th><th>Untested</th><th>N/A/B</th><th>Total Test Cases</th></tr>
  <tr><td>6</td><td>0</td><td>0</td><td>1 / 5 / 0</td><td>6</td></tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr class="matrix-head"><th>Section</th><th>Category</th><th>Item</th><th>UTCID01</th><th>UTCID02</th><th>UTCID03</th><th>UTCID04</th><th>UTCID05</th><th>UTCID06</th></tr>
  <tr><td rowspan="6" class="matrix-section"><b>Condition</b></td><td rowspan="2"><b>Precondition</b></td><td>Authenticated user and message are provided</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td>AI chat depends on session/message persistence and AI generation</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="3"><b>Mock State (Dependencies)</b></td><td>Session exists or is created successfully</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Message is empty or session is invalid</td><td></td><td></td><td></td><td>O</td><td>O</td><td></td></tr>
  <tr><td>AI generation throws runtime failure</td><td></td><td></td><td></td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="1"><b>Input</b></td><td>Message content is valid or invalid</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Confirm</b></td><td rowspan="2"><b>Return</b></td><td>AI response is generated and mapped into persisted chat history</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td>Session reuse/creation behaves correctly</td><td></td><td>O</td><td>O</td><td></td><td></td><td></td></tr>
  <tr><td rowspan="1"><b>Exception</b></td><td>Validation or AI failure is propagated</td><td></td><td></td><td></td><td>O</td><td>O</td><td>O</td></tr>
  <tr><td rowspan="1"><b>Log message / Interaction</b></td><td>Chat session and message repositories are updated during successful flows</td><td>O</td><td>O</td><td>O</td><td></td><td></td><td>O</td></tr>
  <tr><td rowspan="4" class="matrix-section"><b>Result</b></td><td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td><td>N</td><td>A</td><td>A</td><td>A</td><td>A</td><td>A</td></tr>
  <tr><td colspan="2">Passed/Failed</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td><td>P</td></tr>
  <tr><td colspan="2">Executed Date</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td></tr>
  <tr><td colspan="2">Defect ID</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
