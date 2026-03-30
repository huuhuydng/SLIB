/**
 * Seat Layout Calculation Utility
 * Tính toán layout ghế dựa trên width của zone
 */

const SEAT_DEFAULT_WIDTH = 44;
const SEAT_DEFAULT_HEIGHT = 44;
const SEAT_MARGIN = 4; // margin giữa các ghế

/**
 * Tính số ghế có thể xếp vào 1 row dựa trên width zone
 * @param {number} zoneWidth - Width của zone
 * @returns {number} Số ghế tối đa trong 1 row
 */
export const calculateSeatsPerRow = (zoneWidth) => {
  if (!zoneWidth || zoneWidth <= 0) return 1;

  // Tính: width = (seatsPerRow * seatWidth) + ((seatsPerRow - 1) * margin)
  // width = seatsPerRow * (seatWidth + margin) - margin
  const availableWidth = zoneWidth;
  const effectiveWidth = SEAT_DEFAULT_WIDTH + SEAT_MARGIN;

  const seatsPerRow = Math.floor((availableWidth + SEAT_MARGIN) / effectiveWidth);
  return Math.max(1, seatsPerRow);
};

/**
 * Tính width mỗi ghế khi được dãn đều trong row
 * @param {number} zoneWidth - Width của zone
 * @param {number} seatsInRow - Số ghế trong row
 * @returns {number} Width của mỗi ghế (responsive)
 */
export const calculateResponsiveSeatWidth = (zoneWidth, seatsInRow) => {
  if (!seatsInRow || seatsInRow <= 0) return SEAT_DEFAULT_WIDTH;


  const totalMargin = (seatsInRow - 1) * SEAT_MARGIN;
  const seatWidth = (zoneWidth - totalMargin) / seatsInRow;

  return Math.max(20, seatWidth); // Minimum 20px
};

/**
 * Tính minimum zone dimensions để chứa hết seats
 * @param {Array} seats -
 * @returns {{minWidth: number, minHeight: number}}
 */
export const calculateMinZoneDimensions = (seats) => {
  if (!seats || seats.length === 0) {
    return { minWidth: 100, minHeight: 80 }; // Default min size
  }

  // Tìm max column và max row
  const maxColumn = Math.max(...seats.map(s => s.columnNumber || 1));
  const maxRow = Math.max(...seats.map(s => s.rowNumber || 1));

  // Padding 8px mỗi bên (left + right = 16px, top + bottom = 16px)
  const padding = 8;
  const horizontalPadding = padding * 2; // left + right
  const verticalPadding = padding * 2;   // top + bottom

  // Min width = (maxColumn - 1) * 48 + 44 + padding
  const minWidth = (maxColumn - 1) * (SEAT_DEFAULT_WIDTH + SEAT_MARGIN) + SEAT_DEFAULT_WIDTH + horizontalPadding;

  // Min height = headerHeight + (maxRow - 1) * 48 + 44 + padding
  const headerHeight = 40;
  const minHeight = headerHeight + (maxRow - 1) * (SEAT_DEFAULT_HEIGHT + SEAT_MARGIN) + SEAT_DEFAULT_HEIGHT + verticalPadding;

  return {
    minWidth: Math.max(100, minWidth),
    minHeight: Math.max(80, minHeight)
  };
};

/**
 * Tính position X của ghế dựa trên columnNumber
 * Width ghế = 44px, margin = 4px
 */
export const calculateSeatPositionX = (columnNumber) => {
  if (!columnNumber || columnNumber <= 0) return 0;
  return (columnNumber - 1) * (SEAT_DEFAULT_WIDTH + SEAT_MARGIN);
};

/**
 * Tính position Y của ghế dựa trên rowNumber
 * Height ghế = 44px, margin = 4px, header = 40px
 */
export const calculateSeatPositionY = (rowNumber) => {
  if (!rowNumber || rowNumber <= 0) return 0;
  // Y = headerHeight + (rowNumber - 1) * (44 + 4)
  const headerHeight = 40;
  return headerHeight + (rowNumber - 1) * (SEAT_DEFAULT_HEIGHT + SEAT_MARGIN);
};

/**
 * Tính tất cả các giá trị layout cho mỗi ghế (FIXED SIZE)
 * @param {Object} seat - Ghế object
 * @returns {Object} Layout data
 */
export const calculateSeatLayout = (seat) => {
  const { rowNumber, columnNumber, seatId } = seat;

  const positionX = calculateSeatPositionX(columnNumber);
  const positionY = calculateSeatPositionY(rowNumber);

  return {
    width: SEAT_DEFAULT_WIDTH,
    height: SEAT_DEFAULT_HEIGHT,
    positionX,
    positionY,
  };
};

/**
 * Tính layout ghế ĐỘNG theo kích thước zone hiện tại
 * Ghế giữ nguyên size 44x44, chỉ giãn khoảng cách giữa các ghế khi resize zone
 * 
 * @param {Object} seat - Ghế object với rowNumber, columnNumber
 * @param {number} zoneWidth - Width hiện tại của zone
 * @param {number} zoneHeight - Height hiện tại của zone  
 * @param {Array} zoneSeats - Tất cả ghế trong zone (để tính maxRow/maxCol)
 * @returns {Object} Layout data với position động, size cố định
 */
export const calculateDynamicSeatLayout = (seat, zoneWidth, zoneHeight, zoneSeats) => {
  const { rowNumber, columnNumber, seatId } = seat;

  // Tìm số cột max và số hàng max từ tất cả ghế trong zone
  const maxColumn = zoneSeats.length > 0
    ? Math.max(...zoneSeats.map(s => s.columnNumber || 1))
    : 1;
  const maxRow = zoneSeats.length > 0
    ? Math.max(...zoneSeats.map(s => s.rowNumber || 1))
    : 1;

  // Zone layout constants
  const zonePadding = 8;
  const headerHeight = 24;
  const footerHeight = 20;

  // Tính available space trong content area
  const availableWidth = Math.max(40, zoneWidth - zonePadding * 2);
  const availableHeight = Math.max(40, zoneHeight - zonePadding * 2 - headerHeight - footerHeight);

  // Giữ nguyên kích thước ghế cố định
  const seatWidth = SEAT_DEFAULT_WIDTH;  // 44px
  const seatHeight = SEAT_DEFAULT_HEIGHT; // 44px

  // Tính khoảng cách (gap) động giữa các ghế
  // Gap = (available space - total seat size) / số khoảng cách
  // Số khoảng cách = số ghế - 1 (giữa các ghế) + 2 (padding 2 bên)
  const totalSeatsWidth = maxColumn * seatWidth;
  const totalSeatsHeight = maxRow * seatHeight;

  // Gap ngang: chia đều không gian còn lại cho các khoảng cách
  // Với maxColumn ghế, có (maxColumn + 1) khoảng cách (2 bên + giữa các ghế)
  const horizontalGaps = maxColumn + 1;
  const horizontalGap = Math.max(SEAT_MARGIN, (availableWidth - totalSeatsWidth) / horizontalGaps);

  // Gap dọc: tương tự
  const verticalGaps = maxRow + 1;
  const verticalGap = Math.max(SEAT_MARGIN, (availableHeight - totalSeatsHeight) / verticalGaps);

  // Tính position - ghế được phân bố đều với gap động
  const positionX = horizontalGap + (columnNumber - 1) * (seatWidth + horizontalGap);
  const positionY = verticalGap + (rowNumber - 1) * (seatHeight + verticalGap);

  return {
    width: seatWidth,
    height: seatHeight,
    positionX,
    positionY,
  };
};

/**
 * Kiểm tra xem ghế có được phép add vào row này không
 * @param {number} rowNumber 
 * @param {number} columnNumber 
 * @param {number} zoneWidth 
 * @returns {Object} 
 */
export const validateSeatPosition = (rowNumber, columnNumber, zoneWidth) => {
  const seatsPerRow = calculateSeatsPerRow(zoneWidth);

  if (columnNumber > seatsPerRow) {
    return {
      isValid: false,
      reason: `Column ${columnNumber} vượt quá giới hạn ${seatsPerRow} ghế/row`,
      seatsPerRow,
    };
  }

  if (columnNumber < 1 || !Number.isInteger(columnNumber)) {
    return {
      isValid: false,
      reason: `Column không hợp lệ: ${columnNumber}`,
      seatsPerRow,
    };
  }

  if (rowNumber < 1 || !Number.isInteger(rowNumber)) {
    return {
      isValid: false,
      reason: `Row không hợp lệ: ${rowNumber}`,
      seatsPerRow,
    };
  }

  return {
    isValid: true,
    reason: "OK",
    seatsPerRow,
  };
};

export const SEAT_MARGIN_SIZE = SEAT_MARGIN;
export const SEAT_DEFAULT_WIDTH_SIZE = SEAT_DEFAULT_WIDTH;
export const SEAT_DEFAULT_HEIGHT_SIZE = SEAT_DEFAULT_HEIGHT;

/**
 * Tính column number tiếp theo khi add ghế vào row
 * @param {Array} seatsInZone 
 * @param {number} rowNumber
 * @returns {number} 
 */
export const getNextColumnNumber = (seatsInZone, rowNumber) => {
  if (!seatsInZone || seatsInZone.length === 0) {
    return 1;
  }

  // Lọc ghế trong cùng row
  const seatsInRow = seatsInZone.filter(s => s.rowNumber === rowNumber);

  if (seatsInRow.length === 0) {
    return 1;
  }

  // Lấy column number lớn nhất rồi +1
  const maxColumn = Math.max(...seatsInRow.map(s => s.columnNumber || 1));
  return maxColumn + 1;
};

/**
 * Convert row number to letter (1->A, 2->B, etc.)
 */
const getRowLetter = (rowNumber) => {
  if (!rowNumber || rowNumber < 1) return 'A';
  return String.fromCharCode(64 + rowNumber); // 64 + 1 = 65 ('A'), 64 + 2 = 66 ('B'), etc.
};

/**
 * Tính tất cả seat data khi tạo ghế mới
 * @param {Object} options - { zoneId, rowNumber, seatsInZone }
 * @returns {Object} Toàn bộ data cần gửi API
 */
export const generateNewSeatData = (options) => {
  const { zoneId, rowNumber, seatsInZone } = options;

  // Tính column tiếp theo
  const columnNumber = getNextColumnNumber(seatsInZone, rowNumber);

  // Generate seat code with letter format (A1, B2, etc.)
  const rowLetter = getRowLetter(rowNumber);
  const seatCode = `${rowLetter}${columnNumber}`;

  return {
    zoneId,
    seatCode,
    rowNumber,
    columnNumber,
    seatStatus: 'AVAILABLE',
  };
};
