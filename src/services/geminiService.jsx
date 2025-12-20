const MOCK_INSIGHTS = [
  {
    type: "warning",
    title: "Cảnh báo đông đúc",
    message:
      "Zone A (Khu yên tĩnh) đã được lấp 95% khu vực. Hãy điều hướng sinh viên sang Zone B (thảo luận).",
  },
  {
    type: "info",
    title: "Dự báo cao điểm",
    message: "Lượng sinh viên sẽ tăng đột biến sau 10:00 khi kết thúc tiết học đầu tiên.",
  },
];

export const getLibraryInsights = async () => {
  return MOCK_INSIGHTS;
};
