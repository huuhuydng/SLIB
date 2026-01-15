// ============================================
// SỬA LỖI: Tìm đoạn code này trong SeatManage.jsx
// ============================================

// ❌ CODE CŨ SAI (XÓA ĐOẠN NÀY):
useEffect(() => {
  loadSeatDataForTimeSlot(0);
  
  const interval = setInterval(() => {
    if (currentSlotIndex === 0) {
      loadSeatDataForTimeSlot(0);
    }
  }, 30000);
  
  return () => clearInterval(interval);
}, [currentSlotIndex]);  // ❌ Dependency này gây ra bug!

useEffect(() => {
  loadSeatDataForTimeSlot(currentSlotIndex);
}, [currentSlotIndex]);


// ✅ CODE MỚI ĐÚNG (THAY BẰNG ĐOẠN NÀY):
useEffect(() => {
  // Load data cho slot hiện tại
  loadSeatDataForTimeSlot(currentSlotIndex);
  
  // Chỉ auto-refresh khi đang ở "Hiện tại" tab
  const interval = setInterval(() => {
    if (currentSlotIndex === 0) {
      loadSeatDataForTimeSlot(0);
    }
  }, 30000);
  
  return () => clearInterval(interval);
}, [currentSlotIndex]);
// ✅ Chỉ cần 1 useEffect thôi, không cần 2!
