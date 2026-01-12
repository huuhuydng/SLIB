import React, { useState } from 'react';
import {
  Sparkles,
  AlertCircle,
  ChevronRight,
  ChevronLeft,
  List,
  CalendarOff,
  Briefcase
} from 'lucide-react';
import Header from './Header';

// Mock Data
const CHART_DATA = [
  { day: 'Mon', value: 35 },
  { day: 'Tue', value: 28 },
  { day: 'Wed', value: 50 },
  { day: 'Thu', value: 35 },
  { day: 'Fri', value: 80 }, // Highest
  { day: 'Sat', value: 35 },
  { day: 'Sun', value: 50 },
];

const ZONE_USAGE = [
  { name: 'Khu yên tĩnh', percent: 95, color: '#EF4444' }, // Red
  { name: 'Khu thảo luận', percent: 45, color: '#10B981' }, // Green
  { name: 'Khu tự học', percent: 70, color: '#FACC15' },   // Yellow
];

const FEEDBACKS = [
  {
    id: 1,
    date: '10:35-15/12/2023',
    content: 'Technical English for Beginners',
    user: 'PhucNH',
    code: 'DE170706',
    avatar: 'https://picsum.photos/id/64/40/40'
  },
  {
    id: 2,
    date: '10:35-15/12/2023',
    content: 'Anh bàn bên đẹp trai quá',
    user: 'PhucNH',
    code: 'DE170706',
    avatar: 'https://picsum.photos/id/65/40/40'
  },
  {
    id: 3,
    date: '09:00-14/12/2023',
    content: 'Wifi hôm nay hơi lag ở khu B',
    user: 'Minh Anh',
    code: 'DE182201',
    avatar: 'https://picsum.photos/id/66/40/40'
  },
  {
    id: 4,
    date: '14:20-13/12/2023',
    content: 'Cần thêm ổ cắm điện',
    user: 'Hoang Long',
    code: 'DE170999',
    avatar: 'https://picsum.photos/id/67/40/40'
  }
];

const Statistic = () => {
  const [feedbackIndex, setFeedbackIndex] = useState(0);

  const handlePrevFeedback = () => {
    setFeedbackIndex((prev) => Math.max(0, prev - 1));
  };

  const handleNextFeedback = () => {
    setFeedbackIndex((prev) => Math.min(FEEDBACKS.length - 2, prev + 1));
  };

  const visibleFeedbacks = FEEDBACKS.slice(feedbackIndex, feedbackIndex + 2);

  return (
    <>
      <Header searchPlaceholder="Search for anything..." />

      <div style={{
        padding: '2rem',
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: '#f9fafb',
        minHeight: 'calc(100vh - 80px)'
      }}>
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(2, 1fr)',
        gap: '1.5rem'
      }}>
        
        {/* Card 1: Đặt chỗ & Bar Chart */}
        <div style={{
          backgroundColor: 'white',
          borderRadius: '16px',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1rem'
          }}>
            <h3 style={{
              fontSize: '1.125rem',
              fontWeight: '600',
              color: '#111827',
              margin: 0
            }}>Đặt chỗ</h3>
            <span style={{
              display: 'inline-flex',
              alignItems: 'center',
              padding: '0.25rem 0.75rem',
              backgroundColor: '#f3f4f6',
              borderRadius: '12px',
              fontSize: '0.75rem',
              color: '#6B7280'
            }}>
              <CalendarOff size={12} style={{marginRight: 4}}/> last 7 days
            </span>
          </div>
          
          <div style={{
            backgroundColor: '#faf5ff',
            borderRadius: '12px',
            padding: '1rem',
            marginBottom: '1.5rem'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginBottom: '0.75rem'
            }}>
              <Sparkles size={16} color="#C026D3" />
              <span style={{
                fontSize: '0.875rem',
                fontWeight: '600',
                color: '#C026D3'
              }}>AI phân tích</span>
            </div>
            <div style={{
              display: 'flex',
              gap: '0.75rem',
              backgroundColor: '#fff7ed',
              padding: '1rem',
              borderRadius: '8px',
              border: '1px solid #fed7aa'
            }}>
              <div style={{
                width: '40px',
                height: '40px',
                backgroundColor: '#ffedd5',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <AlertCircle size={20} color="#EA580C" />
              </div>
              <div>
                <strong style={{
                  display: 'block',
                  fontSize: '0.875rem',
                  color: '#9A3412',
                  marginBottom: '0.25rem'
                }}>Cảnh báo đông đúc</strong>
                <p style={{
                  margin: 0,
                  fontSize: '0.813rem',
                  color: '#9A3412',
                  lineHeight: '1.5'
                }}>Khu yên tĩnh là khu vực thường xuyên được sử dụng. Hãy điều hướng sinh viên sang các khu vực khác để tận dụng không gian</p>
              </div>
            </div>
          </div>

          <div style={{
            display: 'flex',
            height: '200px',
            gap: '0.5rem'
          }}>
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              fontSize: '0.75rem',
              color: '#9CA3AF',
              paddingRight: '0.5rem'
            }}>
              <span>100</span>
              <span>75</span>
              <span>50</span>
              <span>25</span>
              <span>0</span>
            </div>
            <div style={{
              flex: 1,
              display: 'flex',
              alignItems: 'flex-end',
              justifyContent: 'space-around',
              gap: '0.5rem',
              borderBottom: '1px solid #e5e7eb',
              paddingBottom: '2rem',
              position: 'relative'
            }}>
              {CHART_DATA.map((item, idx) => (
                <div key={idx} style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  flex: 1,
                  gap: '0.5rem'
                }}>
                  <div style={{
                    width: '100%',
                    height: `${item.value * 2}px`,
                    backgroundColor: item.value === 80 ? '#3b82f6' : '#e5e7eb',
                    borderRadius: '4px 4px 0 0',
                    transition: 'all 0.3s'
                  }}></div>
                  <span style={{
                    fontSize: '0.75rem',
                    color: '#6B7280',
                    position: 'absolute',
                    bottom: '0.5rem',
                    transform: 'translateX(-50%)',
                    left: `${(idx + 0.5) * (100 / CHART_DATA.length)}%`
                  }}>{item.day}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Card 2: Tỉ lệ vi phạm */}
        <div style={{
          backgroundColor: 'white',
          borderRadius: '16px',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1.5rem'
          }}>
            <h3 style={{
              fontSize: '1.125rem',
              fontWeight: '600',
              color: '#111827',
              margin: 0
            }}>Tỉ lệ vi phạm đặt chỗ</h3>
            <span style={{
              display: 'inline-flex',
              alignItems: 'center',
              padding: '0.25rem 0.75rem',
              backgroundColor: '#f3f4f6',
              borderRadius: '12px',
              fontSize: '0.75rem',
              color: '#6B7280'
            }}>
              <CalendarOff size={12} style={{marginRight: 4}}/> last 7 days
            </span>
          </div>
          
          <div style={{
            textAlign: 'center',
            marginBottom: '2rem'
          }}>
            <div style={{
              fontSize: '3rem',
              fontWeight: '700',
              color: '#111827',
              marginBottom: '0.5rem'
            }}>75%</div>
            <div style={{
              fontSize: '0.875rem',
              color: '#6B7280'
            }}>Sinh viên đã tuân thủ</div>
            
            <div style={{
              width: '100%',
              height: '8px',
              backgroundColor: '#f3f4f6',
              borderRadius: '4px',
              overflow: 'hidden',
              display: 'flex',
              marginTop: '1.5rem',
              marginBottom: '0.5rem'
            }}>
              <div style={{
                width: '75%',
                backgroundColor: '#10B981'
              }}></div>
              <div style={{
                width: '20%',
                backgroundColor: '#FACC15'
              }}></div>
              <div style={{
                width: '5%',
                backgroundColor: '#EF4444'
              }}></div>
            </div>
            <div style={{
              position: 'relative',
              fontSize: '0.75rem',
              color: '#6B7280'
            }}>
              <span style={{
                position: 'absolute',
                left: '75%',
                transform: 'translateX(-50%)'
              }}>75%</span>
              <span style={{
                position: 'absolute',
                left: '95%',
                transform: 'translateX(-50%)'
              }}>25%</span>
              <span style={{
                position: 'absolute',
                left: '100%',
                transform: 'translateX(-50%)'
              }}>5%</span>
            </div>
          </div>

          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
            marginTop: '2rem'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                backgroundColor: '#10B981',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <List size={20} color="white" />
              </div>
              <div>
                <strong style={{
                  display: 'block',
                  fontSize: '1rem',
                  color: '#111827'
                }}>75%</strong>
                <span style={{
                  fontSize: '0.875rem',
                  color: '#6B7280'
                }}>Chỗ đặt được sử dụng</span>
              </div>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                backgroundColor: '#FACC15',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <Briefcase size={20} color="white" />
              </div>
              <div>
                <strong style={{
                  display: 'block',
                  fontSize: '1rem',
                  color: '#111827'
                }}>16</strong>
                <span style={{
                  fontSize: '0.875rem',
                  color: '#6B7280'
                }}>25% Hủy sau khi đặt chỗ</span>
              </div>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem'
            }}>
              <div style={{
                width: '48px',
                height: '48px',
                backgroundColor: '#EF4444',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0
              }}>
                <Briefcase size={20} color="white" />
              </div>
              <div>
                <strong style={{
                  display: 'block',
                  fontSize: '1rem',
                  color: '#111827'
                }}>16</strong>
                <span style={{
                  fontSize: '0.875rem',
                  color: '#6B7280'
                }}>5% Vi phạm</span>
              </div>
            </div>
          </div>
        </div>

        {/* Card 3: Khu vực sử dụng nhiều nhất */}
        <div style={{
          backgroundColor: 'white',
          borderRadius: '16px',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            marginBottom: '1.5rem'
          }}>
            <h3 style={{
              fontSize: '1.125rem',
              fontWeight: '600',
              color: '#111827',
              margin: 0
            }}>Khu vực được sử dụng nhiều nhất</h3>
          </div>
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1.25rem'
          }}>
            {ZONE_USAGE.map((zone, idx) => (
              <div key={idx}>
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  marginBottom: '0.5rem'
                }}>
                  <span style={{
                    fontSize: '0.875rem',
                    color: '#111827',
                    fontWeight: '500'
                  }}>{zone.name}</span>
                  <span style={{
                    fontSize: '0.875rem',
                    color: '#111827',
                    fontWeight: '600'
                  }}>{zone.percent}%</span>
                </div>
                <div style={{
                  width: '100%',
                  height: '8px',
                  backgroundColor: '#f3f4f6',
                  borderRadius: '4px',
                  overflow: 'hidden'
                }}>
                  <div style={{
                    width: `${zone.percent}%`,
                    height: '100%',
                    backgroundColor: zone.color,
                    borderRadius: '4px',
                    transition: 'width 0.3s'
                  }}></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Card 4: Phản hồi */}
        <div style={{
          backgroundColor: 'white',
          borderRadius: '16px',
          padding: '1.5rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1.5rem'
          }}>
            <h3 style={{
              fontSize: '1.125rem',
              fontWeight: '600',
              color: '#111827',
              margin: 0
            }}>Phản hồi của sinh viên</h3>
            <div style={{
              display: 'flex',
              gap: '0.5rem'
            }}>
              <button 
                onClick={handlePrevFeedback} 
                disabled={feedbackIndex === 0}
                style={{
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: '6px',
                  padding: '0.375rem',
                  cursor: feedbackIndex === 0 ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  opacity: feedbackIndex === 0 ? 0.5 : 1
                }}
              >
                <ChevronLeft size={16} />
              </button>
              <button 
                onClick={handleNextFeedback} 
                disabled={feedbackIndex >= FEEDBACKS.length - 2}
                style={{
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: '6px',
                  padding: '0.375rem',
                  cursor: feedbackIndex >= FEEDBACKS.length - 2 ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  opacity: feedbackIndex >= FEEDBACKS.length - 2 ? 0.5 : 1
                }}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
          
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem'
          }}>
            {visibleFeedbacks.map((fb) => (
              <div key={fb.id} style={{
                padding: '1rem',
                backgroundColor: '#f9fafb',
                borderRadius: '12px',
                border: '1px solid #e5e7eb'
              }}>
                <div style={{
                  fontSize: '0.75rem',
                  color: '#9CA3AF',
                  marginBottom: '0.5rem'
                }}>{fb.date}</div>
                <div style={{
                  fontSize: '0.875rem',
                  color: '#111827',
                  marginBottom: '0.75rem',
                  lineHeight: '1.5'
                }}>{fb.content}</div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem'
                }}>
                  <img src={fb.avatar} alt={fb.user} style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '50%',
                    objectFit: 'cover'
                  }} />
                  <div>
                    <div style={{
                      fontSize: '0.875rem',
                      fontWeight: '600',
                      color: '#111827'
                    }}>{fb.user}</div>
                    <div style={{
                      fontSize: '0.75rem',
                      color: '#6B7280'
                    }}>{fb.code}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>
      </div>
    </>
  );
};

export default Statistic;