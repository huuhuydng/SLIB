import React, { useEffect, useMemo, useState } from "react";
import {
  BellRing,
  CalendarClock,
  Database,
  Hourglass,
  Loader2,
  LogOut,
  PlayCircle,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  Sparkles,
  Target,
  Trash2,
  TriangleAlert,
  UserRoundSearch,
  Wand2,
} from "lucide-react";

import { useToast } from "../../components/common/ToastProvider";
import testSystemService from "../../services/system/testSystemService";
import "../../styles/librarian/librarian-shared.css";
import "./TestSystemPage.css";

const STORAGE_TOKEN_KEY = "test_system_token";
const STORAGE_USER_KEY = "test_system_user";

const DEFAULT_SEED_FORM = {
  bookings: 18,
  accessLogs: 28,
  violations: 6,
  supports: 6,
  complaints: 5,
  feedbacks: 6,
  seatStatusReports: 6,
  news: 6,
  newBooks: 8,
  notifications: 10,
  studentCode: "",
};

const DEMO_READY_SEED_FORM = {
  bookings: 18,
  accessLogs: 28,
  violations: 6,
  supports: 6,
  complaints: 5,
  feedbacks: 6,
  seatStatusReports: 6,
  news: 6,
  newBooks: 8,
  notifications: 10,
  studentCode: "",
};

const DEFAULT_VIOLATION_FORM = {
  userCode: "",
  neighbors: 4,
  sameZone: true,
};

const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN");
};

function TestSystemPage() {
  const toast = useToast();

  const [token, setToken] = useState(() => sessionStorage.getItem(STORAGE_TOKEN_KEY) || "");
  const [currentUser, setCurrentUser] = useState(() => {
    try {
      const raw = sessionStorage.getItem(STORAGE_USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });

  const [authForm, setAuthForm] = useState({ identifier: "", password: "" });
  const [authLoading, setAuthLoading] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [actionKey, setActionKey] = useState("");
  const [lastActionResult, setLastActionResult] = useState(null);

  const [seedForm, setSeedForm] = useState(DEFAULT_SEED_FORM);
  const [journeyUserCode, setJourneyUserCode] = useState("");
  const [reminderUserCode, setReminderUserCode] = useState("");
  const [violationForm, setViolationForm] = useState(DEFAULT_VIOLATION_FORM);

  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [userSearch, setUserSearch] = useState("");
  const [bookingSearch, setBookingSearch] = useState("");

  const [selectedUserId, setSelectedUserId] = useState("");
  const [reputationTarget, setReputationTarget] = useState("");
  const [reputationReason, setReputationReason] = useState("");

  const handleLogout = React.useCallback(() => {
    sessionStorage.removeItem(STORAGE_TOKEN_KEY);
    sessionStorage.removeItem(STORAGE_USER_KEY);
    setToken("");
    setCurrentUser(null);
    setUsers([]);
    setBookings([]);
    setSelectedUserId("");
    setReputationTarget("");
    setReputationReason("");
  }, []);

  const selectedUser = useMemo(
    () => users.find((item) => item.id === selectedUserId) || null,
    [users, selectedUserId]
  );

  const filteredUsers = useMemo(() => {
    const query = userSearch.trim().toLowerCase();
    if (!query) return users.slice(0, 10);
    return users
      .filter((user) =>
        [user.fullName, user.email, user.userCode]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(query))
      )
      .slice(0, 10);
  }, [users, userSearch]);

  const filteredBookings = useMemo(() => {
    const query = bookingSearch.trim().toLowerCase();
    const visibleStatuses = new Set(["BOOKED", "CONFIRMED", "PROCESSING", "COMPLETED", "EXPIRED", "CANCEL", "CANCELLED"]);
    const base = bookings.filter((booking) => visibleStatuses.has(booking.status));

    const sorted = [...base].sort((a, b) => {
      const left = new Date(b.startTime || b.createdAt || 0).getTime();
      const right = new Date(a.startTime || a.createdAt || 0).getTime();
      return left - right;
    });

    if (!query) return sorted.slice(0, 14);
    return sorted
      .filter((booking) =>
        [
          booking.user?.fullName,
          booking.user?.email,
          booking.user?.userCode,
          booking.seat?.seatCode,
          booking.seat?.zone?.zoneName,
          booking.status,
        ]
          .filter(Boolean)
          .some((value) => value.toLowerCase().includes(query))
      )
      .slice(0, 14);
  }, [bookings, bookingSearch]);

  const refreshData = React.useCallback(async (authToken = token) => {
    if (!authToken) return;
    setDataLoading(true);
    try {
      const [userList, bookingList] = await Promise.all([
        testSystemService.getAdminUsers(authToken),
        testSystemService.getBookings(authToken),
      ]);
      setUsers(Array.isArray(userList) ? userList : []);
      setBookings(Array.isArray(bookingList) ? bookingList : []);
    } catch (error) {
      toast.error(error.message || "Không thể tải dữ liệu test.");
      if (/401|403|hết hạn|không hợp lệ/i.test(error.message || "")) {
        handleLogout();
      }
    } finally {
      setDataLoading(false);
    }
  }, [handleLogout, token, toast]);

  useEffect(() => {
    document.title = "SLIB - Test System";
  }, []);

  useEffect(() => {
    if (token && currentUser?.role === "ADMIN") {
      refreshData(token);
    }
  }, [token, currentUser?.role, refreshData]);

  const handleLogin = async (event) => {
    event.preventDefault();
    setAuthLoading(true);
    try {
      const response = await testSystemService.login(authForm.identifier.trim(), authForm.password);
      if (response.role !== "ADMIN") {
        throw new Error("Chỉ tài khoản ADMIN mới được dùng Test System.");
      }

      sessionStorage.setItem(STORAGE_TOKEN_KEY, response.accessToken);
      sessionStorage.setItem(
        STORAGE_USER_KEY,
        JSON.stringify({
          id: response.id,
          fullName: response.fullName,
          email: response.email,
          role: response.role,
        })
      );

      setToken(response.accessToken);
      setCurrentUser({
        id: response.id,
        fullName: response.fullName,
        email: response.email,
        role: response.role,
      });
      setAuthForm((prev) => ({ ...prev, password: "" }));
      toast.success("Đăng nhập Test System thành công.");
    } catch (error) {
      toast.error(error.message || "Đăng nhập thất bại.");
    } finally {
      setAuthLoading(false);
    }
  };

  const runAction = async (key, executor, successMessage) => {
    setActionKey(key);
    try {
      const result = await executor();
      setLastActionResult({ key, result, executedAt: new Date().toISOString() });
      if (successMessage || result?.message) {
        toast.success(successMessage || result.message);
      }
      await refreshData();
      return result;
    } catch (error) {
      toast.error(error.message || "Thao tác thất bại.");
    } finally {
      setActionKey("");
    }
  };

  const handleAdjustReputation = async () => {
    if (!selectedUser) {
      toast.error("Vui lòng chọn người dùng.");
      return;
    }

    const targetScore = Number.parseInt(reputationTarget, 10);
    if (Number.isNaN(targetScore) || targetScore < 0 || targetScore > 100) {
      toast.error("Điểm mục tiêu phải nằm trong khoảng 0-100.");
      return;
    }

    const reason = reputationReason.trim();
    if (!reason) {
      toast.error("Vui lòng nhập lý do điều chỉnh.");
      return;
    }

    const currentScore = selectedUser.reputationScore ?? 100;
    const delta = targetScore - currentScore;
    if (delta === 0) {
      toast.error("Điểm mục tiêu đang trùng với điểm hiện tại.");
      return;
    }

    await runAction(
      "adjust-reputation",
      () => testSystemService.adjustReputation(token, selectedUser.id, delta, reason),
      `Đã điều chỉnh điểm uy tín của ${selectedUser.fullName}.`
    );
  };

  if (!token || currentUser?.role !== "ADMIN") {
    return (
      <div className="test-system-page">
        <div className="test-system-login">
          <div className="test-system-login__hero">
            <span className="test-system-chip">SLIB Demo Control</span>
            <h1>Test System cho buổi bảo vệ</h1>
            <p>
              Đăng nhập bằng tài khoản admin để tạo seed, ép trạng thái booking về đúng kịch bản
              demo và thao tác nhanh các tình huống khó test bằng thời gian thực.
            </p>
          </div>

          <form className="test-system-card test-system-login__form" onSubmit={handleLogin}>
            <div className="test-system-card__header">
              <ShieldCheck size={22} />
              <div>
                <h2>Đăng nhập admin</h2>
                <span>Trang này dùng token riêng, không chạm vào phiên đăng nhập portal chính.</span>
              </div>
            </div>

            <label className="test-system-field">
              <span>Tài khoản</span>
              <input
                value={authForm.identifier}
                onChange={(event) => setAuthForm((prev) => ({ ...prev, identifier: event.target.value }))}
                placeholder="Email, username hoặc mã người dùng"
                autoComplete="username"
              />
            </label>

            <label className="test-system-field">
              <span>Mật khẩu</span>
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm((prev) => ({ ...prev, password: event.target.value }))}
                placeholder="Nhập mật khẩu admin"
                autoComplete="current-password"
              />
            </label>

            <button className="test-system-btn test-system-btn--primary" type="submit" disabled={authLoading}>
              {authLoading ? <Loader2 size={16} className="ts-spin" /> : <ShieldCheck size={16} />}
              Vào Test System
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="test-system-page">
      <div className="test-system-shell">
        <header className="test-system-topbar">
          <div>
            <span className="test-system-chip">/test_system</span>
            <h1>SLIB Test System</h1>
            <p>Bảng điều khiển demo một chạm cho seed, notification và các kịch bản bảo vệ.</p>
          </div>

          <div className="test-system-topbar__actions">
            <div className="test-system-user">
              <strong>{currentUser.fullName || currentUser.email}</strong>
              <span>{currentUser.email}</span>
            </div>
            <button
              className="test-system-btn"
              type="button"
              onClick={() => refreshData()}
              disabled={dataLoading}
            >
              {dataLoading ? <Loader2 size={16} className="ts-spin" /> : <RefreshCw size={16} />}
              Làm mới dữ liệu
            </button>
            <button className="test-system-btn test-system-btn--danger" type="button" onClick={handleLogout}>
              <LogOut size={16} />
              Đăng xuất
            </button>
          </div>
        </header>

        <section className="test-system-grid test-system-grid--stats">
          <article className="test-system-stat">
            <Database size={18} />
            <div>
              <strong>{users.length}</strong>
              <span>Người dùng admin nhìn thấy</span>
            </div>
          </article>
          <article className="test-system-stat">
            <CalendarClock size={18} />
            <div>
              <strong>{bookings.length}</strong>
              <span>Lịch đặt hiện có trong hệ thống</span>
            </div>
          </article>
          <article className="test-system-stat">
            <BellRing size={18} />
            <div>
              <strong>4 mốc booking</strong>
              <span>Đến giờ ngồi, nhắc lịch, cảnh báo sắp hết giờ, hủy do chưa xác nhận</span>
            </div>
          </article>
        </section>

        <section className="test-system-grid">
          <article className="test-system-card">
            <div className="test-system-card__header">
              <Sparkles size={20} />
              <div>
                <h2>Dữ liệu mẫu toàn hệ thống</h2>
                <span>Tạo bộ dữ liệu đủ dày và cân đối để các màn hình nhìn đầy đặn, hợp lý khi bảo vệ.</span>
              </div>
            </div>

            <div className="test-system-form-grid">
              <label className="test-system-field">
                <span>Số booking</span>
                <input
                  type="number"
                  min="1"
                  value={seedForm.bookings}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, bookings: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Access logs</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.accessLogs}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, accessLogs: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Số vi phạm</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.violations}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, violations: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Số hỗ trợ</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.supports}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, supports: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Số khiếu nại</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.complaints}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, complaints: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Số phản hồi</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.feedbacks}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, feedbacks: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Báo cáo tình trạng ghế</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.seatStatusReports}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, seatStatusReports: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Tin tức</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.news}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, news: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Sách mới</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.newBooks}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, newBooks: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>Thông báo mẫu</span>
                <input
                  type="number"
                  min="0"
                  value={seedForm.notifications}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, notifications: event.target.value }))}
                />
              </label>
              <label className="test-system-field">
                <span>UserCode ưu tiên</span>
                <input
                  value={seedForm.studentCode}
                  onChange={(event) => setSeedForm((prev) => ({ ...prev, studentCode: event.target.value }))}
                  placeholder="Ví dụ: DE180295"
                />
              </label>
            </div>

            <div className="test-system-note test-system-note--soft">
              Gợi ý đẹp cho buổi demo: giữ khoảng 15-20 booking, 20-30 access logs và 4-8 bản ghi cho mỗi nhóm xử lý để dashboard và các trang danh sách vừa đủ dày nhưng không rối.
            </div>

            <div className="test-system-actions">
              <button
                className="test-system-btn"
                type="button"
                onClick={() => {
                  setSeedForm((prev) => ({ ...DEMO_READY_SEED_FORM, studentCode: prev.studentCode }));
                  toast.success("Đã áp preset Demo Ready.");
                }}
              >
                <Sparkles size={16} />
                Điền preset Demo Ready
              </button>
              <button
                className="test-system-btn test-system-btn--primary"
                type="button"
                disabled={actionKey === "seed-all"}
                onClick={() =>
                  runAction("seed-all", () => testSystemService.seedAll(token, seedForm))
                }
              >
                {actionKey === "seed-all" ? <Loader2 size={16} className="ts-spin" /> : <Wand2 size={16} />}
                Tạo seed tổng
              </button>
              <button
                className="test-system-btn"
                type="button"
                disabled={actionKey === "clear-seed"}
                onClick={() => runAction("clear-seed", () => testSystemService.clearSeedData(token))}
              >
                {actionKey === "clear-seed" ? <Loader2 size={16} className="ts-spin" /> : <Trash2 size={16} />}
                Dọn dữ liệu seed
              </button>
              <button
                className="test-system-btn test-system-btn--danger"
                type="button"
                disabled={actionKey === "clear-bookings"}
                onClick={() => runAction("clear-bookings", () => testSystemService.clearAllBookings(token))}
              >
                {actionKey === "clear-bookings" ? <Loader2 size={16} className="ts-spin" /> : <TriangleAlert size={16} />}
                Xóa toàn bộ booking
              </button>
            </div>
          </article>

          <article className="test-system-card">
            <div className="test-system-card__header">
              <Target size={20} />
              <div>
                <h2>Kịch bản nhanh theo userCode</h2>
                <span>Tạo đúng case cần demo mà không phải đợi thời gian thực.</span>
              </div>
            </div>

            <div className="test-system-stack">
              <div className="test-system-inline">
                <label className="test-system-field">
                  <span>Demo mobile đầy đủ</span>
                  <input
                    value={journeyUserCode}
                    onChange={(event) => {
                      const value = event.target.value;
                      setJourneyUserCode(value);
                      setSeedForm((prev) => ({ ...prev, studentCode: value }));
                    }}
                    placeholder="Ví dụ: DE180295"
                  />
                </label>
              <button
                className="test-system-btn test-system-btn--primary"
                type="button"
                disabled={actionKey === "student-mobile-demo"}
                onClick={() => {
                  if (!journeyUserCode.trim()) {
                    toast.error("Vui lòng nhập userCode để tạo preset mobile cho sinh viên.");
                    return;
                  }
                  runAction(
                    "student-mobile-demo",
                    () => testSystemService.seedStudentMobileDemo(token, journeyUserCode.trim())
                  );
                }}
              >
                  {actionKey === "student-mobile-demo" ? (
                    <Loader2 size={16} className="ts-spin" />
                  ) : (
                    <Smartphone size={16} />
                  )}
                  Tạo preset mobile
                </button>
              </div>

              <div className="test-system-note test-system-note--soft">
                Preset này tạo sẵn cho một sinh viên: booking đang dùng, booking sắp tới, booking đã hoàn thành,
                booking đã hết hạn, booking đã bị thủ thư hủy kèm lý do, lịch sử hoạt động, điểm uy tín, vi phạm
                bị ghi nhận, vi phạm đã báo cáo, khiếu nại, phản hồi, hỗ trợ, báo cáo ghế và thông báo mobile.
              </div>

              <div className="test-system-inline">
                <label className="test-system-field">
                  <span>Reminder test</span>
                  <input
                    value={reminderUserCode}
                    onChange={(event) => setReminderUserCode(event.target.value)}
                    placeholder="Ví dụ: DE180295"
                  />
                </label>
              <button
                className="test-system-btn"
                type="button"
                disabled={actionKey === "reminder-seed"}
                onClick={() => {
                  if (!reminderUserCode.trim()) {
                    toast.error("Vui lòng nhập userCode để tạo booking nhắc lịch.");
                    return;
                  }
                  runAction(
                    "reminder-seed",
                    () => testSystemService.seedReminderTest(token, reminderUserCode.trim())
                  );
                }}
              >
                  {actionKey === "reminder-seed" ? <Loader2 size={16} className="ts-spin" /> : <BellRing size={16} />}
                  Tạo booking nhắc lịch
                </button>
              </div>

              <div className="test-system-inline test-system-inline--triple">
                <label className="test-system-field">
                  <span>Violation test - userCode</span>
                  <input
                    value={violationForm.userCode}
                    onChange={(event) => setViolationForm((prev) => ({ ...prev, userCode: event.target.value }))}
                    placeholder="Ví dụ: DE180295"
                  />
                </label>
                <label className="test-system-field">
                  <span>Số người xung quanh</span>
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={violationForm.neighbors}
                    onChange={(event) => setViolationForm((prev) => ({ ...prev, neighbors: event.target.value }))}
                  />
                </label>
                <label className="test-system-field test-system-field--checkbox">
                  <span>Cùng khu vực</span>
                  <input
                    type="checkbox"
                    checked={violationForm.sameZone}
                    onChange={(event) => setViolationForm((prev) => ({ ...prev, sameZone: event.target.checked }))}
                  />
                </label>
                <button
                  className="test-system-btn"
                  type="button"
                  disabled={actionKey === "violation-seed"}
                  onClick={() => {
                    if (!violationForm.userCode.trim()) {
                      toast.error("Vui lòng nhập userCode cho kịch bản vi phạm.");
                      return;
                    }
                    runAction(
                      "violation-seed",
                      () => testSystemService.seedViolationTest(token, violationForm)
                    );
                  }}
                >
                  {actionKey === "violation-seed" ? <Loader2 size={16} className="ts-spin" /> : <TriangleAlert size={16} />}
                  Tạo case vi phạm
                </button>
              </div>
            </div>
          </article>
        </section>

        {lastActionResult?.result && (
          <section className="test-system-grid">
            <article className="test-system-card">
              <div className="test-system-card__header">
                <Sparkles size={20} />
                <div>
                  <h2>Kết quả thao tác gần nhất</h2>
                  <span>{lastActionResult.result.message || "Đã chạy xong thao tác gần nhất."}</span>
                </div>
              </div>

              <div className="test-system-result-grid">
                {lastActionResult.result.userCode && (
                  <div className="test-system-result-pill">
                    <strong>UserCode</strong>
                    <span>{lastActionResult.result.userCode}</span>
                  </div>
                )}
                {lastActionResult.result.studentName && (
                  <div className="test-system-result-pill">
                    <strong>Sinh viên</strong>
                    <span>{lastActionResult.result.studentName}</span>
                  </div>
                )}
                {lastActionResult.result.reputationScore !== undefined && (
                  <div className="test-system-result-pill">
                    <strong>Điểm uy tín</strong>
                    <span>{lastActionResult.result.reputationScore}</span>
                  </div>
                )}
                {lastActionResult.result.currentSeat && (
                  <div className="test-system-result-pill">
                    <strong>Ghế đang dùng</strong>
                    <span>{lastActionResult.result.currentSeat}</span>
                  </div>
                )}
                {lastActionResult.result.upcomingSeat && (
                  <div className="test-system-result-pill">
                    <strong>Ghế sắp tới</strong>
                    <span>{lastActionResult.result.upcomingSeat}</span>
                  </div>
                )}
                {lastActionResult.result.cancelledSeat && (
                  <div className="test-system-result-pill">
                    <strong>Ghế đã hủy</strong>
                    <span>{lastActionResult.result.cancelledSeat}</span>
                  </div>
                )}
                {lastActionResult.result.notificationCount !== undefined && (
                  <div className="test-system-result-pill">
                    <strong>Thông báo tạo ra</strong>
                    <span>{lastActionResult.result.notificationCount}</span>
                  </div>
                )}
              </div>

              {Array.isArray(lastActionResult.result.coverage) && lastActionResult.result.coverage.length > 0 && (
                <div className="test-system-result-list">
                  {lastActionResult.result.coverage.map((item) => (
                    <span key={item}>{item}</span>
                  ))}
                </div>
              )}
            </article>
          </section>
        )}

        <section className="test-system-grid">
          <article className="test-system-card">
            <div className="test-system-card__header">
              <BellRing size={20} />
              <div>
                <h2>Booking demo tức thì</h2>
                <span>Chọn lịch đặt hiện có và ép về mốc thông báo để chạy ngay trong buổi bảo vệ.</span>
              </div>
            </div>

            <label className="test-system-field">
              <span>Tìm booking</span>
              <input
                value={bookingSearch}
                onChange={(event) => setBookingSearch(event.target.value)}
                placeholder="Tên, email, mã người dùng, ghế, khu vực..."
              />
            </label>

            <div className="test-system-table">
              {filteredBookings.map((booking) => (
                <div key={booking.reservationId} className="test-system-row">
                  <div className="test-system-row__main">
                    <strong>{booking.user?.fullName || "Không rõ người dùng"}</strong>
                    <span>
                      {booking.user?.userCode || "N/A"} • {booking.seat?.seatCode || "N/A"} •{" "}
                      {booking.seat?.zone?.zoneName || "N/A"}
                    </span>
                    <span>
                      {booking.status} • {formatDateTime(booking.startTime)} → {formatDateTime(booking.endTime)}
                    </span>
                  </div>
                  <div className="test-system-row__actions">
                    <button
                      className="test-system-btn"
                      type="button"
                      disabled={actionKey === `start-${booking.reservationId}`}
                      onClick={() =>
                        runAction(
                          `start-${booking.reservationId}`,
                          () => testSystemService.prepareSeatStart(token, booking.reservationId)
                        )
                      }
                    >
                      {actionKey === `start-${booking.reservationId}` ? (
                        <Loader2 size={16} className="ts-spin" />
                      ) : (
                        <PlayCircle size={16} />
                      )}
                      Đã đến giờ ngồi
                    </button>
                    <button
                      className="test-system-btn"
                      type="button"
                      disabled={actionKey === `reminder-${booking.reservationId}`}
                      onClick={() =>
                        runAction(
                          `reminder-${booking.reservationId}`,
                          () => testSystemService.prepareReminder(token, booking.reservationId)
                        )
                      }
                    >
                      {actionKey === `reminder-${booking.reservationId}` ? (
                        <Loader2 size={16} className="ts-spin" />
                      ) : (
                        <BellRing size={16} />
                      )}
                      Nhắc lịch sau 15 phút
                    </button>
                    <button
                      className="test-system-btn"
                      type="button"
                      disabled={actionKey === `expiry-${booking.reservationId}`}
                      onClick={() =>
                        runAction(
                          `expiry-${booking.reservationId}`,
                          () => testSystemService.prepareExpiryWarning(token, booking.reservationId)
                        )
                      }
                    >
                      {actionKey === `expiry-${booking.reservationId}` ? (
                        <Loader2 size={16} className="ts-spin" />
                      ) : (
                        <CalendarClock size={16} />
                      )}
                      Cảnh báo sắp hết giờ
                    </button>
                    <button
                      className="test-system-btn test-system-btn--danger"
                      type="button"
                      disabled={actionKey === `cancel-${booking.reservationId}`}
                      onClick={() =>
                        runAction(
                          `cancel-${booking.reservationId}`,
                          () => testSystemService.prepareNoCheckinCancel(token, booking.reservationId)
                        )
                      }
                    >
                      {actionKey === `cancel-${booking.reservationId}` ? (
                        <Loader2 size={16} className="ts-spin" />
                      ) : (
                        <Hourglass size={16} />
                      )}
                      Hủy do chưa xác nhận
                    </button>
                  </div>
                </div>
              ))}

              {!filteredBookings.length && (
                <div className="test-system-empty">Không có booking phù hợp để demo.</div>
              )}
            </div>
          </article>

          <article className="test-system-card">
            <div className="test-system-card__header">
              <UserRoundSearch size={20} />
              <div>
                <h2>Điểm uy tín và hạn chế đặt chỗ</h2>
                <span>Đưa người dùng về đúng mức điểm để test rule hạn chế hoặc mở quyền đặt chỗ.</span>
              </div>
            </div>

            <label className="test-system-field">
              <span>Tìm người dùng</span>
              <input
                value={userSearch}
                onChange={(event) => setUserSearch(event.target.value)}
                placeholder="Tên, email hoặc mã người dùng"
              />
            </label>

            <div className="test-system-picker">
              {filteredUsers.map((user) => (
                <button
                  key={user.id}
                  type="button"
                  className={`test-system-picker__item ${selectedUserId === user.id ? "active" : ""}`}
                  onClick={() => {
                    setSelectedUserId(user.id);
                    setReputationTarget(String(user.reputationScore ?? 100));
                  }}
                >
                  <strong>{user.fullName}</strong>
                  <span>
                    {user.userCode || "N/A"} • {user.role} • {user.reputationScore ?? 100} điểm
                  </span>
                </button>
              ))}
            </div>

            <div className="test-system-form-grid">
              <label className="test-system-field">
                <span>Điểm mục tiêu</span>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={reputationTarget}
                  onChange={(event) => setReputationTarget(event.target.value)}
                  placeholder="0 - 100"
                />
              </label>
              <label className="test-system-field test-system-field--wide">
                <span>Lý do điều chỉnh</span>
                <input
                  value={reputationReason}
                  onChange={(event) => setReputationReason(event.target.value)}
                  placeholder="Ví dụ: Hạ xuống 20 điểm để demo hạn chế đặt chỗ"
                />
              </label>
            </div>

            {selectedUser && (
              <div className="test-system-note">
                Điểm hiện tại của <strong>{selectedUser.fullName}</strong>:{" "}
                <strong>{selectedUser.reputationScore ?? 100}</strong>
              </div>
            )}

            <div className="test-system-actions">
              <button
                className="test-system-btn test-system-btn--primary"
                type="button"
                disabled={actionKey === "adjust-reputation"}
                onClick={handleAdjustReputation}
              >
                {actionKey === "adjust-reputation" ? <Loader2 size={16} className="ts-spin" /> : <Target size={16} />}
                Áp dụng điểm uy tín
              </button>
            </div>
          </article>
        </section>
      </div>
    </div>
  );
}

export default TestSystemPage;
