export const PATRON_ROLES = ['STUDENT', 'TEACHER'];
export const STAFF_ROLES = ['LIBRARIAN', 'ADMIN'];
export const VALID_USER_ROLES = [...PATRON_ROLES, ...STAFF_ROLES];

export const normalizeRole = (role) =>
  role ? role.toString().trim().toUpperCase() : null;

export const isPatronRole = (role) => PATRON_ROLES.includes(normalizeRole(role));

export const isStaffRole = (role) => STAFF_ROLES.includes(normalizeRole(role));
