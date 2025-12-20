import React, { useMemo, useState } from "react";
import { Search, Users, Armchair, AlertCircle, Sparkles, Clock } from "lucide-react";
import StatCard from "./StatCard";
import { getLibraryInsights } from "../../services/geminiService";

const MOCK_STUDENTS = [
  { id: "1", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check in", time: "12:21:10", date: "15/12/2025" },
  { id: "2", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
  { id: "3", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
  { id: "4", name: "Nguyễn Hoàng Phúc", studentId: "DE170706", action: "Check out", time: "12:21:10", date: "15/12/2025" },
];

const MOCK_NOTIFICATIONS = [
  "[Thông báo] sự kiện gây hội mượn sách tại trường đại học FPT",
  "[Thông báo] lịch bảo trì khu vực học",
  "[Thông báo] lịch trả sách tháng 12",
  "[Thông báo] danh sách sinh viên bị cấm vào thư viện",
  "[Thông báo] cách tính điểm đánh giá dành cho sinh viên sử dụng thư viện",
];

const AREA = [
  { name: "Khu yên tĩnh", percentage: 95 },
  { name: "Khu thảo luận", percentage: 45 },
  { name: "Khu tự học", percentage: 70 },
];

const DASHBOARD_STATS = { currentUsers: 69, occupancyRate: 69, violations: 9 };

const Dashboard = () => {
  const [searchText, setSearchText] = useState("");
  const [insights, setInsights] = useState([]);

  React.useEffect(() => {
    (async () => {
      try {
        const data = await getLibraryInsights(DASHBOARD_STATS);
        setInsights(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
        setInsights([]);
      }
    })();
  }, []);

  const filteredStudents = useMemo(() => {
    const q = searchText.trim().toLowerCase();
    if (!q) return MOCK_STUDENTS;
    return MOCK_STUDENTS.filter((s) => {
      return (
        s.name.toLowerCase().includes(q) ||
        s.studentId.toLowerCase().includes(q) ||
        s.action.toLowerCase().includes(q)
      );
    });
  }, [searchText]);

  const fillClass = (p) => (p >= 90 ? "fillRed" : p >= 60 ? "fillYellow" : "fillGreen");

  return (
    <div className="main">
      {/* topbar */}
      <div className="topbar">
        <div className="searchBox">
          <Search size={18} style={{ opacity: 0.55 }} />
          <input
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            placeholder="Search for anything..."
          />
        </div>

        <div className="profile">
          <img src="https://picsum.photos/80/80" alt="avatar" />
          <div>
            <p className="profile__name">PhucNH</p>
            <p className="profile__role">Librarian</p>
          </div>
        </div>
      </div>

      <div className="h1">Dashboard</div>

      {/* stats */}
      <div className="statsRow">
        <StatCard
          icon={<Users size={20} />}
          value={DASHBOARD_STATS.currentUsers}
          label="Đang trong thư viện"
          bg="#EDE9FE"
          color="#7C3AED"
        />
        <StatCard
          icon={<Armchair size={20} />}
          value={`${DASHBOARD_STATS.occupancyRate}%`}
          label="Chỗ ngồi đã có người"
          bg="#DCFCE7"
          color="#16A34A"
        />
        <StatCard
          icon={<AlertCircle size={20} />}
          value={`0${DASHBOARD_STATS.violations}`}
          label="Vi phạm xảy ra hôm nay"
          bg="#FEE2E2"
          color="#EF4444"
        />
      </div>

      {/* middle */}
      <div className="gridMid">
        <section className="panel">
          <div className="panelTitle">Danh sách sinh viên ra vào</div>

          <table className="table">
            <thead>
              <tr>
                <th>Tên sinh viên</th>
                <th>Mã số sinh viên</th>
                <th>Hành động</th>
                <th>Thời gian</th>
              </tr>
            </thead>

            <tbody>
              {filteredStudents.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td style={{ color: "#6b7280", fontWeight: 700 }}>{s.studentId}</td>
                  <td>
                    <span className={`badge ${s.action === "Check in" ? "badgeIn" : "badgeOut"}`}>
                      {s.action}
                    </span>
                  </td>
                  <td style={{ color: "#6b7280", fontWeight: 700 }}>
                    {s.time} {s.date}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="panel">
          <div className="aiHeader">
            <Sparkles size={16} />
            <div className="panelTitle" style={{ margin: 0 }}>AI phân tích</div>
          </div>

          {(insights || []).map((it, idx) => (
            <div
              key={idx}
              className={`aiCard ${it.type === "warning" ? "aiCard--warn" : "aiCard--info"}`}
            >
              <div style={{ paddingTop: 1 }}>
                {it.type === "warning" ? <AlertCircle size={16} /> : <Clock size={16} />}
              </div>
              <div>
                <p className="aiTitle">{it.title}</p>
                <p className="aiMsg">{it.message}</p>
              </div>
            </div>
          ))}
        </section>
      </div>

      {/* bottom */}
      <div className="gridBottom">
        <section className="noticeBox">
          <div className="panelTitle">Thông báo gần đây</div>
          {MOCK_NOTIFICATIONS.map((t, idx) => (
            <div key={idx} className="noticeItem">{t}</div>
          ))}
        </section>

        <section className="panel">
          <div className="panelTitle">Trạng thái khu vực</div>

          <div className="legend">
            <span><i className="dot dotGreen" />Trống</span>
            <span><i className="dot dotYellow" />Khá đông</span>
            <span><i className="dot dotRed" />Full</span>
          </div>

          {AREA.map((a, idx) => (
            <div key={idx} className="areaRow">
              <div className="areaTop">
                <span>{a.name}</span>
                <span>{a.percentage}%</span>
              </div>
              <div className="bar">
                <div className={`fill ${fillClass(a.percentage)}`} style={{ width: `${a.percentage}%` }} />
              </div>
            </div>
          ))}
        </section>
      </div>
    </div>
  );
};

export default Dashboard;
