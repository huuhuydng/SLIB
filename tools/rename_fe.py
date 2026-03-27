#!/usr/bin/env python3
"""
Rename and update FE test reports (HTML) and Java test files
according to the new FE numbering scheme.

Steps:
1. Copy all affected files to .tmp to avoid naming conflicts
2. Rename and update content from old FE numbers to new FE numbers
3. Delete removed FE-68
4. Create placeholder HTML reports for FE-122 to FE-127
5. Regenerate index.html
6. Print summary
"""

import os
import re
import shutil
import glob

# ============================================================
# CONFIGURATION
# ============================================================

HTML_DIR = "/Users/hadi/Desktop/slib/doc/Report/UnitTestReport/UnitTestHtml"
JAVA_DIR = "/Users/hadi/Desktop/slib/backend/src/test/java/slib/com/example/controller"

# Mapping: old_number -> new_number
# None means REMOVED
OLD_TO_NEW = {
    1: 1,
    2: 2,
    122: 3,  # Password Reset -> Forgot password
    3: 4,
    4: 5,
    5: 6,
    6: 7,
    7: 8,
    8: 9,
    9: 10,
    10: 11,
    11: 12,
    12: 13,
    13: 14,
    14: 15,
    15: 16,
    16: 17,
    17: 18,
    18: 19,
    19: 20,
    20: 21,
    21: 22,
    22: 23,
    23: 24,
    24: 25,
    25: 26,
    26: 27,
    27: 28,
    28: 29,
    29: 30,
    30: 31,
    31: 32,
    32: 33,
    33: 34,
    34: 35,
    35: 36,
    36: 37,
    37: 38,
    38: 39,
    39: 40,
    41: 41,  # stays same
    40: 42,  # swapped with 41
    42: 43,
    43: 44,
    44: 45,
    45: 46,
    46: 47,
    47: 48,
    48: 49,
    49: 50,
    50: 51,
    51: 52,
    52: 53,
    53: 54,
    54: 55,
    55: 56,
    56: 57,
    57: 58,
    58: 59,
    59: 60,
    60: 61,
    61: 62,
    62: 63,
    63: 64,
    64: 65,
    65: 66,
    66: 67,
    67: 68,
    68: None,  # REMOVED
    69: 69,
    70: 70,
    71: 71,
    72: 72,
    73: 73,
    74: 74,
    75: 75,
    76: 76,
    77: 77,
    78: 78,
    79: 79,
    80: 80,
    81: 81,
    82: 82,
    83: 83,
    84: 84,
    85: 85,
    86: 86,
    87: 87,
    88: 88,
    89: 89,
    90: 90,
    91: 91,
    92: 92,
    93: 93,
    94: 94,
    95: 95,
    96: 96,
    97: 97,
    98: 98,
    99: 99,
    100: 100,
    101: 101,
    102: 102,
    103: 103,
    104: 104,
    105: 105,
    106: 106,
    107: 107,
    108: 108,
    109: 109,
    110: 110,
    111: 111,
    112: 112,
    113: 113,
    114: 114,
    115: 115,
    116: 116,
    117: 117,
    118: 118,
    119: 119,
    120: 120,
    121: 121,
}

NEW_FE_NAMES = {
    1: "Login with Google Account",
    2: "Login with SLIB Account",
    3: "Forgot password",
    4: "Log out",
    5: "View profile",
    6: "Change basic profile",
    7: "Change password",
    8: "View Barcode",
    9: "View history of activities",
    10: "View account setting",
    11: "Turn on/Turn off notification",
    12: "Turn on/Turn off AI suggestion",
    13: "Turn on/Turn off HCE feature",
    14: "View list of users in the system",
    15: "Import Student via file",
    16: "Download template of the Student upload file",
    17: "Add Librarian to the system",
    18: "View user details",
    19: "Change user status",
    20: "Delete user account",
    21: "View area map",
    22: "CRUD area",
    23: "Change area status",
    24: "Lock area movement",
    25: "View zone map",
    26: "CRUD zone",
    27: "CRUD zone attribute",
    28: "View zone details",
    29: "Lock zone movement",
    30: "View seat map",
    31: "CRUD seat",
    32: "Change seat status",
    33: "View list of reputation rules",
    34: "CRUD reputation rule",
    35: "Set the deducted point for each reputation rule",
    36: "Set library operating hours",
    37: "Configure booking rules",
    38: "Turn on/Turn off automatic check-out when time exceeds",
    39: "Enable/Disable library",
    40: "View HCE scan stations",
    41: "View HCE scan stations details",
    42: "Manage HCE station registration",
    43: "CRUD material",
    44: "View list of materials",
    45: "CRUD knowledge store",
    46: "View list of knowledge stores",
    47: "Test AI chat",
    48: "Manage NFC Tag UID mapping",
    49: "View NFC Tag mapping list",
    50: "View NFC Tag mapping details",
    51: "View list of Kiosk images",
    52: "CRUD Kiosk image",
    53: "Change image status",
    54: "Preview Kiosk display",
    55: "Config system notification",
    56: "View system overview information",
    57: "View system log",
    58: "Backup data manually",
    59: "Set automatic backup schedule",
    60: "View real time seat map",
    61: "Filter seat map",
    62: "View map density",
    63: "Booking seat",
    64: "Preview booking information",
    65: "Confirm booking via NFC",
    66: "View history of booking",
    67: "Cancel booking",
    68: "Ask AI for recommending seat",
    69: "View list of Student bookings",
    70: "Search and Filter Student booking",
    71: "View booking details and status",
    72: "Cancel invalid booking",
    73: "Check-in/Check-out library via HCE",
    74: "Check-in/Check-out library via QR code",
    75: "View history of check-ins/check-outs",
    76: "View list of Students access to library",
    77: "View reputation score",
    78: "View history of changed reputation points",
    79: "View detailed reason for deducting point",
    80: "View list of Students violation",
    81: "View Student violation details",
    82: "Create complaint",
    83: "View history of sending complaint",
    84: "View list of complaints",
    85: "View complaint details",
    86: "Verify complaint",
    87: "Create feedback after check-out",
    88: "View list of feedbacks",
    89: "View feedback details",
    90: "Create seat status report",
    91: "View history of sending seat status report",
    92: "View list of seat status reports",
    93: "View seat status report details",
    94: "Verify seat status report",
    95: "Create report seat violation",
    96: "View history of sending report seat violation",
    97: "View list of seat violation reports",
    98: "View report seat violation details",
    99: "Verify seat violation report",
    100: "View and delete list of notifications",
    101: "View notification details",
    102: "Filter notification",
    103: "Mark notification as read",
    104: "View list of news & announcements",
    105: "View news & announcement details",
    106: "View list of news & announcement categories",
    107: "View list of new books",
    108: "View basic information of new book",
    109: "CRUD new book",
    110: "CRUD news & announcement",
    111: "CRUD news & announcement category",
    112: "Set time to post news & announcement",
    113: "Save news & announcement draft",
    114: "Chat with AI virtual assistant",
    115: "Chat with Librarian",
    116: "View history of chat",
    117: "View list of chats",
    118: "View chat details",
    119: "Response to Student manually",
    120: "Response to Student with AI suggestion",
    121: "View general analytics dashboard",
    122: "View violation statistics",
    123: "View statistics of density forecast by using AI",
    124: "View check-in/check-out statistics",
    125: "View seat booking statistics",
    126: "Export seat & maintenance report",
    127: "Export general analytical report",
}

PLACEHOLDER_TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>FE-{num:02d} Test Report</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="page">
    <div class="nav"><a href="index.html">Back to index</a></div>
    <h1>Unit Test Report - FE-{num:02d}: {name}</h1>
<table class="report-meta">
  <tr>  <td class="label">Function Code</td>  <td class="value italic">FE-{num:02d}</td>  <td class="label">Function Name</td>  <td class="value link">{name}</td></tr>
  <tr>  <td class="label">Created By</td>  <td class="value italic"></td>  <td class="label">Executed By</td>  <td class="value italic"></td></tr>
  <tr>  <td class="label">Lines of code</td>  <td class="value italic center">~0</td>  <td class="label">Lack of test cases</td>  <td class="value center">0</td></tr>
  <tr>  <td class="label">Class Under Test</td>  <td class="value code" colspan="3"><code>N/A</code></td></tr>
  <tr>  <td class="label">Test requirement</td>  <td class="value italic" colspan="3">N/A</td></tr>
  <tr>  <th class="stats-head center">Passed</th>  <th class="stats-head center">Failed</th>  <th class="stats-head center">Untested</th>  <th class="stats-head center">N / A / B</th>  <th class="stats-head center">Total Test Cases</th></tr>
  <tr>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">0</td>  <td class="value center">0 / 0 / 0</td>  <td class="value center">0</td></tr>
</table>
  </div>
</body>
</html>"""


def fmt_fe_code(num):
    """Format FE number with zero-padded 2 digits: FE-01, FE-02, ..., FE-127"""
    return f"FE-{num:02d}"


def fmt_fe_prefix(num):
    """Format FE prefix for filenames: FE01, FE02, ..., FE127"""
    return f"FE{num:02d}"


def old_fmt_fe_prefix(num):
    """Format FE prefix as used in old files (no zero padding for >= 10): FE01..FE09, FE10..FE122"""
    if num < 10:
        return f"FE0{num}"
    return f"FE{num}"


def old_fmt_fe_code(num):
    """Format FE code as used in old HTML content: FE-01..FE-09, FE-10..FE-122"""
    if num < 10:
        return f"FE-0{num}"
    return f"FE-{num}"


# ============================================================
# MAIN SCRIPT
# ============================================================

def main():
    summary = {
        "html_renamed": [],
        "html_content_updated": [],
        "html_deleted": [],
        "html_created": [],
        "java_renamed": [],
        "java_content_updated": [],
        "java_deleted": [],
        "index_regenerated": False,
        "errors": [],
    }

    # --------------------------------------------------------
    # PHASE 1: HTML REPORTS
    # --------------------------------------------------------
    print("=" * 60)
    print("PHASE 1: Processing HTML Test Reports")
    print("=" * 60)

    # Step 1a: Copy all HTML files that need renaming to .tmp
    print("\nStep 1a: Copying HTML files to temp names...")
    html_tmp_map = {}  # old_num -> tmp_path
    for old_num, new_num in OLD_TO_NEW.items():
        old_prefix = old_fmt_fe_prefix(old_num)
        old_file = os.path.join(HTML_DIR, f"{old_prefix}_TestReport.html")
        if os.path.exists(old_file):
            tmp_file = old_file + ".tmp"
            shutil.copy2(old_file, tmp_file)
            html_tmp_map[old_num] = tmp_file
            print(f"  Copied {old_prefix}_TestReport.html -> .tmp")
        else:
            print(f"  WARNING: {old_file} not found, skipping")
            summary["errors"].append(f"HTML not found: {old_file}")

    # Step 1b: Delete all old HTML files (that are in the mapping)
    print("\nStep 1b: Removing old HTML files...")
    for old_num in OLD_TO_NEW:
        old_prefix = old_fmt_fe_prefix(old_num)
        old_file = os.path.join(HTML_DIR, f"{old_prefix}_TestReport.html")
        if os.path.exists(old_file):
            os.remove(old_file)

    # Step 1c: Rename tmp files to new names and update content
    print("\nStep 1c: Renaming and updating HTML files...")
    for old_num, new_num in OLD_TO_NEW.items():
        if new_num is None:
            # REMOVED - delete the tmp file
            tmp_file = html_tmp_map.get(old_num)
            if tmp_file and os.path.exists(tmp_file):
                os.remove(tmp_file)
                old_prefix = old_fmt_fe_prefix(old_num)
                print(f"  DELETED: {old_prefix}_TestReport.html (FE-{old_num} removed)")
                summary["html_deleted"].append(f"FE{old_num:02d}_TestReport.html")
            continue

        tmp_file = html_tmp_map.get(old_num)
        if not tmp_file or not os.path.exists(tmp_file):
            continue

        new_prefix = fmt_fe_prefix(new_num)
        new_file = os.path.join(HTML_DIR, f"{new_prefix}_TestReport.html")

        # Read content
        with open(tmp_file, "r", encoding="utf-8") as f:
            content = f.read()

        # Update content
        old_code = old_fmt_fe_code(old_num)  # e.g., FE-03 or FE-122
        new_code = fmt_fe_code(new_num)      # e.g., FE-04
        new_name = NEW_FE_NAMES[new_num]

        # Replace FE code in title
        content = re.sub(
            r'<title>FE-\d+ Test Report</title>',
            f'<title>{new_code} Test Report</title>',
            content
        )

        # Replace in h1: "Unit Test Report - FE-XX: ..."
        content = re.sub(
            r'<h1>Unit Test Report - FE-\d+:\s*[^<]*</h1>',
            f'<h1>Unit Test Report - {new_code}: {new_name}</h1>',
            content
        )

        # Replace Function Code value
        content = re.sub(
            r'(Function Code</td>\s*<td class="value italic">)FE-\d+(</td>)',
            rf'\g<1>{new_code}\2',
            content
        )

        # Replace Function Name value
        content = re.sub(
            r'(Function Name</td>\s*<td class="value link">)[^<]*(</td>)',
            rf'\g<1>{new_name}\2',
            content
        )

        # Write to new file
        with open(new_file, "w", encoding="utf-8") as f:
            f.write(content)

        # Remove tmp file
        os.remove(tmp_file)

        old_prefix = old_fmt_fe_prefix(old_num)
        if old_num != new_num:
            print(f"  Renamed: {old_prefix}_TestReport.html -> {new_prefix}_TestReport.html (FE-{old_num:02d} -> {new_code})")
            summary["html_renamed"].append(f"{old_prefix} -> {new_prefix}")
        else:
            print(f"  Updated content: {new_prefix}_TestReport.html ({new_code}: {new_name})")
        summary["html_content_updated"].append(f"{new_prefix}_TestReport.html")

    # Step 1d: Create placeholder HTML reports for FE-122 to FE-127
    print("\nStep 1d: Creating placeholder HTML reports (FE-122 to FE-127)...")
    for num in range(122, 128):
        new_prefix = fmt_fe_prefix(num)
        new_file = os.path.join(HTML_DIR, f"{new_prefix}_TestReport.html")
        name = NEW_FE_NAMES[num]
        content = PLACEHOLDER_TEMPLATE.format(num=num, name=name)
        with open(new_file, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"  Created: {new_prefix}_TestReport.html ({fmt_fe_code(num)}: {name})")
        summary["html_created"].append(f"{new_prefix}_TestReport.html")

    # --------------------------------------------------------
    # PHASE 2: JAVA TEST FILES
    # --------------------------------------------------------
    print("\n" + "=" * 60)
    print("PHASE 2: Processing Java Test Files")
    print("=" * 60)

    # Build a map of old_num -> (old_filename, old_suffix)
    # e.g., 3 -> ("FE03_LogoutTest.java", "LogoutTest")
    java_files = {}
    for fname in os.listdir(JAVA_DIR):
        if not fname.endswith("Test.java"):
            continue
        m = re.match(r'^FE(\d+)_(.+)\.java$', fname)
        if m:
            num = int(m.group(1))
            suffix = m.group(2)
            java_files[num] = (fname, suffix)

    # Step 2a: Copy all Java files to .tmp
    print("\nStep 2a: Copying Java files to temp names...")
    java_tmp_map = {}  # old_num -> (tmp_path, suffix)
    for old_num, new_num in OLD_TO_NEW.items():
        if old_num not in java_files:
            continue
        fname, suffix = java_files[old_num]
        old_file = os.path.join(JAVA_DIR, fname)
        tmp_file = old_file + ".tmp"
        shutil.copy2(old_file, tmp_file)
        java_tmp_map[old_num] = (tmp_file, suffix)
        print(f"  Copied {fname} -> .tmp")

    # Step 2b: Delete all old Java files (that are in the mapping)
    print("\nStep 2b: Removing old Java files...")
    for old_num in OLD_TO_NEW:
        if old_num not in java_files:
            continue
        fname = java_files[old_num][0]
        old_file = os.path.join(JAVA_DIR, fname)
        if os.path.exists(old_file):
            os.remove(old_file)

    # Step 2c: Rename and update Java files
    print("\nStep 2c: Renaming and updating Java files...")
    for old_num, new_num in OLD_TO_NEW.items():
        if new_num is None:
            # REMOVED
            if old_num in java_tmp_map:
                tmp_file, suffix = java_tmp_map[old_num]
                if os.path.exists(tmp_file):
                    os.remove(tmp_file)
                    print(f"  DELETED: FE{old_num}_{suffix}.java (FE-{old_num} removed)")
                    summary["java_deleted"].append(f"FE{old_num}_{suffix}.java")
            continue

        if old_num not in java_tmp_map:
            continue

        tmp_file, suffix = java_tmp_map[old_num]
        old_prefix_no_pad = old_fmt_fe_prefix(old_num)  # FE03, FE122
        new_prefix = fmt_fe_prefix(new_num)              # FE04, FE03
        old_code = old_fmt_fe_code(old_num)              # FE-03, FE-122
        new_code = fmt_fe_code(new_num)                  # FE-04, FE-03

        new_fname = f"{new_prefix}_{suffix}.java"
        new_file = os.path.join(JAVA_DIR, new_fname)

        # Read content
        with open(tmp_file, "r", encoding="utf-8") as f:
            content = f.read()

        # Update class name: FE03_LogoutTest -> FE04_LogoutTest
        old_class = f"{old_prefix_no_pad}_{suffix}"
        new_class = f"{new_prefix}_{suffix}"
        content = content.replace(old_class, new_class)

        # Update @DisplayName with FE code: "FE-03:" -> "FE-04:"
        # Handle both zero-padded and non-zero-padded old codes
        content = re.sub(
            rf'FE-0*{old_num}:',
            f'{new_code}:',
            content
        )

        # Update comment references: "FE-03" -> "FE-04" (in comments like "Unit Tests for FE-03:")
        content = re.sub(
            rf'(Unit Tests for )FE-0*{old_num}\b',
            rf'\g<1>{new_code}',
            content
        )

        # Update test report path references: FE03_TestReport -> FE04_TestReport
        content = content.replace(f"{old_prefix_no_pad}_TestReport", f"{new_prefix}_TestReport")

        # Write to new file
        with open(new_file, "w", encoding="utf-8") as f:
            f.write(content)

        # Remove tmp file
        os.remove(tmp_file)

        if old_num != new_num:
            print(f"  Renamed: {old_prefix_no_pad}_{suffix}.java -> {new_fname} ({old_code} -> {new_code})")
            summary["java_renamed"].append(f"{old_prefix_no_pad}_{suffix}.java -> {new_fname}")
        else:
            print(f"  Updated content: {new_fname} ({new_code})")
        summary["java_content_updated"].append(new_fname)

    # --------------------------------------------------------
    # PHASE 3: REGENERATE index.html
    # --------------------------------------------------------
    print("\n" + "=" * 60)
    print("PHASE 3: Regenerating index.html")
    print("=" * 60)

    index_path = os.path.join(HTML_DIR, "index.html")
    lines = [
        '<!DOCTYPE html>',
        '<html lang="en">',
        '<head>',
        '  <meta charset="UTF-8">',
        '  <meta name="viewport" content="width=device-width, initial-scale=1.0">',
        '  <title>Unit Test Reports</title>',
        '  <link rel="stylesheet" href="style.css">',
        '</head>',
        '<body>',
        '  <div class="page">',
        '    <h1>Unit Test Reports</h1>',
        '    <ul>',
    ]
    for num in range(1, 128):
        prefix = fmt_fe_prefix(num)
        name = NEW_FE_NAMES[num]
        code = fmt_fe_code(num)
        lines.append(f'      <li><a href="{prefix}_TestReport.html">{code}: {name}</a></li>')
    lines.extend([
        '    </ul>',
        '  </div>',
        '</body>',
        '</html>',
    ])
    with open(index_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    summary["index_regenerated"] = True
    print(f"  Generated index.html with {127} entries (FE-01 to FE-127)")

    # --------------------------------------------------------
    # PHASE 4: CLEANUP - remove any leftover .tmp files
    # --------------------------------------------------------
    for tmp in glob.glob(os.path.join(HTML_DIR, "*.tmp")):
        os.remove(tmp)
        print(f"  Cleaned up leftover: {tmp}")
    for tmp in glob.glob(os.path.join(JAVA_DIR, "*.tmp")):
        os.remove(tmp)
        print(f"  Cleaned up leftover: {tmp}")

    # --------------------------------------------------------
    # SUMMARY
    # --------------------------------------------------------
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"\nHTML Reports:")
    print(f"  Files renamed:         {len(summary['html_renamed'])}")
    print(f"  Files content updated: {len(summary['html_content_updated'])}")
    print(f"  Files deleted:         {len(summary['html_deleted'])}")
    print(f"  Files created (new):   {len(summary['html_created'])}")

    print(f"\nJava Test Files:")
    print(f"  Files renamed:         {len(summary['java_renamed'])}")
    print(f"  Files content updated: {len(summary['java_content_updated'])}")
    print(f"  Files deleted:         {len(summary['java_deleted'])}")

    print(f"\nIndex regenerated: {summary['index_regenerated']}")

    if summary["errors"]:
        print(f"\nErrors ({len(summary['errors'])}):")
        for err in summary["errors"]:
            print(f"  - {err}")

    print("\n--- Renamed HTML files ---")
    for item in summary["html_renamed"]:
        print(f"  {item}")

    print("\n--- Deleted HTML files ---")
    for item in summary["html_deleted"]:
        print(f"  {item}")

    print("\n--- Created HTML placeholders ---")
    for item in summary["html_created"]:
        print(f"  {item}")

    print("\n--- Renamed Java files ---")
    for item in summary["java_renamed"]:
        print(f"  {item}")

    print("\n--- Deleted Java files ---")
    for item in summary["java_deleted"]:
        print(f"  {item}")

    # Verify final file counts
    html_count = len([f for f in os.listdir(HTML_DIR) if f.endswith("_TestReport.html")])
    java_count = len([f for f in os.listdir(JAVA_DIR) if f.endswith("Test.java")])
    print(f"\nFinal file counts:")
    print(f"  HTML reports: {html_count} (expected: 127)")
    print(f"  Java tests:   {java_count} (expected: 121)")


if __name__ == "__main__":
    main()
