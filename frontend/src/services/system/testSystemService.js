import { API_BASE_URL } from "../../config/apiConfig";

const BASE = `${API_BASE_URL}/slib`;

const parseResponse = async (response) => {
  if (response.ok) {
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }

  const text = await response.text().catch(() => "");
  try {
    const json = text ? JSON.parse(text) : null;
    throw new Error(json?.error || json?.message || text || `Lỗi máy chủ (${response.status})`);
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new Error(text || `Lỗi máy chủ (${response.status})`);
    }
    throw error;
  }
};

const request = async (path, { method = "GET", token, body } = {}) => {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  return parseResponse(response);
};

const testSystemService = {
  async login(identifier, password) {
    const response = await fetch(`${BASE}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Device-Info": navigator.userAgent,
      },
      body: JSON.stringify({ identifier, password }),
    });

    return parseResponse(response);
  },

  getAdminUsers(token, search = "") {
    const query = search ? `?search=${encodeURIComponent(search)}` : "";
    return request(`/users/admin/list${query}`, { token });
  },

  getBookings(token) {
    return request("/bookings/getall", { token });
  },

  seedAll(token, params) {
    const query = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
      if (value !== "" && value !== null && value !== undefined) {
        query.set(key, value);
      }
    });
    return request(`/seed/all?${query.toString()}`, { method: "POST", token });
  },

  clearSeedData(token) {
    return request("/seed/clear", { method: "DELETE", token });
  },

  clearAllBookings(token) {
    return request("/seed/bookings", { method: "DELETE", token });
  },

  seedStudentJourney(token, userCode) {
    return request(`/seed/student-journey?userCode=${encodeURIComponent(userCode)}`, {
      method: "POST",
      token,
    });
  },

  seedStudentMobileDemo(token, userCode) {
    return request(`/seed/student-mobile-demo?userCode=${encodeURIComponent(userCode)}`, {
      method: "POST",
      token,
    });
  },

  seedReminderTest(token, userCode) {
    return request(`/seed/reminder-test?userCode=${encodeURIComponent(userCode)}`, {
      method: "POST",
      token,
    });
  },

  seedActiveBookingTest(token, userCode) {
    return request(`/seed/active-booking-test?userCode=${encodeURIComponent(userCode)}`, {
      method: "POST",
      token,
    });
  },

  seedViolationTest(token, { userCode, neighbors, sameZone }) {
    const query = new URLSearchParams({
      userCode,
      neighbors: String(neighbors),
      sameZone: String(Boolean(sameZone)),
    });
    return request(`/seed/violation-test?${query.toString()}`, {
      method: "POST",
      token,
    });
  },

  adjustReputation(token, userId, targetScore, reason) {
    return request(`/system/test-tools/users/${userId}/set-reputation`, {
      method: "PATCH",
      token,
      body: { targetScore, reason },
    });
  },

  prepareReminder(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-reminder`, {
      method: "POST",
      token,
    });
  },

  prepareNearReminder(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-near-reminder`, {
      method: "POST",
      token,
    });
  },

  prepareExpiryWarning(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-expiry-warning`, {
      method: "POST",
      token,
    });
  },

  prepareSeatLeave(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-seat-leave`, {
      method: "POST",
      token,
    });
  },

  prepareLateCheckout(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-late-checkout`, {
      method: "POST",
      token,
    });
  },

  prepareSeatStart(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-seat-start`, {
      method: "POST",
      token,
    });
  },

  prepareNoCheckinCancel(token, reservationId) {
    return request(`/system/test-tools/bookings/${reservationId}/prepare-no-checkin-cancel`, {
      method: "POST",
      token,
    });
  },
};

export default testSystemService;
