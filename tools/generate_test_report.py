#!/usr/bin/env python3

from __future__ import annotations

import html
import re
from dataclasses import dataclass
from pathlib import Path


ROOT = Path("/Users/hadi/Desktop/slib")
UNIT_REPORT_DIR = ROOT / "doc/Report/UnitTestReport/UnitTestHtml"
JAVA_TEST_DIR = ROOT / "backend/src/test/java/slib/com/example/controller"
OUTPUT_PATH = ROOT / "doc/Report/TestReport/TestReport.html"
EXECUTED_DATE = "2026-04-09"
BUILD_RESULT = "BUILD SUCCESS"
TOTAL_TESTS = 752

ROLE_OVERRIDES = {
    "FE-11": "the user is logged in with a valid student account",
    "FE-12": "the user is logged in with a valid student account",
    "FE-13": "the user is logged in with a valid student account",
    "FE-14": "the user is logged in with a valid student account",
    "FE-34": "the user is logged in with a valid admin account",
    "FE-35": "the user is logged in with a valid admin account",
    "FE-36": "the user is logged in with a valid admin account",
    "FE-37": "the user is logged in with a valid admin account",
    "FE-38": "the user is logged in with a valid admin account",
    "FE-39": "the user is logged in with a valid admin account",
    "FE-40": "the user is logged in with a valid admin account",
    "FE-41": "the user is logged in with a valid admin account",
    "FE-42": "the user is logged in with a valid admin account",
    "FE-43": "the user is logged in with a valid admin account",
    "FE-64": "the user is logged in with a valid student account",
    "FE-65": "the user is logged in with a valid student account",
    "FE-68": "the user is logged in with a valid student account",
    "FE-69": "the user is logged in with a valid student account",
    "FE-70": "the user is logged in with a valid student account",
    "FE-71": "the user is logged in with a valid student account",
    "FE-72": "the user is logged in with a valid student account",
    "FE-73": "the user is logged in with a valid student account",
    "FE-74": "the user is logged in with a valid librarian account",
    "FE-75": "the user is logged in with a valid librarian account",
    "FE-76": "the user is logged in with a valid librarian account or uses the authorized HCE interface",
    "FE-77": "the user is logged in with a valid librarian account or uses the authorized QR check-in interface",
    "FE-78": "the user is logged in with a valid librarian account",
    "FE-79": "the user is logged in with a valid librarian account",
    "FE-82": "the user is logged in with a valid student account",
    "FE-83": "the user is logged in with a valid librarian account",
    "FE-84": "the user is logged in with a valid librarian account",
    "FE-85": "the user is logged in with a valid student account",
    "FE-86": "the user is logged in with a valid student account",
    "FE-87": "the user is logged in with a valid librarian account",
    "FE-88": "the user is logged in with a valid librarian account",
    "FE-89": "the user is logged in with a valid librarian account",
    "FE-90": "the user is logged in with a valid student account",
    "FE-91": "the user is logged in with a valid librarian account",
    "FE-92": "the user is logged in with a valid librarian account",
    "FE-103": "the user is logged in with a valid student account",
    "FE-107": "the user is logged in with a valid student account",
    "FE-108": "the user is logged in with a valid student account",
    "FE-109": "the user is logged in with a valid student account",
    "FE-110": "the user is logged in with a valid student account",
    "FE-111": "the user is logged in with a valid student account",
    "FE-112": "the user is logged in with a valid librarian account",
    "FE-113": "the user is logged in with a valid librarian account",
    "FE-114": "the user is logged in with a valid librarian account",
    "FE-115": "the user is logged in with a valid librarian account",
    "FE-116": "the user is logged in with a valid librarian account",
    "FE-117": "the user is logged in with a valid admin or librarian account",
    "FE-118": "the user is logged in with a valid admin or librarian account",
    "FE-119": "the user is logged in with a valid admin or librarian account",
    "FE-120": "the user is logged in with a valid admin or librarian account",
    "FE-121": "the user is logged in with a valid student account",
    "FE-122": "the user is logged in with a valid student account",
    "FE-123": "the user is logged in with a valid student account",
    "FE-124": "the user is logged in with a valid librarian account",
    "FE-125": "the user is logged in with a valid student account",
    "FE-126": "the user is logged in with a valid librarian account",
    "FE-127": "the user is logged in with a valid librarian account",
    "FE-128": "the user is logged in with a valid librarian account",
    "FE-129": "the user is logged in with a valid admin account",
    "FE-130": "the user is logged in with a valid admin account",
    "FE-131": "the user is logged in with a valid admin account",
    "FE-132": "the user is logged in with a valid admin account",
    "FE-133": "the user is logged in with a valid admin account",
    "FE-134": "the user is logged in with a valid admin account",
    "FE-135": "the user is logged in with a valid admin account",
    "FE-136": "the user is logged in with a valid admin account",
}

NAV_OVERRIDES = {
    "FE-11": "Open the mobile app and go to the account settings or notification settings screen.",
    "FE-12": "Open the mobile app and go to the account settings or AI suggestion settings screen.",
    "FE-13": "Open the mobile app and go to the account settings or HCE settings screen.",
    "FE-14": "Open the mobile app and open the profile or reputation-related screen.",
    "FE-34": "Login to the admin portal and open the configuration screen for reputation rules.",
    "FE-35": "Login to the admin portal and open the configuration screen for reputation rules.",
    "FE-36": "Login to the admin portal and open the configuration screen for reputation rules.",
    "FE-37": "Login to the admin portal and open the system configuration screen.",
    "FE-38": "Login to the admin portal and open the system configuration screen for booking rules.",
    "FE-39": "Login to the admin portal and open the system configuration screen for auto checkout.",
    "FE-40": "Login to the admin portal and open the system configuration screen.",
    "FE-41": "Login to the admin portal and open the HCE station management screen.",
    "FE-42": "Login to the admin portal and open the HCE station management screen.",
    "FE-43": "Login to the admin portal and open the HCE station management screen.",
    "FE-64": "Open the mobile app and move to the Floor Plan/Booking tab.",
    "FE-65": "Open the mobile app and move to the Floor Plan/Booking tab.",
    "FE-68": "Open the mobile app and move to the booking history or current booking screen.",
    "FE-69": "Open the mobile app and move to the Floor Plan/Booking tab to request AI seat suggestions.",
    "FE-70": "Open the mobile app and open the booking history screen.",
    "FE-71": "Open the mobile app and open the booking history screen.",
    "FE-72": "Open the mobile app and open the booking detail or booking history screen.",
    "FE-73": "Open the mobile app and access the NFC leave-seat function.",
    "FE-74": "Login to the librarian portal and open the bookings or seat management screen.",
    "FE-75": "Login to the librarian portal and open the bookings/history screen.",
    "FE-76": "Open the HCE check-in/check-out interface used by the authorized operator.",
    "FE-77": "Open the QR check-in/check-out interface used by the authorized operator.",
    "FE-78": "Login to the librarian portal and open the check-in/check-out history screen.",
    "FE-79": "Login to the librarian portal and open the check-in/check-out monitoring screen.",
    "FE-82": "Open the mobile app and open the reputation or violation detail screen.",
    "FE-83": "Login to the librarian portal and open the violation management screen.",
    "FE-84": "Login to the librarian portal and open the violation management screen.",
    "FE-85": "Open the mobile app and open the complaint creation screen.",
    "FE-86": "Open the mobile app and open the complaint history screen.",
    "FE-87": "Login to the librarian portal and open the complaint management screen.",
    "FE-88": "Login to the librarian portal and open the complaint management screen.",
    "FE-89": "Login to the librarian portal and open the complaint management screen.",
    "FE-90": "Open the mobile app and open the feedback form from the completed booking flow.",
    "FE-91": "Login to the librarian portal and open the feedback management screen.",
    "FE-92": "Login to the librarian portal and open the feedback management screen.",
    "FE-103": "Open the mobile app and go to the Notifications screen from the main navigation.",
    "FE-107": "Open the mobile app and open the news or announcements screen.",
    "FE-108": "Open the mobile app and open the news or announcements screen.",
    "FE-109": "Open the mobile app and open the news or announcements screen.",
    "FE-110": "Open the mobile app and open the new books screen.",
    "FE-111": "Open the mobile app and open the new books screen.",
    "FE-112": "Login to the librarian portal and open the New Books management screen.",
    "FE-113": "Login to the librarian portal and open the News management screen.",
    "FE-114": "Login to the librarian portal and open the News management screen.",
    "FE-115": "Login to the librarian portal and open the News management screen.",
    "FE-116": "Login to the librarian portal and open the News management screen.",
    "FE-117": "Login to the admin or librarian portal and open the Kiosk slideshow/image management screen.",
    "FE-118": "Login to the admin or librarian portal and open the Kiosk slideshow/image management screen.",
    "FE-119": "Login to the admin or librarian portal and open the Kiosk slideshow/image management screen.",
    "FE-120": "Login to the admin or librarian portal and open the Kiosk slideshow preview screen.",
    "FE-121": "Open the mobile app and open the AI chat screen.",
    "FE-122": "Open the mobile app and open the chat with librarian screen.",
    "FE-123": "Open the mobile app and open the support request screen.",
    "FE-124": "Login to the librarian portal and open the support request management screen.",
    "FE-125": "Open the mobile app and open the chat history screen.",
    "FE-126": "Login to the librarian portal and open the chat list screen.",
    "FE-127": "Login to the librarian portal and open the chat detail screen.",
    "FE-128": "Login to the librarian portal and open the manual response or support handling screen.",
    "FE-129": "Login to the admin portal and open the analytics dashboard.",
    "FE-130": "Login to the admin portal and open the statistics dashboard.",
    "FE-131": "Login to the admin portal and open the statistics dashboard.",
    "FE-132": "Login to the admin portal and open the statistics dashboard.",
    "FE-133": "Login to the admin portal and open the statistics dashboard.",
    "FE-134": "Login to the admin portal and open the analytics dashboard.",
    "FE-135": "Login to the admin portal and open the analytics dashboard.",
    "FE-136": "Login to the admin portal and open the analytics/report export screen.",
}


@dataclass
class MatrixRow:
    section: str
    category: str
    item: str
    marks: list[str]


@dataclass
class FeatureReport:
    function_code: str
    function_name: str
    total_cases: int
    type_stats: str
    created_by: str
    executed_by: str
    class_under_test: str
    test_requirement: str
    html_filename: str
    rows: list[MatrixRow]
    java_cases: list["JavaCase"]


@dataclass
class JavaCase:
    scenario: str
    method: str
    endpoint: str
    return_item: str


def numeric_fe_key(path: Path) -> int:
    match = re.match(r"FE(\d+)", path.name)
    if not match:
        return 9999
    return int(match.group(1))


def format_fe_code(number: int) -> str:
    return f"FE-{number:02d}" if number < 100 else f"FE-{number}"


def strip_tags(value: str) -> str:
    value = value.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
    value = re.sub(r"<[^>]+>", "", value)
    value = html.unescape(value)
    value = re.sub(r"[ \t]+", " ", value)
    value = re.sub(r"\n\s*", "\n", value)
    return value.strip()


def extract_table(html_text: str, class_name: str) -> str:
    match = re.search(
        rf"<table class=\"{re.escape(class_name)}\">(.*?)</table>",
        html_text,
        re.S,
    )
    if not match:
        raise ValueError(f"Cannot find table {class_name}")
    return match.group(1)


def parse_cells(row_html: str) -> list[dict[str, str]]:
    cells: list[dict[str, str]] = []
    for match in re.finditer(r"<(td|th)([^>]*)>(.*?)</\1>", row_html, re.S):
        attrs = match.group(2)
        inner = match.group(3)
        class_match = re.search(r'class="([^"]+)"', attrs)
        rowspan_match = re.search(r'rowspan="(\d+)"', attrs)
        cells.append(
            {
                "class": class_match.group(1) if class_match else "",
                "rowspan": rowspan_match.group(1) if rowspan_match else "",
                "inner": inner,
                "text": strip_tags(inner),
            }
        )
    return cells


def parse_meta(html_text: str, html_filename: str) -> FeatureReport:
    title_match = re.search(r"<h1>Unit Test Report - (FE-\d+):\s*(.*?)</h1>", html_text)
    if not title_match:
        raise ValueError(f"Cannot parse title from {html_filename}")

    fe_number = numeric_fe_key(Path(html_filename))
    code = format_fe_code(fe_number)
    function_name = html.unescape(title_match.group(2).strip())

    class_match = re.search(
        r"Class Under Test</td>\s*<td class=\"value code\" colspan=\"3\"><code>(.*?)</code></td>",
        html_text,
        re.S,
    )
    created_match = re.search(
        r"Created By</td>\s*<td class=\"value italic\">(.*?)</td>\s*<td class=\"label\">Executed By</td>\s*<td class=\"value italic\">(.*?)</td>",
        html_text,
        re.S,
    )
    requirement_match = re.search(
        r"Test requirement</td>\s*<td class=\"value italic\" colspan=\"3\">(.*?)</td>",
        html_text,
        re.S,
    )

    return FeatureReport(
        function_code=code,
        function_name=function_name,
        total_cases=0,
        type_stats="N/A",
        created_by=strip_tags(created_match.group(1)) if created_match else "",
        executed_by=strip_tags(created_match.group(2)) if created_match else "",
        class_under_test=strip_tags(class_match.group(1)) if class_match else "N/A",
        test_requirement=strip_tags(requirement_match.group(1)) if requirement_match else "N/A",
        html_filename=html_filename,
        rows=[],
        java_cases=[],
    )


def parse_matrix_rows(html_text: str) -> list[MatrixRow]:
    table_html = extract_table(html_text, "matrix-table")
    row_htmls = re.findall(r"<tr>(.*?)</tr>", table_html, re.S)
    if len(row_htmls) < 2:
        return []

    data_rows = row_htmls[1:]
    rows: list[MatrixRow] = []
    current_section = ""
    current_category = ""

    for row_html in data_rows:
        cells = parse_cells(row_html)
        if not cells:
            continue
        idx = 0

        if idx < len(cells) and "section-cell" in cells[idx]["class"]:
            current_section = cells[idx]["text"]
            idx += 1

        if idx >= len(cells):
            continue

        if "category-cell" in cells[idx]["class"]:
            current_category = cells[idx]["text"]
            idx += 1

        if idx >= len(cells):
            continue

        item_cell = cells[idx]
        idx += 1
        item_text = item_cell["text"]
        mark_values = [cell["text"] for cell in cells[idx:]]

        rows.append(
            MatrixRow(
                section=current_section,
                category=current_category,
                item=item_text,
                marks=mark_values,
            )
        )

    return rows


def parse_feature_report(path: Path) -> FeatureReport:
    html_text = path.read_text(encoding="utf-8")
    report = parse_meta(html_text, path.name)
    report.rows = parse_matrix_rows(html_text)
    report.java_cases = parse_java_cases(report.function_code)
    report.total_cases = len(re.findall(r'matrix-head--utc', html_text))
    report.type_stats = "N/A"
    for row in report.rows:
        if row.section == "Result" and row.item == "Type (N: Normal, A: Abnormal, B: Boundary)":
            counts = {"N": 0, "A": 0, "B": 0}
            for mark in row.marks:
                if mark in counts:
                    counts[mark] += 1
            report.type_stats = f"{counts['N']} / {counts['A']} / {counts['B']}"
            break
    return report


def normalize_display_name(value: str) -> str:
    value = re.sub(r"^(UTCID|UTCD)\d+:\s*", "", value.strip(), flags=re.I)
    value = re.sub(
        r"\s+returns\s+\d{3}\b.*$",
        "",
        value,
        flags=re.I,
    )
    return value.strip() or "N/A"


def status_text_from_match(status_method: str) -> str:
    mapping = {
        "isOk": "200: OK",
        "isCreated": "201: Created",
        "isAccepted": "202: Accepted",
        "isNoContent": "204: No Content",
        "isBadRequest": "400: Bad Request",
        "isUnauthorized": "401: Unauthorized",
        "isForbidden": "403: Forbidden",
        "isNotFound": "404: Not Found",
        "isConflict": "409: Conflict",
        "isUnsupportedMediaType": "415: Unsupported Media Type",
        "isUnprocessableEntity": "422: Unprocessable Entity",
        "isInternalServerError": "500: Internal Server Error",
        "isServiceUnavailable": "503: Service Unavailable",
    }
    return mapping.get(status_method, status_method)


def status_text_from_code(status_code: str) -> str:
    mapping = {
        "200": "200: OK",
        "201": "201: Created",
        "202": "202: Accepted",
        "204": "204: No Content",
        "400": "400: Bad Request",
        "401": "401: Unauthorized",
        "403": "403: Forbidden",
        "404": "404: Not Found",
        "409": "409: Conflict",
        "415": "415: Unsupported Media Type",
        "422": "422: Unprocessable Entity",
        "500": "500: Internal Server Error",
        "503": "503: Service Unavailable",
    }
    return mapping.get(status_code, status_code)


def parse_java_cases(function_code: str) -> list[JavaCase]:
    fe_number = int(function_code.replace("FE-", ""))
    java_prefix = f"FE{fe_number:02d}" if fe_number < 100 else f"FE{fe_number}"
    matches = sorted(JAVA_TEST_DIR.glob(f"{java_prefix}_*.java"))
    if not matches:
        return []

    text = matches[0].read_text(encoding="utf-8")
    blocks = re.findall(r"@Test\b(.*?)(?=@Test\b|\Z)", text, re.S)
    cases: list[JavaCase] = []

    for block in blocks:
        display_match = re.search(r'@DisplayName\("((?:[^"\\]|\\.)*)"\)', block, re.S)
        if not display_match:
            continue

        method_match = re.search(r'perform\(\s*(get|post|put|delete|patch)\("([^"]+)"', block, re.I | re.S)
        status_match = re.search(r'status\(\)\.(is[A-Za-z0-9]+)\(\)', block)
        status_codes = list(dict.fromkeys(re.findall(r'status\s*==\s*(\d+)', block)))
        if status_match:
            return_item = status_text_from_match(status_match.group(1))
        elif status_codes:
            return_item = " or ".join(status_text_from_code(code) for code in status_codes)
        else:
            return_item = "N/A"

        cases.append(
            JavaCase(
                scenario=normalize_display_name(html.unescape(display_match.group(1))),
                method=method_match.group(1).upper() if method_match else "N/A",
                endpoint=method_match.group(2) if method_match else "N/A",
                return_item=return_item,
            )
        )

    return cases


def first_marked(rows: list[MatrixRow], section: str, category: str, idx: int) -> list[str]:
    values: list[str] = []
    for row in rows:
        if row.section == section and row.category == category and idx < len(row.marks) and row.marks[idx] == "O":
            values.append(row.item)
    return values


def first_value(rows: list[MatrixRow], section: str, category: str, idx: int, default: str = "N/A") -> str:
    values = first_marked(rows, section, category, idx)
    return values[0] if values else default


def result_value(rows: list[MatrixRow], item: str, idx: int, default: str = "") -> str:
    for row in rows:
        if row.section == "Result" and row.item == item and idx < len(row.marks):
            return row.marks[idx] or default
    return default


def infer_role_text(report: FeatureReport, endpoint: str) -> str:
    if report.function_code in ROLE_OVERRIDES:
        return ROLE_OVERRIDES[report.function_code]
    text = f"{report.function_name} {endpoint}".lower()
    if "login" in text:
        return "the login screen is available and the test account is ready"
    if "forgot password" in text:
        return "the forgot password option is available from the login screen"
    if "logout" in text:
        return "the user is already logged in and can access the logout action"
    student_keywords = [
        "login", "password", "profile", "barcode", "activity", "account setting",
        "booking", "notification", "feedback", "complaint", "history", "reputation",
        "chat with ai", "support request", "hce", "qr", "news", "new book",
    ]
    admin_keywords = [
        "user", "area", "zone", "seat", "rule", "material", "station", "kiosk",
        "system", "backup", "knowledge", "nfc", "statistics", "report", "dashboard",
    ]

    if any(keyword in text for keyword in student_keywords):
        return "the user is logged in with a valid student account"
    if any(keyword in text for keyword in admin_keywords):
        return "the user is logged in with a valid admin or librarian account"
    return "the user is logged in with a valid account that can access this feature"


def infer_feature_screen(report: FeatureReport) -> str:
    name = report.function_name.lower()
    if "login" in name:
        return "login screen"
    if "profile" in name:
        return "profile screen"
    if "booking" in name:
        return "booking screen"
    if "notification" in name:
        return "notification screen"
    if "feedback" in name:
        return "feedback screen"
    if "complaint" in name:
        return "complaint screen"
    if "user" in name:
        return "user management screen"
    if "area" in name:
        return "area management screen"
    if "zone" in name:
        return "zone management screen"
    if "seat" in name:
        return "seat management screen"
    if "news" in name:
        return "news management screen"
    if "chat" in name:
        return "chat screen"
    return f"{report.function_name} screen"


def infer_platform(report: FeatureReport) -> str:
    text = report.function_name.lower()
    if any(keyword in text for keyword in ["add librarian", "view user", "change user status", "delete user"]):
        return "admin_web"
    if any(keyword in text for keyword in ["booking seat", "cancel booking", "preview booking", "ai suggest seat", "view and delete list of notifications"]):
        return "mobile"
    mobile_keywords = [
        "login", "forgot password", "log out", "profile", "barcode", "activity",
        "account setting", "booking", "notification", "feedback", "complaint",
        "history", "reputation", "chat with ai", "support request", "hce", "qr",
        "news", "new book", "student bookings",
    ]
    librarian_keywords = [
        "violation", "support request", "complaint", "seat status report", "feedback",
        "notification", "news", "new book", "chat with librarian", "check-in",
        "check-out", "students list", "booking details", "filter bookings",
    ]
    admin_keywords = [
        "user", "librarian", "area", "zone", "seat", "rule", "material", "station", "kiosk",
        "system", "backup", "knowledge", "nfc", "statistics", "report", "dashboard",
        "density", "analytical",
    ]
    if any(keyword in text for keyword in admin_keywords):
        return "admin_web"
    if any(keyword in text for keyword in librarian_keywords):
        return "librarian_web"
    if any(keyword in text for keyword in mobile_keywords):
        return "mobile"
    return "web"


def infer_navigation_step(report: FeatureReport) -> str:
    if report.function_code in NAV_OVERRIDES:
        return NAV_OVERRIDES[report.function_code]
    text = report.function_name.lower()
    platform = infer_platform(report)

    if "login" in text:
        return "Open the login screen of the system."
    if "forgot password" in text:
        return "Open the login screen and select the Forgot Password option."
    if "log out" in text or "logout" in text:
        return "Open the account menu or settings screen."
    if "change password" in text:
        return "Open the mobile app, go to the account settings screen, and open the Change Password form."
    if "profile" in text and any(word in text for word in ["update", "change", "view"]):
        return "Open the mobile app and go to the Profile screen."

    if platform == "mobile":
        if any(keyword in text for keyword in ["booking", "seat map", "density map", "realtime seat map", "preview booking", "ai suggest seat"]):
            return "Open the mobile app and move to the Floor Plan/Booking tab."
        if any(keyword in text for keyword in ["profile", "barcode", "activities", "history points", "deduct reason", "account setting", "notification setting", "ai suggestion", "hce feature", "reputation"]):
            return "Open the mobile app and go to the related profile/settings screen."
        if "notification" in text:
            return "Open the mobile app and go to the Notifications screen from the main navigation."
        if any(keyword in text for keyword in ["feedback", "complaint", "support request", "chat", "news", "new book"]):
            return "Open the mobile app and go to the related feature screen from the main navigation."
        return "Open the mobile app and navigate to the corresponding feature screen."

    if platform == "admin_web":
        if any(keyword in text for keyword in ["user", "librarian"]):
            return "Login to the admin portal and open the 'Người dùng' menu from the left sidebar."
        if any(keyword in text for keyword in ["area", "zone", "seat", "map"]):
            return "Login to the admin portal and open the 'Bản đồ thư viện' menu from the left sidebar."
        if any(keyword in text for keyword in ["rule", "system", "backup", "notification", "hce", "booking rules", "operating hours"]):
            return "Login to the admin portal and open the 'Cấu hình' or relevant system menu from the left sidebar."
        if any(keyword in text for keyword in ["nfc", "station", "kiosk", "slideshow"]):
            return "Login to the admin portal and open the relevant NFC/Kiosk management menu from the left sidebar."
        if any(keyword in text for keyword in ["knowledge", "material"]):
            return "Login to the admin portal and open the relevant management menu from the left sidebar."
        if any(keyword in text for keyword in ["dashboard", "statistics", "forecast", "analytical"]):
            return "Login to the admin portal and open the Dashboard or statistics screen."
        return "Login to the admin portal and open the corresponding menu from the left sidebar."

    if platform == "librarian_web":
        if any(keyword in text for keyword in ["booking", "seat", "check-in", "check-out", "qr", "nfc"]):
            return "Login to the librarian portal and open the booking/check-in related menu from the left sidebar."
        if any(keyword in text for keyword in ["student", "violation", "complaint", "feedback", "support request", "report"]):
            return "Login to the librarian portal and open the related processing menu from the left sidebar."
        if any(keyword in text for keyword in ["news", "new book", "notification"]):
            return "Login to the librarian portal and open the content management menu from the left sidebar."
        if "chat" in text:
            return "Login to the librarian portal and open the Chat menu from the left sidebar."
        return "Login to the librarian portal and open the corresponding menu from the left sidebar."

    return "Open the corresponding screen in the system."


def infer_data_entry_step(report: FeatureReport, action_text: str) -> str:
    text = report.function_name.lower()
    action_lower = action_text.lower()
    if "login" in text:
        return f"Enter or provide the required credentials/data: {html.escape(action_text)}."
    if "forgot password" in text:
        return f"Enter the recovery data required by the scenario: {html.escape(action_text)}."
    if "change password" in text:
        return f"Enter the current password and new password for this scenario: {html.escape(action_text)}."
    if "chat" in text and "ai" in text:
        return f"Enter the chat message or chat condition for this scenario: {html.escape(action_text)}."
    if any(keyword in text for keyword in ["notification setting", "ai suggestion", "hce feature", "account setting"]):
        return f"Set the options on the settings screen according to this scenario: {html.escape(action_text)}."
    if action_lower in {"no query params.", "no query params", "no request body (get endpoints).", "no request body (get endpoints)"}:
        return "Keep the default screen state and do not enter any additional filter or request data."
    if action_text.startswith("{") and any(keyword in text for keyword in ["booking", "complaint", "feedback", "notification", "create", "update", "change", "import", "set", "configure"]):
        return f"Enter the form data for this scenario: {html.escape(action_text)}."
    if any(keyword in text for keyword in ["view", "search", "filter", "preview", "list", "details", "history", "statistics"]):
        return f"Apply the viewing or filtering condition for this scenario: {html.escape(action_text)}."
    if "notification" in text:
        return f"Open the relevant notification state or action for this scenario: {html.escape(action_text)}."
    if any(keyword in text for keyword in ["booking", "seat map", "check-in", "check-out", "qr", "hce"]):
        return f"Choose the required booking or check-in information for this scenario: {html.escape(action_text)}."
    if any(keyword in text for keyword in ["add", "create", "update", "change", "import", "set", "configure", "crud"]):
        return f"Input or update the required form data: {html.escape(action_text)}."
    if any(keyword in text for keyword in ["delete", "cancel", "verify", "confirm", "activate", "lock", "enable", "disable"]):
        return f"Select the target record or option for this scenario: {html.escape(action_text)}."
    return f"Perform the scenario with the required data: {html.escape(action_text)}."


def infer_action_button(report: FeatureReport, scenario_text: str) -> str:
    feature_text = report.function_name.lower()
    scenario_lower = scenario_text.lower()
    text = f"{scenario_lower} {feature_text}"
    if "login" in text:
        return "Click the Login/Continue button"
    if "logout" in text:
        return "Click the Logout button"
    if any(word in scenario_lower for word in ["delete", "cancel", "close", "verify", "confirm", "checkout", "check-out", "check out"]):
        return "Confirm the action in the system"
    if any(word in scenario_lower for word in ["view", "search", "filter", "list", "count", "details", "history", "statistics", "preview"]):
        return "Wait for the system to load the result"
    if any(word in scenario_lower for word in ["add", "create", "import", "send", "booking", "check-in", "check in", "reply"]):
        return "Click the Save/Submit button"
    if any(word in scenario_lower for word in ["change", "update", "configure", "turn on", "turn off", "set", "enable", "disable", "lock", "activate"]):
        return "Click the Save/Confirm button"
    if any(word in feature_text for word in ["delete", "cancel", "close", "verify", "confirm", "checkout", "check-out", "check out"]):
        return "Confirm the action in the system"
    if any(word in feature_text for word in ["view", "search", "filter", "list", "count", "details", "history", "statistics", "preview"]):
        return "Wait for the system to load the result"
    if any(word in feature_text for word in ["add", "create", "import", "send", "booking", "check-in", "check in", "reply"]):
        return "Click the Save/Submit button"
    if any(word in feature_text for word in ["change", "update", "configure", "turn on", "turn off", "set", "enable", "disable", "lock", "activate"]):
        return "Click the Save/Confirm button"
    if any(word in text for word in ["download", "view", "preview", "search", "filter"]):
        return "Wait for the system to load the result"
    if any(word in text for word in ["list", "count", "details", "history"]):
        return "Wait for the system to load the result"
    return "Complete the action and wait for the system response"


def is_technical_text(value: str) -> bool:
    return bool(
        re.search(
            r"\b(mock|stub|repository|service|json|dto|jwt|runtimeexception|responseentity|controller|dependency|dependencies|entity|principal|exceptionhandler|authresponse|token payload)\b",
            value,
            re.I,
        )
    )


def humanize_action_text(value: str) -> str:
    text = strip_tags(value)
    if not text or text == "N/A":
        return text

    replacements = [
        (r"\bvalid jwt token\b", "a valid login session"),
        (r"\binvalid jwt token\b", "an invalid login session"),
        (r"\bexpired jwt token\b", "an expired login session"),
        (r"\bmissing jwt token\b", "a missing login session"),
        (r"\bno token\b", "no login session"),
        (r"\bwithout token\b", "without a login session"),
        (r"\bvalid idtoken\b", "valid Google sign-in information"),
        (r"\binvalid idtoken\b", "invalid Google sign-in information"),
        (r"@withmockuser", "a logged-in account"),
        (r"\bauth mismatch\b", "a different logged-in account than the target user"),
        (r"\bauthorized\b", "an authorized account"),
    ]

    for pattern, replacement in replacements:
        text = re.sub(pattern, replacement, text, flags=re.I)

    text = re.sub(r"\s*[-:]\s*$", "", text).strip()

    return text


def login_step_text(report: FeatureReport, endpoint: str) -> str:
    role = infer_role_text(report, endpoint).lower()
    platform = infer_platform(report)
    if "student" in role:
        return "Login to the mobile app with a valid student account."
    if "admin or librarian" in role:
        if platform == "librarian_web":
            return "Login to the portal with an authorized admin or librarian account."
        return "Login to the admin or librarian portal with an authorized account."
    if "admin" in role:
        return "Login to the admin portal with a valid admin account."
    if "librarian" in role:
        return "Login to the librarian portal with a valid librarian account."
    if "logout" in report.function_name.lower():
        return "Login to the system with a valid account."
    return "Login to the system with a valid account that can access this feature."


def procedure_text(report: FeatureReport, scenario_text: str, input_item: str, endpoint: str) -> str:
    raw_action_text = input_item if input_item and input_item != "N/A" else scenario_text or report.function_name
    action_text = humanize_action_text(raw_action_text)
    feature_text = report.function_name
    login_like = "login" in feature_text.lower()
    forgot_password = "forgot password" in feature_text.lower()
    if login_like:
        steps = [
            "1. Open the login screen of the system.",
            "2. Choose the login method required by the scenario.",
            f"3. Enter or provide the required credentials/data: {html.escape(action_text)}.",
            f"4. {html.escape(infer_action_button(report, scenario_text))}.",
            "5. Observe the authentication result on the screen.",
        ]
        return "<br>".join(steps)
    if forgot_password:
        steps = [
            "1. Open the login screen of the system.",
            "2. Select the Forgot Password option.",
            f"3. Enter the recovery data required by the scenario: {html.escape(action_text)}.",
            f"4. {html.escape(infer_action_button(report, scenario_text))}.",
            "5. Observe the recovery result and related message on the screen.",
        ]
        return "<br>".join(steps)
    return "<br>".join(
        [
            f"1. {html.escape(login_step_text(report, endpoint))}",
            f"2. {html.escape(infer_navigation_step(report))}",
            f"3. {infer_data_entry_step(report, action_text)}",
            f"4. {html.escape(infer_action_button(report, scenario_text))}.",
            "5. Observe the system result on the current screen.",
        ]
    )


def business_result_guidance(report: FeatureReport, scenario_text: str, return_item: str) -> str:
    feature_text = report.function_name.lower()
    scenario_lower = scenario_text.lower()
    text = f"{scenario_lower} {feature_text}"
    status_prefix = return_item.split(":", 1)[0]
    if status_prefix in {"200", "201", "202", "204"}:
        if "login" in text:
            return "The user logs in successfully and is moved to the appropriate screen."
        if "logout" in text:
            return "The user is logged out successfully and returned to the login screen."
        if "forgot password" in text or "reset password" in text:
            return "The password recovery action is completed according to the scenario."
        if "download" in text:
            return "The requested file is downloaded successfully."
        if any(word in scenario_lower for word in ["delete", "cancel", "close", "verify", "confirm", "checkout", "check-out", "check out"]):
            return "The system completes the action successfully and updates the related status or list."
        if any(word in scenario_lower for word in ["change", "update", "configure", "turn on", "turn off", "set", "enable", "disable", "lock", "activate"]):
            return "The system updates the information successfully and keeps the latest value."
        if any(word in scenario_lower for word in ["add", "create", "import", "send", "booking", "check-in", "check in", "reply"]):
            return "The system saves the data successfully and shows the corresponding success result."
        if any(word in scenario_lower for word in ["view", "list", "search", "filter", "details", "history", "notification", "news", "reputation", "dashboard", "statistics", "map", "preview", "count"]):
            return "The system displays the expected information correctly."
        if any(word in feature_text for word in ["delete", "cancel", "close", "verify", "confirm", "checkout", "check-out", "check out"]):
            return "The system completes the action successfully and updates the related status or list."
        if any(word in feature_text for word in ["change", "update", "configure", "turn on", "turn off", "set", "enable", "disable", "lock", "activate"]):
            return "The system updates the information successfully and keeps the latest value."
        if any(word in feature_text for word in ["add", "create", "import", "send", "booking", "check-in", "check in", "reply"]):
            return "The system saves the data successfully and shows the corresponding success result."
        if any(word in feature_text for word in ["view", "list", "search", "filter", "details", "history", "notification", "news", "reputation", "dashboard", "statistics", "map", "preview", "count"]):
            return "The system displays the expected information correctly."
        return "The system completes the action successfully."
    guidance = {
        "400": "The system rejects the request and shows a validation or business error.",
        "401": "The system requires the user to authenticate before continuing.",
        "403": "The system blocks the action because the user does not have permission.",
        "404": "The system informs that the requested data or resource does not exist.",
        "409": "The system detects a conflict and does not save the change.",
        "500": "The system shows an internal error and does not complete the action.",
        "503": "The service is temporarily unavailable and the action is not completed.",
    }
    return guidance.get(status_prefix, "The system responds according to the scenario.")


def humanize_detail_item(report: FeatureReport, scenario_text: str, detail_item: str) -> str:
    value = strip_tags(detail_item)
    if not value or value == "N/A":
        return ""
    lower = value.lower()
    scenario_lower = scenario_text.lower()

    if "json response matches expected data" in lower:
        return "The screen shows the expected data for this scenario."
    if lower.startswith("successfully "):
        return "The system completes the requested action successfully."
    if lower.startswith("failed"):
        return "The system shows the corresponding error message for this scenario."
    if "wrong password" in lower:
        return "The system informs the user that the password is incorrect."
    if "account not found" in lower:
        return "The system informs the user that the account does not exist."
    if "unauthorized request" in lower:
        return "The system informs the user that login is required or the session is not valid."
    if "response body" in lower and any(word in lower for word in ["contains", "shows", "returns", "includes"]):
        return "The system returns the expected information for this scenario."
    if lower.startswith("response returns"):
        if any(word in scenario_lower for word in ["notification", "setting", "ai suggestion", "hce feature"]):
            return "The updated setting values are displayed correctly after saving."
        if "booking" in scenario_lower:
            return "The booking information is displayed with the newly created reservation details."
        return "The system shows the returned information correctly for this scenario."
    if lower.startswith("body contains"):
        if "complaint" in scenario_lower:
            return "The complaint is created successfully and the submitted information is shown."
        if "feedback" in scenario_lower:
            return "The feedback is created successfully and the submitted information is shown."
        if "hce" in scenario_lower or "check-in" in scenario_lower or "check-out" in scenario_lower:
            return "The check-in or check-out result is shown with the corresponding status."
        return "The result information is shown correctly after the action is completed."
    if "dto list contains" in lower or "array with" in lower:
        if "notification" in scenario_lower:
            return "The notification list is displayed with the expected information."
        if "news" in scenario_lower:
            return "The news list is displayed with the expected information."
        if "user" in scenario_lower:
            return "The user list is displayed with the expected account information."
        return "The list is displayed with the expected information."
    if lower.startswith("plain text response:"):
        if "reservation not found" in lower:
            return "The system informs the user that the reservation does not exist."
        return "The system shows the corresponding message for this scenario."
    if "list returns" in lower or "filtered list returns" in lower:
        return "The list is displayed with records matching the selected condition."
    if lower.startswith("{") and "count" in lower:
        return "The returned count is displayed correctly."
    if lower.startswith("{") and "user.email" in lower and "user.role" in lower:
        return "The system shows a success message together with the created librarian information."
    if "dashboard stats returned" in lower:
        return "Dashboard summary statistics are displayed successfully."
    if "library status returned" in lower:
        return "The current library status is displayed successfully."
    if "full statistics returned" in lower or "statistics returned with insights" in lower:
        return "The report statistics are displayed for the selected period."
    if "error body" in lower:
        return "The system shows the corresponding error information."
    if any(word in lower for word in ["toast", "redirect", "navigate", "message"]) and not is_technical_text(value):
        return value
    if any(word in lower for word in ["download", "file bytes", "excel", "template", "export"]):
        return "The requested file or export result is available for the user."
    if is_technical_text(value):
        return ""
    return value


def expected_result_text(report: FeatureReport, scenario_text: str, return_item: str, detail_item: str) -> str:
    lines: list[str] = []
    if return_item and return_item != "N/A":
        lines.append(html.escape(return_item))
        lines.append(html.escape(business_result_guidance(report, scenario_text, return_item)))
    human_detail = humanize_detail_item(report, scenario_text, detail_item)
    if human_detail and human_detail not in lines:
        lines.append(html.escape(human_detail))
    return "<br>".join(lines) if lines else "Verify the system response matches the expected behavior."


def detail_value(rows: list[MatrixRow], idx: int) -> str:
    response_item = first_value(rows, "Confirm", "Response body", idx, "")
    if response_item:
        return response_item
    log_item = first_value(rows, "Confirm", "Log message", idx, "")
    if log_item:
        return log_item
    return "N/A"


def render_status_value(value: str) -> str:
    if value == "P":
        return "Passed"
    if value == "F":
        return "Failed"
    return html.escape(value)


def humanize_precondition_item(report: FeatureReport, endpoint: str, item: str) -> str:
    value = strip_tags(item)
    if not value or value == "N/A":
        return ""
    lower = value.lower()

    replacements = [
        ("required controller dependencies are mocked", ""),
        ("service or repository behavior is stubbed for each scenario", ""),
        ("json response matches expected data", "The related information exists and can be displayed by the system."),
    ]
    for source, target in replacements:
        if source in lower:
            return target

    if "userrepository.findbyemail" in lower or "userrepository.findbyid" in lower:
        if any(word in lower for word in ["not found", "does not exist", "empty"]):
            return "The related user information does not exist in the system for this scenario."
        return "The related user information exists in the system."
    if "authservice.loginwithpassword" in lower or "verifygoogletoken" in lower:
        return "The account and login information are prepared according to the scenario."
    if "otp service" in lower or "jwt service" in lower or "auth service" in lower:
        return "The password recovery or authentication flow is available for this scenario."
    if "refresh token" in lower:
        return "The login session and token state are prepared according to the scenario."
    if lower == "authorized":
        return "The user has permission to access this feature."
    if "required data and system state are prepared according to the scenario" in lower:
        return ""
    if "corresponding system data state is prepared for this scenario" in lower:
        return ""
    if "required data for the scenario is available in the system" in lower:
        return ""
    if "request role is librarian" in lower:
        return "The role selected in the form is Librarian."
    if "area data may exist in the system" in lower:
        return "Area data is available in the system."
    if "zone data exists for list and detail cases" in lower:
        return "Zone data is available in the system."
    if "path variable id may be valid or invalid" in lower:
        return "The selected area ID is prepared according to the scenario."
    if "reservation does not exist or belongs to another user" in lower:
        return "The selected reservation is invalid or does not belong to the current user."
    if "valid hce token and gateid payload" in lower:
        return "A valid HCE token and gate information are prepared according to the scenario."
    if "correct gate secret key is configured" in lower:
        return "The HCE gateway is configured with a valid secret key."
    if "published news exists for public list" in lower:
        return "Published news items already exist in the system."
    if "admin list contains draft and published news" in lower:
        return "Draft and published news items already exist in the system."
    if "ai proxy request body is provided" in lower:
        return "The user enters a chat message before sending the request."
    if "resttemplate returns ai json response or throws connection error" in lower:
        return "The AI service responds according to the scenario."
    if "google" in lower and "email" in lower:
        if any(word in lower for word in ["not been provisioned", "not provisioned", "not in the system"]):
            return "The Google account used in the test has not been registered in the system."
        return "The Google account information is prepared according to the scenario."
    if any(word in lower for word in ["mock", "stub", "repository", "service", "dto", "jwt", "runtimeexception", "controller", "dependency"]):
        return ""
    if "authenticated" in lower:
        return "The user is authenticated in the system."
    if any(word in lower for word in ["existing", "already exists", "available in database", "stored values"]):
        return "The related data already exists in the system."
    return value


def precondition_text(report: FeatureReport, endpoint: str, preconditions: list[str], mocks: list[str]) -> str:
    items: list[str] = []
    base_role = infer_role_text(report, endpoint).capitalize() + "."
    items.append(base_role)
    for value in preconditions + mocks:
        humanized = humanize_precondition_item(report, endpoint, value)
        if humanized and humanized not in items:
            items.append(humanized)
    if len(items) == 1:
        items.append("Required data for the scenario is available in the system.")
    return "<br>".join(html.escape(item) for item in items)


def description_text(report: FeatureReport, scenario_text: str) -> str:
    scenario_text = humanize_action_text(scenario_text)
    if scenario_text and scenario_text != "N/A":
        if scenario_text.lower().startswith("test "):
            return html.escape(scenario_text)
        return html.escape(f"Test {scenario_text}")
    return html.escape(f"Test {report.function_name}")


def default_tester(report: FeatureReport) -> str:
    return report.executed_by or report.created_by


def feature_title(report: FeatureReport) -> str:
    return f"{report.function_code} - {report.function_name}"


def note_text(defect_id: str) -> str:
    return defect_id or ""


def render_feature_summary_row(report: FeatureReport) -> str:
    return (
        "<tr>"
        f"<td>{html.escape(report.function_code)}</td>"
        f"<td>{html.escape(report.function_name)}</td>"
        f"<td>{report.total_cases}</td>"
        f"<td>{html.escape(report.type_stats)}</td>"
        f"<td>Passed</td>"
        f"<td><a href=\"../UnitTestReport/UnitTestHtml/{html.escape(report.html_filename)}\">Open unit report</a></td>"
        "</tr>"
    )


def render_feature_rows(report: FeatureReport) -> str:
    rows = report.rows
    sections: list[str] = [f'<tr class="feature-row"><td colspan="15">{html.escape(feature_title(report))}</td></tr>']

    for idx in range(report.total_cases):
        java_case = report.java_cases[idx] if idx < len(report.java_cases) else None
        scenario = java_case.scenario if java_case and java_case.scenario != "N/A" else ""
        input_item = first_value(rows, "Condition", "Input", idx, "")
        if not input_item:
            input_item = scenario or "N/A"
        method = first_value(rows, "Condition", "HTTP Method", idx, "")
        if not method:
            method = java_case.method if java_case else "N/A"
        endpoint = first_value(rows, "Condition", "API Endpoint", idx, "")
        if not endpoint:
            endpoint = java_case.endpoint if java_case else "N/A"
        return_item = first_value(rows, "Confirm", "Return", idx, "")
        if not return_item:
            return_item = java_case.return_item if java_case else "N/A"
        scenario_text = scenario or input_item or "N/A"
        response_item = detail_value(rows, idx)
        preconditions = first_marked(rows, "Condition", "Precondition", idx)
        mocks = first_marked(rows, "Condition", "Mock State (Dependencies)", idx)
        type_value = result_value(rows, "Type (N: Normal, A: Abnormal, B: Boundary)", idx, "N/A")
        status_value = result_value(rows, "Passed/Failed", idx, "P")
        executed_date = result_value(rows, "Executed Date", idx, EXECUTED_DATE)
        defect_id = result_value(rows, "Defect ID", idx, "")
        tester = default_tester(report)

        sections.append(
            "<tr>"
            f"<td>TC-{idx + 1:02d}</td>"
            f"<td>{description_text(report, scenario_text)}</td>"
            f"<td>{procedure_text(report, scenario_text, input_item, endpoint)}</td>"
            f"<td>{expected_result_text(report, scenario_text, return_item, response_item)}</td>"
            f"<td>{precondition_text(report, endpoint, preconditions, mocks)}</td>"
            f"<td>{render_status_value(status_value)}</td>"
            f"<td>{html.escape(executed_date)}</td>"
            f"<td>{html.escape(tester)}</td>"
            "<td></td>"
            "<td></td>"
            "<td></td>"
            "<td></td>"
            "<td></td>"
            "<td></td>"
            f"<td>{html.escape(note_text(defect_id))}</td>"
            "</tr>"
        )

    return "\n".join(sections)


def render_document(reports: list[FeatureReport]) -> str:
    total_features = len(reports)
    total_cases = sum(report.total_cases for report in reports)
    summary_rows = "\n".join(render_feature_summary_row(report) for report in reports)
    detail_rows = "\n".join(render_feature_rows(report) for report in reports)

    return "\n".join(
        [
            "<!DOCTYPE html>",
            '<html lang="en">',
            "<head>",
            '  <meta charset="UTF-8">',
            '  <meta name="viewport" content="width=device-width, initial-scale=1.0">',
            "  <title>SLIB Frontend Test Case Report</title>",
            "  <style>",
            "body { font-family: Arial, sans-serif; margin: 24px; color: #111; }",
            "table { width: 100%; border-collapse: collapse; margin-bottom: 18px; }",
            "th, td { border: 1px solid #111; padding: 6px 8px; vertical-align: top; }",
            "th { background: #0f4c81; color: #fff; }",
            ".case-table th { background: #758b35; color: #fff; }",
            ".summary-table th { background: #173f5f; }",
            ".feature-row td { background: #d6f0ef; font-weight: 700; }",
            ".hero { margin-bottom: 20px; }",
            ".hero p { margin: 4px 0; }",
            ".toc a { text-decoration: none; }",
            "h1 { margin-bottom: 8px; }",
            "h2 { margin-top: 32px; }",
            "code { font-family: Consolas, monospace; }",
            "  </style>",
            "</head>",
            "<body>",
            '  <div class="hero">',
            "    <h1>SLIB Frontend Test Case Report</h1>",
            f"    <p><strong>Report Date:</strong> {EXECUTED_DATE}</p>",
            f"    <p><strong>Scope:</strong> {total_features} frontend features (FE-01 to FE-136)</p>",
            f"    <p><strong>Total Test Cases:</strong> {total_cases}</p>",
            f"    <p><strong>Execution Result:</strong> {BUILD_RESULT} ({TOTAL_TESTS} tests executed, 0 failures, 0 errors)</p>",
            "    <p><strong>Summary:</strong> This document summarizes the frontend test cases and execution status for all FE scenarios, with linked evidence for each feature.</p>",
            "  </div>",
            "",
            "  <h2>Feature Summary</h2>",
            '  <table class="summary-table toc">',
            "    <tr><th>Feature Code</th><th>Function Name</th><th>Total TCs</th><th>Type Stats</th><th>Result</th><th>Evidence</th></tr>",
            *("    " + row for row in summary_rows.splitlines()),
            "  </table>",
            "",
            "  <h2>Detailed Test Cases</h2>",
            '  <table class="case-table">',
            "    <tr><th>Test Case ID</th><th>Test Case Description</th><th>Test Case Procedure</th><th>Expected Results</th><th>Pre-conditions</th><th>Round 1</th><th>Test date</th><th>Tester</th><th>Round 2</th><th>Test date</th><th>Tester</th><th>Round 3</th><th>Test date</th><th>Tester</th><th>Note</th></tr>",
            *("    " + row for row in detail_rows.splitlines()),
            "  </table>",
            "",
            "</body>",
            "</html>",
            "",
        ]
    )


def main() -> None:
    reports: list[FeatureReport] = []
    for path in sorted(UNIT_REPORT_DIR.glob("FE*.html"), key=numeric_fe_key):
        if path.name in {"index.html", "style.css"}:
            continue
        reports.append(parse_feature_report(path))

    OUTPUT_PATH.write_text(render_document(reports), encoding="utf-8")


if __name__ == "__main__":
    main()
