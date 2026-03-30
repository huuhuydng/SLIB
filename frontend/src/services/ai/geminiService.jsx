export const getLibraryInsights = async (stats) => {
  if (!stats) {
    return [];
  }

  const occupancyRate = Number(stats.occupancyRate || 0);
  const violationsToday = Number(stats.violationsToday || 0);
  const pendingSupport = Number(stats.pendingSupportRequests || 0);
  const inProgressSupport = Number(stats.inProgressSupportRequests || 0);
  const activeBookings = Number(stats.activeBookings || 0);
  const currentlyInLibrary = Number(stats.currentlyInLibrary || 0);
  const busiestArea = Array.isArray(stats.areaOccupancies)
    ? [...stats.areaOccupancies].sort(
        (a, b) => Number(b.occupancyPercentage || 0) - Number(a.occupancyPercentage || 0),
      )[0]
    : null;

  const insights = [];

  if (occupancyRate >= 85) {
    insights.push({
      type: "warning",
      title: "Mật độ thư viện đang rất cao",
      message: `Tỷ lệ sử dụng ghế hiện tại khoảng ${occupancyRate.toFixed(0)}%. Nên điều phối sinh viên sang khu còn trống để tránh quá tải cục bộ.`,
    });
  } else if (occupancyRate >= 60) {
    insights.push({
      type: "info",
      title: "Mật độ sử dụng đang tăng",
      message: `Thư viện đang vận hành ở mức ${occupancyRate.toFixed(0)}% công suất. Đây là thời điểm phù hợp để theo dõi sát các khu đông.`,
    });
  } else {
    insights.push({
      type: "info",
      title: "Mật độ hiện tại ổn định",
      message: `Hiện có ${currentlyInLibrary} sinh viên trong thư viện và còn dư địa chỗ ngồi tương đối tốt cho các lượt đặt mới.`,
    });
  }

  if (busiestArea?.areaName) {
    insights.push({
      type: Number(busiestArea.occupancyPercentage || 0) >= 90 ? "warning" : "info",
      title: "Khu vực cần theo dõi",
      message: `${busiestArea.areaName} đang là khu có mật độ cao nhất, khoảng ${Number(
        busiestArea.occupancyPercentage || 0,
      ).toFixed(0)}% số ghế đã được sử dụng.`,
    });
  }

  if (violationsToday > 0) {
    insights.push({
      type: "warning",
      title: "Cần xử lý vi phạm trong ngày",
      message: `Hôm nay đã ghi nhận ${violationsToday} trường hợp vi phạm. Nên ưu tiên rà các báo cáo mới và complaint đang chờ xử lý.`,
    });
  } else if (pendingSupport + inProgressSupport > 0) {
    insights.push({
      type: "info",
      title: "Yêu cầu hỗ trợ đang hoạt động",
      message: `Hiện có ${pendingSupport} yêu cầu chờ nhận và ${inProgressSupport} yêu cầu đang xử lý. Cần bảo đảm các cuộc chat hỗ trợ không bị tồn đọng.`,
    });
  } else if (activeBookings > 0) {
    insights.push({
      type: "info",
      title: "Lượt đặt chỗ đang hoạt động",
      message: `Có ${activeBookings} lượt đặt chỗ đang còn hiệu lực. Nên theo dõi các lượt gần giờ check-in để tránh no-show tăng đột biến.`,
    });
  }

  return insights.slice(0, 3);
};
