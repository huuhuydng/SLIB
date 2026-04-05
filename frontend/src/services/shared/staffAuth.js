export const getStaffAuthToken = () =>
  sessionStorage.getItem("admin_token") ||
  localStorage.getItem("admin_token") ||
  sessionStorage.getItem("librarian_token") ||
  localStorage.getItem("librarian_token");

export const getStaffAuthHeaders = () => {
  const token = getStaffAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};
