export const normalizeText = (value) => {
  if (typeof value !== 'string') return '';
  return value.trim().replace(/\s+/g, ' ');
};

export const isValidHttpUrl = (value) => {
  if (!value) return true;
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
};

export const getApiErrorMessage = (error, fallbackMessage = 'Dữ liệu không hợp lệ') => {
  const data = error?.response?.data;

  if (data?.errors && typeof data.errors === 'object') {
    const firstError = Object.values(data.errors).find(Boolean);
    if (firstError) {
      return firstError;
    }
  }

  return data?.message || data?.error || error?.message || fallbackMessage;
};

export const validateNewBookPayload = (payload) => {
  const title = normalizeText(payload.title);
  if (!title) return 'Tiêu đề sách không được để trống';
  if (title.length > 300) return 'Tiêu đề sách không được vượt quá 300 ký tự';

  const author = normalizeText(payload.author);
  if (author.length > 200) return 'Tác giả không được vượt quá 200 ký tự';

  const isbn = normalizeText(payload.isbn);
  if (isbn.length > 20) return 'ISBN không được vượt quá 20 ký tự';

  const publisher = normalizeText(payload.publisher);
  if (publisher.length > 255) return 'Nhà xuất bản không được vượt quá 255 ký tự';

  const category = normalizeText(payload.category);
  if (category.length > 255) return 'Thể loại không được vượt quá 255 ký tự';

  const description = normalizeText(payload.description);
  if (description.length > 5000) return 'Mô tả sách không được vượt quá 5000 ký tự';

  if (payload.coverUrl && !isValidHttpUrl(payload.coverUrl)) return 'Ảnh bìa phải là URL hợp lệ';
  if (payload.sourceUrl && !isValidHttpUrl(payload.sourceUrl)) return 'Link nguồn OPAC phải là URL hợp lệ';

  if (payload.publishYear) {
    const year = Number(payload.publishYear);
    if (!Number.isInteger(year) || year < 1000 || year > 3000) {
      return 'Năm phát hành phải nằm trong khoảng 1000 đến 3000';
    }
  }

  return null;
};

export const validateNewsPayload = ({ title, summary, imageUrl, publishStatus, scheduleDate, scheduleTime, customCategory }) => {
  const normalizedTitle = normalizeText(title);
  if (!normalizedTitle) return 'Tiêu đề tin tức không được để trống';
  if (normalizedTitle.length > 255) return 'Tiêu đề tin tức không được vượt quá 255 ký tự';

  const normalizedSummary = normalizeText(summary);
  if (normalizedSummary.length > 1000) return 'Tóm tắt không được vượt quá 1000 ký tự';

  if (imageUrl && !isValidHttpUrl(imageUrl)) return 'Đường dẫn ảnh phải là URL hợp lệ';

  if (customCategory && normalizeText(customCategory).length > 50) {
    return 'Tên danh mục không được vượt quá 50 ký tự';
  }

  if (publishStatus === 'schedule') {
    if (!scheduleDate || !scheduleTime) {
      return 'Vui lòng chọn ngày và giờ đăng';
    }
    const scheduledAt = new Date(`${scheduleDate}T${scheduleTime}`);
    if (Number.isNaN(scheduledAt.getTime()) || scheduledAt <= new Date()) {
      return 'Thời gian lên lịch phải ở tương lai';
    }
  }

  return null;
};

export const validateKioskPayload = (payload, { isEdit = false } = {}) => {
  const kioskCode = normalizeText(payload.kioskCode);
  const kioskName = normalizeText(payload.kioskName);
  const kioskType = normalizeText(payload.kioskType);
  const location = normalizeText(payload.location);

  if (!isEdit) {
    if (!kioskCode) return 'Mã kiosk không được để trống';
    if (!/^[A-Za-z0-9_-]+$/.test(kioskCode)) {
      return 'Mã kiosk chỉ được chứa chữ cái, số, gạch ngang hoặc gạch dưới';
    }
    if (kioskCode.length > 50) return 'Mã kiosk không được vượt quá 50 ký tự';
  }

  if (!kioskName) return 'Tên kiosk không được để trống';
  if (kioskName.length > 255) return 'Tên kiosk không được vượt quá 255 ký tự';

  if (!kioskType) return 'Loại kiosk không được để trống';
  if (!['INTERACTIVE', 'MONITORING'].includes(kioskType)) {
    return 'Loại kiosk phải là INTERACTIVE hoặc MONITORING';
  }

  if (location.length > 255) return 'Vị trí không được vượt quá 255 ký tự';

  return null;
};
