import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

// ============ SHARED STYLES + ANIMATIONS ============
const keyframes = `
@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-12px); }
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
@keyframes shake {
  0%, 100% { transform: rotate(0deg); }
  10% { transform: rotate(-8deg); }
  20% { transform: rotate(8deg); }
  30% { transform: rotate(-6deg); }
  40% { transform: rotate(6deg); }
  50% { transform: rotate(0deg); }
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes drawCircle {
  from { stroke-dashoffset: 314; }
  to { stroke-dashoffset: 0; }
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes tickTock {
  0%, 100% { transform: rotate(-15deg); transform-origin: top center; }
  50% { transform: rotate(15deg); transform-origin: top center; }
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
@keyframes countDown {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: 251; }
}
`;

const BRAND = '#FF751F';
const BRAND_LIGHT = '#FFF3EB';
const BRAND_DARK = '#CC5D19';
const GRAY_50 = '#FAFAFA';
const GRAY_100 = '#F5F5F5';
const GRAY_400 = '#9CA3AF';
const GRAY_500 = '#6B7280';
const GRAY_700 = '#374151';
const GRAY_900 = '#111827';

const containerStyle = {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    background: `linear-gradient(160deg, ${GRAY_50} 0%, ${BRAND_LIGHT} 50%, ${GRAY_50} 100%)`,
    padding: '40px 20px',
    fontFamily: "'Be Vietnam Pro', 'Segoe UI', sans-serif",
    position: 'relative',
    overflow: 'hidden',
};

const titleStyle = {
    fontSize: '28px',
    fontWeight: '700',
    color: GRAY_900,
    margin: '0 0 12px 0',
    letterSpacing: '-0.5px',
    animation: 'slideUp 0.6s ease-out 0.3s both',
};

const descStyle = {
    fontSize: '15px',
    color: GRAY_500,
    margin: '0 0 32px 0',
    maxWidth: '440px',
    textAlign: 'center',
    lineHeight: '1.7',
    animation: 'slideUp 0.6s ease-out 0.5s both',
};

const btnPrimary = {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
    padding: '12px 32px',
    background: `linear-gradient(135deg, ${BRAND} 0%, ${BRAND_DARK} 100%)`,
    color: '#fff',
    border: 'none',
    borderRadius: '12px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    boxShadow: `0 4px 14px ${BRAND}44`,
    animation: 'slideUp 0.6s ease-out 0.7s both',
    fontFamily: "'Be Vietnam Pro', 'Segoe UI', sans-serif",
};

const btnSecondary = {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
    padding: '12px 28px',
    background: 'transparent',
    color: GRAY_700,
    border: `2px solid #E5E7EB`,
    borderRadius: '12px',
    fontSize: '15px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.3s ease',
    animation: 'slideUp 0.6s ease-out 0.7s both',
    fontFamily: "'Be Vietnam Pro', 'Segoe UI', sans-serif",
};

const svgContainer = {
    width: '200px',
    height: '200px',
    marginBottom: '32px',
    animation: 'fadeIn 0.8s ease-out',
};

const decorDot = (top, left, size, delay) => ({
    position: 'absolute',
    top, left,
    width: size, height: size,
    borderRadius: '50%',
    background: `${BRAND}15`,
    animation: `pulse 3s ease-in-out ${delay}s infinite`,
});

// ============ Background Particles ============
const Particles = () => (
    <>
        <div style={decorDot('10%', '8%', '80px', 0)} />
        <div style={decorDot('70%', '5%', '60px', 0.5)} />
        <div style={decorDot('20%', '85%', '100px', 1)} />
        <div style={decorDot('75%', '88%', '50px', 1.5)} />
        <div style={decorDot('45%', '92%', '30px', 2)} />
        <div style={decorDot('5%', '50%', '40px', 0.8)} />
    </>
);

// ============ 1. SESSION EXPIRED ============
export const SessionExpired = () => {
    const navigate = useNavigate();
    return (
        <div style={containerStyle}>
            <style>{keyframes}</style>
            <Particles />
            <div style={svgContainer}>
                <svg viewBox="0 0 200 200" fill="none">
                    {/* Hourglass */}
                    <g style={{ animation: 'float 4s ease-in-out infinite' }}>
                        {/* Circle bg */}
                        <circle cx="100" cy="100" r="80" fill={`${BRAND}08`} stroke={`${BRAND}20`} strokeWidth="2" />
                        <circle cx="100" cy="100" r="65" fill="none" stroke={BRAND} strokeWidth="3"
                            strokeDasharray="314" style={{ animation: 'drawCircle 2s ease-out forwards' }} />
                        {/* Hourglass body */}
                        <path d="M70 60 L130 60 L105 95 L130 130 L70 130 L95 95 Z"
                            fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" strokeLinejoin="round" />
                        {/* Top cap */}
                        <rect x="65" y="55" width="70" height="8" rx="4" fill={BRAND} />
                        {/* Bottom cap */}
                        <rect x="65" y="128" width="70" height="8" rx="4" fill={BRAND} />
                        {/* Sand top */}
                        <path d="M78 68 L122 68 L105 88 L95 88 Z" fill={`${BRAND}60`}>
                            <animate attributeName="d" dur="3s" repeatCount="indefinite"
                                values="M78 68 L122 68 L105 88 L95 88 Z;M90 68 L110 68 L102 78 L98 78 Z;M78 68 L122 68 L105 88 L95 88 Z" />
                        </path>
                        {/* Sand stream */}
                        <line x1="100" y1="88" x2="100" y2="105" stroke={BRAND} strokeWidth="2" strokeDasharray="4 4">
                            <animate attributeName="strokeDashoffset" from="0" to="8" dur="0.5s" repeatCount="indefinite" />
                        </line>
                        {/* Sand bottom */}
                        <path d="M85 122 L115 122 L108 112 L92 112 Z" fill={`${BRAND}60`}>
                            <animate attributeName="d" dur="3s" repeatCount="indefinite"
                                values="M85 122 L115 122 L108 112 L92 112 Z;M78 122 L122 122 L110 108 L90 108 Z;M85 122 L115 122 L108 112 L92 112 Z" />
                        </path>
                    </g>
                    {/* Decorative dots */}
                    <circle cx="150" cy="50" r="4" fill={`${BRAND}40`} style={{ animation: 'pulse 2s ease-in-out 0.5s infinite' }} />
                    <circle cx="45" cy="145" r="3" fill={`${BRAND}30`} style={{ animation: 'pulse 2s ease-in-out 1s infinite' }} />
                    <circle cx="160" cy="150" r="5" fill={`${BRAND}20`} style={{ animation: 'pulse 2s ease-in-out 1.5s infinite' }} />
                </svg>
            </div>
            <h1 style={titleStyle}>Phiên đăng nhập đã hết hạn</h1>
            <p style={descStyle}>
                Phiên làm việc của bạn đã kết thúc do không hoạt động trong thời gian dài.
                Vui lòng đăng nhập lại để tiếp tục sử dụng hệ thống.
            </p>
            <div style={{ display: 'flex', gap: '12px', animation: 'slideUp 0.6s ease-out 0.7s both' }}>
                <button style={btnPrimary}
                    onMouseEnter={e => { e.target.style.transform = 'translateY(-2px)'; e.target.style.boxShadow = `0 6px 20px ${BRAND}55`; }}
                    onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; e.target.style.boxShadow = `0 4px 14px ${BRAND}44`; }}
                    onClick={() => navigate('/login')}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4M10 17l5-5-5-5M15 12H3" />
                    </svg>
                    Đăng nhập lại
                </button>
            </div>
        </div>
    );
};

// ============ 2. TOKEN EXPIRED ============
export const TokenExpired = () => {
    const navigate = useNavigate();
    return (
        <div style={containerStyle}>
            <style>{keyframes}</style>
            <Particles />
            <div style={svgContainer}>
                <svg viewBox="0 0 200 200" fill="none">
                    <g style={{ animation: 'float 4s ease-in-out infinite' }}>
                        <circle cx="100" cy="100" r="80" fill={`${BRAND}08`} stroke={`${BRAND}20`} strokeWidth="2" />
                        {/* Key shape */}
                        <g transform="translate(60, 55)" style={{ animation: 'shake 4s ease-in-out infinite' }}>
                            {/* Key head */}
                            <circle cx="30" cy="30" r="22" fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" />
                            <circle cx="30" cy="30" r="10" fill="none" stroke={BRAND} strokeWidth="2" strokeDasharray="4 3" />
                            {/* Key shaft */}
                            <rect x="48" y="25" width="35" height="10" rx="2" fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2" />
                            {/* Key teeth */}
                            <rect x="72" y="35" width="6" height="10" rx="1" fill={BRAND} />
                            <rect x="62" y="35" width="6" height="8" rx="1" fill={BRAND} />
                        </g>
                        {/* Warning triangle */}
                        <g transform="translate(110, 80)">
                            <path d="M15 2 L28 26 L2 26 Z" fill="#FEF3C7" stroke="#F59E0B" strokeWidth="2" strokeLinejoin="round" />
                            <text x="15" y="22" textAnchor="middle" fontSize="14" fontWeight="bold" fill="#F59E0B">!</text>
                        </g>
                        {/* Crack lines */}
                        <line x1="85" y1="110" x2="75" y2="125" stroke={`${BRAND}50`} strokeWidth="2" strokeLinecap="round" />
                        <line x1="75" y1="125" x2="80" y2="135" stroke={`${BRAND}50`} strokeWidth="2" strokeLinecap="round" />
                        <line x1="115" y1="110" x2="125" y2="125" stroke={`${BRAND}40`} strokeWidth="1.5" strokeLinecap="round" />
                    </g>
                    <circle cx="40" cy="55" r="3" fill={`${BRAND}30`} style={{ animation: 'pulse 2s ease-in-out 0.3s infinite' }} />
                    <circle cx="165" cy="70" r="4" fill={`${BRAND}25`} style={{ animation: 'pulse 2s ease-in-out 0.8s infinite' }} />
                </svg>
            </div>
            <h1 style={titleStyle}>Mã xác thực đã hết hạn</h1>
            <p style={descStyle}>
                Mã xác thực (token) của bạn không còn hiệu lực. Để đảm bảo an toàn tài khoản,
                vui lòng đăng nhập lại để nhận mã xác thực mới.
            </p>
            <div style={{ display: 'flex', gap: '12px', animation: 'slideUp 0.6s ease-out 0.7s both' }}>
                <button style={btnPrimary}
                    onMouseEnter={e => { e.target.style.transform = 'translateY(-2px)'; e.target.style.boxShadow = `0 6px 20px ${BRAND}55`; }}
                    onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; e.target.style.boxShadow = `0 4px 14px ${BRAND}44`; }}
                    onClick={() => navigate('/login')}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Thử lại
                </button>
            </div>
        </div>
    );
};

// ============ 3. NOT FOUND (404) ============
export const NotFound = () => {
    const navigate = useNavigate();
    return (
        <div style={containerStyle}>
            <style>{keyframes}</style>
            <Particles />
            <div style={{ ...svgContainer, width: '240px', height: '200px' }}>
                <svg viewBox="0 0 240 200" fill="none">
                    <g style={{ animation: 'float 4s ease-in-out infinite' }}>
                        {/* Chain link 1 */}
                        <g transform="translate(30, 60)">
                            <rect x="0" y="10" width="80" height="40" rx="20" fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" />
                            <rect x="15" y="22" width="50" height="16" rx="8" fill="none" stroke={BRAND} strokeWidth="2" />
                        </g>
                        {/* Broken chain link 2 */}
                        <g transform="translate(130, 60)">
                            <rect x="0" y="10" width="80" height="40" rx="20" fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" />
                            <rect x="15" y="22" width="50" height="16" rx="8" fill="none" stroke={BRAND} strokeWidth="2" />
                        </g>
                        {/* Break indicator */}
                        <g>
                            <line x1="108" y1="72" x2="120" y2="82" stroke="#EF4444" strokeWidth="3" strokeLinecap="round" />
                            <line x1="120" y1="72" x2="108" y2="82" stroke="#EF4444" strokeWidth="3" strokeLinecap="round" />
                            {/* Spark particles */}
                            <circle cx="114" cy="68" r="2" fill="#F59E0B" style={{ animation: 'pulse 1.5s ease-in-out infinite' }} />
                            <circle cx="108" cy="88" r="2" fill="#F59E0B" style={{ animation: 'pulse 1.5s ease-in-out 0.3s infinite' }} />
                            <circle cx="122" cy="90" r="1.5" fill="#F59E0B" style={{ animation: 'pulse 1.5s ease-in-out 0.6s infinite' }} />
                        </g>
                        {/* 404 text */}
                        <text x="120" y="145" textAnchor="middle" fontSize="42" fontWeight="800"
                            fill={BRAND} opacity="0.15" fontFamily="'Be Vietnam Pro', sans-serif">404</text>
                    </g>
                    <circle cx="30" cy="40" r="4" fill={`${BRAND}30`} style={{ animation: 'pulse 2s ease-in-out 0.5s infinite' }} />
                    <circle cx="210" cy="150" r="5" fill={`${BRAND}20`} style={{ animation: 'pulse 2s ease-in-out 1s infinite' }} />
                    <circle cx="195" cy="45" r="3" fill={`${BRAND}25`} style={{ animation: 'pulse 2s ease-in-out 0.8s infinite' }} />
                </svg>
            </div>
            <h1 style={titleStyle}>Liên kết bị hỏng</h1>
            <p style={descStyle}>
                Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
                Vui lòng kiểm tra lại đường dẫn hoặc quay về trang chủ.
            </p>
            <div style={{ display: 'flex', gap: '12px', animation: 'slideUp 0.6s ease-out 0.7s both' }}>
                <button style={btnPrimary}
                    onMouseEnter={e => { e.target.style.transform = 'translateY(-2px)'; e.target.style.boxShadow = `0 6px 20px ${BRAND}55`; }}
                    onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; e.target.style.boxShadow = `0 4px 14px ${BRAND}44`; }}
                    onClick={() => navigate(-1)}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <path d="M19 12H5M12 19l-7-7 7-7" />
                    </svg>
                    Quay lại
                </button>
                <button style={btnSecondary}
                    onMouseEnter={e => { e.target.style.borderColor = BRAND; e.target.style.color = BRAND; }}
                    onMouseLeave={e => { e.target.style.borderColor = '#E5E7EB'; e.target.style.color = GRAY_700; }}
                    onClick={() => navigate('/')}
                >
                    Về trang chủ
                </button>
            </div>
        </div>
    );
};

// ============ 4. SERVER ERROR (500) ============
export const ServerError = () => {
    const navigate = useNavigate();
    return (
        <div style={containerStyle}>
            <style>{keyframes}</style>
            <Particles />
            <div style={svgContainer}>
                <svg viewBox="0 0 200 200" fill="none">
                    <g style={{ animation: 'float 4s ease-in-out infinite' }}>
                        <circle cx="100" cy="100" r="80" fill={`${BRAND}08`} stroke={`${BRAND}20`} strokeWidth="2" />
                        {/* Warning sign */}
                        <g transform="translate(55, 45)">
                            {/* Sign pole */}
                            <rect x="43" y="85" width="6" height="35" rx="3" fill={GRAY_400} />
                            {/* Sign board */}
                            <path d="M10 10 L80 10 L90 50 L80 90 L10 90 L0 50 Z"
                                fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" strokeLinejoin="round" />
                            {/* Exclamation mark */}
                            <rect x="41" y="28" width="8" height="32" rx="4" fill={BRAND} />
                            <circle cx="45" cy="72" r="5" fill={BRAND} />
                        </g>
                        {/* Gear */}
                        <g transform="translate(135, 110)" style={{ animation: 'spin 6s linear infinite' }}>
                            <circle cx="0" cy="0" r="12" fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2" />
                            <circle cx="0" cy="0" r="5" fill={BRAND} />
                            {[0, 45, 90, 135, 180, 225, 270, 315].map((angle, i) => (
                                <rect key={i} x="-3" y="-16" width="6" height="8" rx="2" fill={BRAND}
                                    transform={`rotate(${angle})`} />
                            ))}
                        </g>
                    </g>
                    <circle cx="45" cy="55" r="3" fill={`${BRAND}30`} style={{ animation: 'pulse 2s ease-in-out 0.5s infinite' }} />
                    <circle cx="165" cy="55" r="4" fill={`${BRAND}20`} style={{ animation: 'pulse 2s ease-in-out 1s infinite' }} />
                </svg>
            </div>
            <h1 style={titleStyle}>Đã xảy ra lỗi</h1>
            <p style={descStyle}>
                Hệ thống đang gặp sự cố kỹ thuật. Đội ngũ phát triển đã được thông báo và
                đang khắc phục. Vui lòng thử lại sau ít phút.
            </p>
            <div style={{ display: 'flex', gap: '12px', animation: 'slideUp 0.6s ease-out 0.7s both' }}>
                <button style={btnPrimary}
                    onMouseEnter={e => { e.target.style.transform = 'translateY(-2px)'; e.target.style.boxShadow = `0 6px 20px ${BRAND}55`; }}
                    onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; e.target.style.boxShadow = `0 4px 14px ${BRAND}44`; }}
                    onClick={() => window.location.reload()}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Thử lại
                </button>
                <button style={btnSecondary}
                    onMouseEnter={e => { e.target.style.borderColor = BRAND; e.target.style.color = BRAND; }}
                    onMouseLeave={e => { e.target.style.borderColor = '#E5E7EB'; e.target.style.color = GRAY_700; }}
                    onClick={() => navigate('/')}
                >
                    Về trang chủ
                </button>
            </div>
        </div>
    );
};

// ============ 5. SESSION TIMEOUT ============
export const SessionTimeout = () => {
    const navigate = useNavigate();
    const [countdown, setCountdown] = useState(10);

    useEffect(() => {
        const timer = setInterval(() => {
            setCountdown(prev => {
                if (prev <= 1) {
                    clearInterval(timer);
                    navigate('/login');
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [navigate]);

    return (
        <div style={containerStyle}>
            <style>{keyframes}</style>
            <Particles />
            <div style={svgContainer}>
                <svg viewBox="0 0 200 200" fill="none">
                    <g style={{ animation: 'float 4s ease-in-out infinite' }}>
                        <circle cx="100" cy="100" r="80" fill={`${BRAND}08`} stroke={`${BRAND}20`} strokeWidth="2" />
                        {/* Clock face */}
                        <circle cx="100" cy="88" r="42" fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" />
                        <circle cx="100" cy="88" r="38" fill="white" stroke={`${BRAND}30`} strokeWidth="1" />
                        {/* Clock center */}
                        <circle cx="100" cy="88" r="4" fill={BRAND} />
                        {/* Hour marks */}
                        {[0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330].map((angle, i) => (
                            <line key={i} x1="100" y1="54" x2="100" y2={i % 3 === 0 ? "58" : "56"}
                                stroke={i % 3 === 0 ? BRAND : `${BRAND}60`}
                                strokeWidth={i % 3 === 0 ? "2.5" : "1.5"} strokeLinecap="round"
                                transform={`rotate(${angle} 100 88)`} />
                        ))}
                        {/* Hour hand */}
                        <line x1="100" y1="88" x2="100" y2="68" stroke={GRAY_700} strokeWidth="3" strokeLinecap="round"
                            transform="rotate(120 100 88)" />
                        {/* Minute hand */}
                        <line x1="100" y1="88" x2="100" y2="60" stroke={BRAND} strokeWidth="2" strokeLinecap="round"
                            style={{ animation: 'tickTock 2s ease-in-out infinite', transformOrigin: '100px 88px' }} />
                        {/* Zzz */}
                        <g transform="translate(138, 55)" style={{ animation: 'pulse 2s ease-in-out infinite' }}>
                            <text fontSize="16" fontWeight="700" fill={`${BRAND}70`} fontFamily="'Be Vietnam Pro', sans-serif">Z</text>
                            <text x="12" y="-8" fontSize="12" fontWeight="700" fill={`${BRAND}50`} fontFamily="'Be Vietnam Pro', sans-serif">z</text>
                            <text x="20" y="-14" fontSize="9" fontWeight="700" fill={`${BRAND}30`} fontFamily="'Be Vietnam Pro', sans-serif">z</text>
                        </g>
                        {/* Sad face on clock */}
                        <circle cx="90" cy="82" r="2.5" fill={GRAY_700} />
                        <circle cx="110" cy="82" r="2.5" fill={GRAY_700} />
                        <path d="M90 100 Q100 94 110 100" stroke={GRAY_700} strokeWidth="2" fill="none" strokeLinecap="round" />
                    </g>
                    <circle cx="38" cy="130" r="3" fill={`${BRAND}30`} style={{ animation: 'pulse 2s ease-in-out 0.5s infinite' }} />
                    <circle cx="170" cy="160" r="4" fill={`${BRAND}20`} style={{ animation: 'pulse 2s ease-in-out 1s infinite' }} />
                </svg>
            </div>
            <h1 style={titleStyle}>Hết thời gian phiên</h1>
            <p style={descStyle}>
                Phiên làm việc đã hết thời gian do không có thao tác nào được thực hiện.
                Bạn sẽ được chuyển về trang đăng nhập sau <strong style={{ color: BRAND }}>{countdown} giây</strong>.
            </p>
            <div style={{ display: 'flex', gap: '12px', animation: 'slideUp 0.6s ease-out 0.7s both' }}>
                <button style={btnPrimary}
                    onMouseEnter={e => { e.target.style.transform = 'translateY(-2px)'; e.target.style.boxShadow = `0 6px 20px ${BRAND}55`; }}
                    onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; e.target.style.boxShadow = `0 4px 14px ${BRAND}44`; }}
                    onClick={() => navigate('/login')}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4M10 17l5-5-5-5M15 12H3" />
                    </svg>
                    Đăng nhập ngay
                </button>
            </div>
            {/* Countdown progress */}
            <div style={{ marginTop: '24px', animation: 'fadeIn 1s ease-out 1s both' }}>
                <svg width="50" height="50" viewBox="0 0 50 50">
                    <circle cx="25" cy="25" r="20" fill="none" stroke={`${BRAND}15`} strokeWidth="3" />
                    <circle cx="25" cy="25" r="20" fill="none" stroke={BRAND} strokeWidth="3"
                        strokeDasharray="125.6" strokeDashoffset={125.6 * (1 - countdown / 10)}
                        strokeLinecap="round" transform="rotate(-90 25 25)"
                        style={{ transition: 'stroke-dashoffset 1s linear' }} />
                    <text x="25" y="30" textAnchor="middle" fontSize="14" fontWeight="700"
                        fill={BRAND} fontFamily="'Be Vietnam Pro', sans-serif">{countdown}</text>
                </svg>
            </div>
        </div>
    );
};

// ============ 6. FORBIDDEN (403) ============
export const Forbidden = () => {
    const navigate = useNavigate();
    return (
        <div style={containerStyle}>
            <style>{keyframes}</style>
            <Particles />
            <div style={svgContainer}>
                <svg viewBox="0 0 200 200" fill="none">
                    <g style={{ animation: 'float 4s ease-in-out infinite' }}>
                        <circle cx="100" cy="100" r="80" fill={`${BRAND}08`} stroke={`${BRAND}20`} strokeWidth="2" />
                        {/* Shield */}
                        <path d="M100 40 L145 60 L145 105 Q145 145 100 165 Q55 145 55 105 L55 60 Z"
                            fill={BRAND_LIGHT} stroke={BRAND} strokeWidth="2.5" strokeLinejoin="round" />
                        {/* Inner shield */}
                        <path d="M100 52 L135 68 L135 102 Q135 136 100 152 Q65 136 65 102 L65 68 Z"
                            fill="white" stroke={`${BRAND}30`} strokeWidth="1.5" strokeLinejoin="round" />
                        {/* Lock icon on shield */}
                        <rect x="88" y="92" width="24" height="20" rx="4" fill={BRAND} />
                        <path d="M93 92 L93 82 Q93 72 100 72 Q107 72 107 82 L107 92"
                            fill="none" stroke={BRAND} strokeWidth="3" strokeLinecap="round" />
                        {/* Keyhole */}
                        <circle cx="100" cy="100" r="3" fill="white" />
                        <rect x="99" y="102" width="2" height="5" rx="1" fill="white" />
                        {/* No entry slash */}
                        <line x1="65" y1="140" x2="135" y2="55" stroke="#EF4444" strokeWidth="4" strokeLinecap="round" opacity="0.6" />
                    </g>
                    <circle cx="40" cy="50" r="4" fill={`${BRAND}30`} style={{ animation: 'pulse 2s ease-in-out 0.5s infinite' }} />
                    <circle cx="165" cy="155" r="5" fill={`${BRAND}20`} style={{ animation: 'pulse 2s ease-in-out 1s infinite' }} />
                </svg>
            </div>
            <h1 style={titleStyle}>Không có quyền truy cập</h1>
            <p style={descStyle}>
                Bạn không có quyền truy cập vào trang này. Nếu bạn cho rằng đây là lỗi,
                vui lòng liên hệ quản trị viên để được hỗ trợ.
            </p>
            <div style={{ display: 'flex', gap: '12px', animation: 'slideUp 0.6s ease-out 0.7s both' }}>
                <button style={btnPrimary}
                    onMouseEnter={e => { e.target.style.transform = 'translateY(-2px)'; e.target.style.boxShadow = `0 6px 20px ${BRAND}55`; }}
                    onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; e.target.style.boxShadow = `0 4px 14px ${BRAND}44`; }}
                    onClick={() => navigate(-1)}
                >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                        <path d="M19 12H5M12 19l-7-7 7-7" />
                    </svg>
                    Quay lại
                </button>
                <button style={btnSecondary}
                    onMouseEnter={e => { e.target.style.borderColor = BRAND; e.target.style.color = BRAND; }}
                    onMouseLeave={e => { e.target.style.borderColor = '#E5E7EB'; e.target.style.color = GRAY_700; }}
                    onClick={() => navigate('/')}
                >
                    Về trang chủ
                </button>
            </div>
        </div>
    );
};

export default {
    SessionExpired,
    TokenExpired,
    NotFound,
    ServerError,
    SessionTimeout,
    Forbidden,
};
