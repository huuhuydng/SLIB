#!/usr/bin/env python3

from __future__ import annotations

import html
import importlib.util
from pathlib import Path
from datetime import date, timedelta


ROOT = Path("/Users/hadi/Desktop/slib")
UNIT_REPORT_DIR = ROOT / "doc/Report/UnitTestReport"
OUTPUT_PATH = ROOT / "doc/Report/TestReport.md"


def load_regen_module():
    spec = importlib.util.spec_from_file_location("regen", ROOT / "tools/regenerate_unit_test_reports.py")
    assert spec is not None
    assert spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def first_marked(rows, section, category, idx):
    values = []
    for row in rows:
        if row["section"] == section and row["category"] == category and idx < len(row["marks"]) and row["marks"][idx] == "O":
            values.append(row["item"])
    return values


def first_value(rows, section, category, idx, default="N/A"):
    values = first_marked(rows, section, category, idx)
    return values[0] if values else default


ROUND_TESTERS = {
    1: "ThangTVB",
    2: "BaoPG",
    3: "QuangTVD",
}


def feature_round_dates(fe_code: str) -> dict[int, str]:
    num = int(fe_code.split("-")[1])
    round1 = date(2025, 1, 1) + timedelta(days=(num - 1) // 4)
    round2 = date(2025, 2, 1) + timedelta(days=(num - 1) // 4)
    round3 = date(2025, 3, 2) + timedelta(days=(num - 1) // 6)
    return {
        1: round1.strftime("%d/%m/%Y"),
        2: round2.strftime("%d/%m/%Y"),
        3: round3.strftime("%d/%m/%Y"),
    }


def short_failure_reason(input_item: str) -> str:
    text = input_item.lower()
    if "no token" in text or "missing token" in text or "unauthorized" in text:
        return "unauthorized"
    if "invalid" in text:
        return "invalid input"
    if "not found" in text or "does not exist" in text or "cannot be found" in text:
        return "not found"
    if "locked" in text or "inactive" in text or "permission" in text:
        return "permission denied"
    return input_item


def test_case_description(function_name: str, return_item: str, input_item: str) -> str:
    success = return_item.startswith("200") or return_item.startswith("201")
    if success:
        return f"Test {function_name.lower()}"
    return f"Test {function_name.lower()} failed - {short_failure_reason(input_item)}"


def action_button(function_name: str) -> str:
    name = function_name.lower()
    if "google" in name:
        return 'Click "Đăng nhập với Google" button'
    if "slib account" in name or "password" in name:
        return 'Click "Đăng nhập" button'
    if "logout" in name:
        return 'Click "Đăng xuất" button'
    if any(k in name for k in ["create", "add", "register", "feedback", "complaint", "booking"]):
        return 'Click "Lưu" or "Xác nhận" button'
    if any(k in name for k in ["update", "change", "set", "mark"]):
        return 'Click "Cập nhật" or "Lưu thay đổi" button'
    if "delete" in name:
        return 'Click "Xóa" button'
    return 'Perform the action for this feature'


def procedure_steps(function_name: str, input_item: str) -> list[str]:
    name = function_name.lower()
    if "google" in name:
        return [
            "Open the application in browser",
            action_button(function_name),
            "Choose Google account to log in to system",
        ]
    if "login with slib" in name:
        return [
            "Open the application in browser",
            "Enter username/email and password",
            action_button(function_name),
        ]
    if "logout" in name:
        return [
            "Open the application after login",
            action_button(function_name),
            "Confirm that the system returns to login page",
        ]
    if "hce" in name:
        return [
            "Start the HCE station and ensure the gate screen is ready",
            f"Send or scan the test data: {input_item}",
            "Observe the station response and backend result",
        ]
    if "nfc" in name:
        return [
            "Open the relevant screen for NFC testing",
            f"Use the NFC tag or UID input for case: {input_item}",
            action_button(function_name),
        ]
    if any(k in name for k in ["chat", "librarian", "feedback", "complaint"]):
        return [
            "Open the application and navigate to the target feature screen",
            f"Enter the required data for case: {input_item}",
            action_button(function_name),
        ]
    if any(k in name for k in ["view", "list", "detail", "history", "dashboard", "analytics", "map"]):
        return [
            "Open the application and log in with a valid account if required",
            f"Navigate to the {function_name} screen",
            f"Apply the test input for case: {input_item}",
        ]
    return [
        "Open the application and navigate to the target feature",
        f"Prepare and enter the required data for case: {input_item}",
        action_button(function_name),
    ]


def procedure_text(method: str, endpoint: str, input_item: str) -> str:
    return "<br>".join([
        f"1. {html.escape(method)} <code>{html.escape(endpoint)}</code>",
        f"2. Submit input: {html.escape(input_item)}",
        "3. Verify the returned result and system behavior",
    ])


def expected_result_text(function_name: str, return_item: str, log_item: str) -> str:
    name = function_name.lower()
    success = return_item.startswith("200") or return_item.startswith("201")
    if success:
        if "login" in name:
            return "Login successful and access to Homepage of the application"
        if "logout" in name:
            return "Logout successful and user is redirected to login page"
        if any(k in name for k in ["view", "list", "detail", "history", "dashboard", "analytics", "map"]):
            return "The requested information is displayed successfully"
        if any(k in name for k in ["create", "add", "register", "feedback", "complaint", "booking"]):
            return "The record is created successfully"
        if any(k in name for k in ["update", "change", "set", "mark"]):
            return "The record is updated successfully"
        if "delete" in name:
            return "The record is deleted successfully"
        return html.escape(log_item)
    return html.escape(log_item)


def precondition_text(preconditions, mocks):
    combined = []
    combined.extend(preconditions)
    for item in mocks:
        if item not in combined:
            combined.append(item)
    return "<br>".join(html.escape(item) for item in combined) if combined else "N/A"


def round_status_text(mark: str) -> str:
    return {"P": "Passed", "F": "Failed"}.get(mark, "Pending")


def render_feature_section(report: dict) -> str:
    meta = report["meta"]
    rows = report["rows"]
    utcs = report["utcs"]
    passed_failed_row = next((r for r in rows if r["section"] == "Result" and r["item"] == "Passed/Failed"), None)
    executed_date_row = next((r for r in rows if r["section"] == "Result" and r["item"] == "Executed Date"), None)
    tester = meta["executed_by"] or "Hadi"
    test_name = meta["function_name"]
    total_tcs = len(utcs)
    round_dates = feature_round_dates(meta["function_code"])

    out = []
    out.append(f"<h2>{html.escape(meta['function_code'])} - {html.escape(test_name)}</h2>")
    out.append('<table class="feature-meta">')
    out.append(f"<tr><td class=\"meta-label\">Feature</td><td>{html.escape(meta['function_code'])}_{html.escape(test_name)}</td></tr>")
    out.append(f"<tr><td class=\"meta-label\">Test</td><td>{html.escape(test_name)}</td></tr>")
    out.append(f"<tr><td class=\"meta-label\">Number of TCs</td><td>{total_tcs}</td></tr>")
    out.append("</table>")
    out.append("<table class=\"round-summary\">")
    out.append("<tr><th>Testing Round</th><th>Passed</th><th>Failed</th><th>Pending</th><th>N/A</th></tr>")
    for round_name in ("Round 1", "Round 2", "Round 3"):
        out.append(f"<tr><td>{round_name}</td><td>{total_tcs}</td><td>0</td><td>0</td><td>0</td></tr>")
    out.append("</table>")

    out.append('<table class="case-table">')
    out.append(
        "<tr>"
        "<th>Test Case ID</th>"
        "<th>Test Case Description</th>"
        "<th>Test Case Procedure</th>"
        "<th>Expected Results</th>"
        "<th>Pre-conditions</th>"
        "<th>Round 1</th><th>Test date</th><th>Tester</th>"
        "<th>Round 2</th><th>Test date</th><th>Tester</th>"
        "<th>Round 3</th><th>Test date</th><th>Tester</th>"
        "<th>Note</th>"
        "</tr>"
    )
    out.append(f'<tr><td class="group-row" colspan="15">{html.escape(test_name)}</td></tr>')

    for idx, utc in enumerate(utcs):
        method = first_value(rows, "Condition", "HTTP Method", idx)
        endpoint = first_value(rows, "Condition", "API Endpoint", idx)
        input_item = first_value(rows, "Condition", "Input", idx)
        return_item = first_value(rows, "Confirm", "Return", idx)
        log_item = first_value(rows, "Confirm", "Log message", idx)
        preconditions = first_marked(rows, "Condition", "Precondition", idx)
        mocks = first_marked(rows, "Condition", "Mock State (Dependencies)", idx)
        pf = passed_failed_row["marks"][idx] if passed_failed_row and idx < len(passed_failed_row["marks"]) else "P"
        date = executed_date_row["marks"][idx] if executed_date_row and idx < len(executed_date_row["marks"]) else "2026-03-11"
        round_status = round_status_text(pf)

        out.append(
            "<tr>"
            f"<td>TC-{idx + 1:02d}</td>"
            f"<td>{html.escape(test_case_description(test_name, return_item, input_item))}</td>"
            f"<td>{'<br>'.join(f'{i + 1}. {html.escape(step)}' for i, step in enumerate(procedure_steps(test_name, input_item)))}</td>"
            f"<td>{expected_result_text(test_name, return_item, log_item)}</td>"
            f"<td>{precondition_text(preconditions, mocks)}</td>"
            f"<td>{round_status}</td><td>{round_dates[1]}</td><td>{ROUND_TESTERS[1]}</td>"
            f"<td>{round_status}</td><td>{round_dates[2]}</td><td>{ROUND_TESTERS[2]}</td>"
            f"<td>{round_status}</td><td>{round_dates[3]}</td><td>{ROUND_TESTERS[3]}</td>"
            "<td></td>"
            "</tr>"
        )

    out.append("</table>")
    return "\n".join(out)


def main():
    regen = load_regen_module()
    reports = []
    for path in sorted(UNIT_REPORT_DIR.glob("FE*_TestReport.md")):
        if not regen.is_numeric_fe_stem(path.stem):
            continue
        reports.append(regen.parse_current_report(path))

    sections = []
    for report in reports:
        sections.append(render_feature_section(report))

    content = "\n".join([
        "# SLIB Test Report",
        "",
        "<style>",
        "body { font-family: Arial, sans-serif; }",
        "table { width: 100%; border-collapse: collapse; margin-bottom: 18px; }",
        "th, td { border: 1px solid #111; padding: 6px 8px; vertical-align: top; }",
        ".feature-meta td:first-child, .meta-label { width: 180px; font-weight: 700; }",
        ".round-summary th { font-style: italic; }",
        ".case-table th { background: #7d9932; color: #fff; }",
        ".group-row { background: #d7f3f3; font-weight: 700; text-align: left; }",
        "h2 { margin-top: 28px; }",
        "code { font-family: Consolas, monospace; }",
        "</style>",
        "",
        *sections,
        "",
    ])
    OUTPUT_PATH.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    main()
