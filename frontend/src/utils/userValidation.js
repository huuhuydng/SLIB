const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USER_CODE_REGEX = /^[A-Z0-9._-]{1,20}$/;
const PHONE_REGEX = /^\+?[0-9]{8,20}$/;

export const normalizeFullName = (value) => {
  if (value == null) return '';
  return String(value).trim().replace(/\s+/g, ' ');
};

export const normalizeEmail = (value) => {
  if (value == null) return '';
  return String(value).trim().toLowerCase();
};

export const normalizeUserCode = (value) => {
  if (value == null) return '';
  return String(value).trim().toUpperCase();
};

export const normalizePhone = (value) => {
  if (value == null) return '';
  return String(value).trim().replace(/[\s().-]/g, '');
};

export const validateFullName = (value, required = true) => {
  const normalized = normalizeFullName(value);
  if (!normalized) {
    return required ? 'Họ và tên không được để trống' : null;
  }
  if (normalized.length > 255) {
    return 'Họ và tên không được vượt quá 255 ký tự';
  }
  return null;
};

export const validateEmail = (value, required = true) => {
  const normalized = normalizeEmail(value);
  if (!normalized) {
    return required ? 'Email không được để trống' : null;
  }
  if (normalized.length > 255) {
    return 'Email không được vượt quá 255 ký tự';
  }
  if (!EMAIL_REGEX.test(normalized)) {
    return 'Email không đúng định dạng';
  }
  return null;
};

export const validateUserCode = (value, required = true) => {
  const normalized = normalizeUserCode(value);
  if (!normalized) {
    return required ? 'Mã người dùng không được để trống' : null;
  }
  if (!USER_CODE_REGEX.test(normalized)) {
    return 'Mã người dùng chỉ được chứa chữ cái, số, dấu chấm, gạch ngang hoặc gạch dưới và tối đa 20 ký tự';
  }
  return null;
};

export const validatePhone = (value, required = false) => {
  const normalized = normalizePhone(value);
  if (!normalized) {
    return required ? 'Số điện thoại không được để trống' : null;
  }
  if (!PHONE_REGEX.test(normalized)) {
    return 'Số điện thoại phải có từ 8 đến 20 ký tự số và chỉ được chứa dấu + ở đầu';
  }
  return null;
};

export const validateDob = (value, required = false) => {
  const raw = value == null ? '' : String(value).trim();
  if (!raw) {
    return required ? 'Ngày sinh không được để trống' : null;
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    return 'Ngày sinh phải đúng định dạng yyyy-MM-dd';
  }

  const date = new Date(`${raw}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return 'Ngày sinh không hợp lệ';
  }

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (date > today) {
    return 'Ngày sinh không được ở tương lai';
  }

  if (date.getFullYear() < 1900) {
    return 'Ngày sinh không hợp lệ';
  }

  return null;
};

export const normalizeUserPayload = (payload = {}) => ({
  ...payload,
  fullName: normalizeFullName(payload.fullName),
  email: normalizeEmail(payload.email),
  userCode: normalizeUserCode(payload.userCode),
  phone: normalizePhone(payload.phone) || null,
});

export const validateUserPayload = (payload = {}, options = {}) => {
  const {
    requireFullName = true,
    requireEmail = true,
    requireUserCode = true,
    requirePhone = false,
    requireDob = false,
  } = options;

  const normalized = normalizeUserPayload(payload);
  const errors = {};

  const fullNameError = validateFullName(normalized.fullName, requireFullName);
  if (fullNameError) errors.fullName = fullNameError;

  const emailError = validateEmail(normalized.email, requireEmail);
  if (emailError) errors.email = emailError;

  const userCodeError = validateUserCode(normalized.userCode, requireUserCode);
  if (userCodeError) errors.userCode = userCodeError;

  const phoneError = validatePhone(normalized.phone, requirePhone);
  if (phoneError) errors.phone = phoneError;

  const dobError = validateDob(payload.dob, requireDob);
  if (dobError) errors.dob = dobError;

  return { normalized, errors };
};

export const getFirstValidationMessage = (errors = {}) => Object.values(errors)[0] || null;
