# Unit Test Report - FE-04: View Profile

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr>
    <td><b>Function Code</b></td>
    <td>FE-04</td>
    <td><b>Function Name</b></td>
    <td>FE-04_View Profile</td>
  </tr>
  <tr>
    <td><b>Created By</b></td>
    <td>Hadi</td>
    <td><b>Executed By</b></td>
    <td>Hadi</td>
  </tr>
  <tr>
    <td><b>Lines of code</b></td>
    <td>18</td>
    <td><b>Lack of test cases</b></td>
    <td>0</td>
  </tr>
  <tr>
    <td><b>Class Under Test</b></td>
    <td colspan="3">`UserService.getMyProfile(String email)`</td>
  </tr>
  <tr>
    <td><b>Test requirement</b></td>
    <td colspan="3">Verify repository lookup, response-field mapping, and not-found exception behavior at backend service level.</td>
  </tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr>
    <th>Passed</th>
    <th>Failed</th>
    <th>Untested</th>
    <th>N/A/B</th>
    <th>Total Test Cases</th>
  </tr>
  <tr>
    <td>4</td>
    <td>0</td>
    <td>0</td>
    <td>2 / 1 / 1</td>
    <td>4</td>
  </tr>
</table>

<table border="1" cellspacing="0" cellpadding="4" style="border-collapse: collapse; width: 100%;">
  <tr style="background: #001a8d; color: #ffffff;">
    <th>Section</th>
    <th>Category</th>
    <th>Item</th>
    <th>UTCID01</th>
    <th>UTCID02</th>
    <th>UTCID03</th>
    <th>UTCID04</th>
  </tr>
  <tr>
    <td rowspan="7" style="background: #001a8d; color: #ffffff;"><b>Condition</b></td>
    <td rowspan="2"><b>Precondition</b></td>
    <td>Email value is provided</td>
    <td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td>Profile lookup depends on `UserRepository.findByEmail()`</td>
    <td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td rowspan="3"><b>Mock State (Dependencies)</b></td>
    <td>Repository returns librarian profile data</td>
    <td>O</td><td></td><td></td><td></td>
  </tr>
  <tr>
    <td>Repository returns admin profile data</td>
    <td></td><td>O</td><td></td><td></td>
  </tr>
  <tr>
    <td>Repository returns user with null optional fields</td>
    <td></td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td rowspan="2"><b>Input</b></td>
    <td>Lookup email exists in repository</td>
    <td>O</td><td>O</td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Lookup email does not exist in repository</td>
    <td></td><td></td><td></td><td>O</td>
  </tr>
  <tr>
    <td rowspan="7" style="background: #001a8d; color: #ffffff;"><b>Confirm</b></td>
    <td rowspan="4"><b>Return</b></td>
    <td>`UserProfileResponse` maps id, fullName, email, userCode, username, role, and active state correctly</td>
    <td>O</td><td>O</td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Optional fields `dob`, `phone`, `avtUrl`, and `passwordChanged` are mapped correctly</td>
    <td>O</td><td>O</td><td>O</td><td></td>
  </tr>
  <tr>
    <td>Admin role is preserved in the mapped response</td>
    <td></td><td>O</td><td></td><td></td>
  </tr>
  <tr>
    <td>Null optional fields remain null in response output</td>
    <td></td><td></td><td>O</td><td></td>
  </tr>
  <tr>
    <td><b>Exception</b></td>
    <td>Throws `RuntimeException` when user is not found</td>
    <td></td><td></td><td></td><td>O</td>
  </tr>
  <tr>
    <td rowspan="2"><b>Log message / Interaction</b></td>
    <td>`findByEmail()` is called exactly once</td>
    <td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td>No save operation is executed during profile retrieval</td>
    <td>O</td><td>O</td><td>O</td><td>O</td>
  </tr>
  <tr>
    <td rowspan="4" style="background: #001a8d; color: #ffffff;"><b>Result</b></td>
    <td colspan="2">Type (N: Normal, A: Abnormal, B: Boundary)</td>
    <td>N</td><td>N</td><td>B</td><td>A</td>
  </tr>
  <tr>
    <td colspan="2">Passed/Failed</td>
    <td>P</td><td>P</td><td>P</td><td>P</td>
  </tr>
  <tr>
    <td colspan="2">Executed Date</td>
    <td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td><td>2026-03-10</td>
  </tr>
  <tr>
    <td colspan="2">Defect ID</td>
    <td></td><td></td><td></td><td></td>
  </tr>
</table>
