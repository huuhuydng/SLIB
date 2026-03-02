import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { ArrowLeft, Pencil, Trash2, Eye, Calendar, Tag } from 'lucide-react';
import '../../../styles/librarian/NewsDetailView.css';
import { getNewsDetailForAdmin, getNewsImage, deleteNews, getAllNewsForAdmin } from '../../../services/newsService';

const NewsDetailView = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  const [newsData, setNewsData] = useState(null);
  const [relatedNews, setRelatedNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Detect base path from current URL (/librarian/news or /librarian/notification)
  const basePath = location.pathname.startsWith('/librarian/news')
    ? '/librarian/news'
    : '/librarian/notification';

  useEffect(() => {
    loadNewsDetail();
    loadRelatedNews();
  }, [id]);

  const loadNewsDetail = async () => {
    try {
      setLoading(true);
      const data = await getNewsDetailForAdmin(id);

      // Load image
      try {
        const imageUrl = await getNewsImage(id);
        data.imageUrl = imageUrl;
      } catch (error) {
        console.warn('Could not load image');
      }

      setNewsData(data);
    } catch (err) {
      setError('Không thể tải thông tin tin tức');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadRelatedNews = async () => {
    try {
      const allNews = await getAllNewsForAdmin();
      // Filter out current news and get latest 5
      const filtered = allNews
        .filter(news => news.id !== parseInt(id) && news.isPublished)
        .slice(0, 5);

      // Load images for related news
      const newsWithImages = await Promise.all(
        filtered.map(async (news) => {
          try {
            const imageUrl = await getNewsImage(news.id);
            return { ...news, imageUrl };
          } catch {
            return news;
          }
        })
      );

      setRelatedNews(newsWithImages);
    } catch (err) {
      console.error('Could not load related news:', err);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Bạn có chắc chắn muốn xóa tin tức này?')) return;

    try {
      await deleteNews(id);
      alert('Xóa tin tức thành công!');
      navigate(basePath);
    } catch (error) {
      alert('Lỗi khi xóa tin tức!');
      console.error(error);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const getCategoryName = (categoryId) => {
    const categories = {
      1: 'Sự kiện',
      2: 'Thông báo quan trọng',
      3: 'Sách mới',
      4: 'Ưu đãi'
    };
    return categories[categoryId] || 'Tin tức';
  };

  if (loading) {
    return (
      <div className="news-detail-loading">
        <div className="loading-spinner"></div>
        <p>Đang tải...</p>
      </div>
    );
  }

  if (error || !newsData) {
    return (
      <div className="news-detail-error">
        <p>{error || 'Không tìm thấy tin tức'}</p>
        <button onClick={() => navigate(basePath)} className="btn-back">
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div className="news-detail-wrapper">
      {/* Breadcrumb */}
      <div
        className="breadcrumb-section"
        style={{
          backgroundImage: newsData.imageUrl
            ? `url(${newsData.imageUrl})`
            : 'url(https://lib.tdtu.edu.vn/sites/tdt_lib/files/breadcrumb-tvvv.png)'
        }}
      >
        <div className="breadcrumb-content">
          <div className="crumb-badge">Tin tức & Sự kiện</div>
          <h1 className="page-title">{newsData.title}</h1>
          <div className="crumb-meta">
            <span className="chip ghost"><Calendar size={14} /> {formatDate(newsData.publishedAt || newsData.createdAt)}</span>
            <span className="chip ghost"><Eye size={14} /> {newsData.viewCount || 0} lượt xem</span>
            <span className="chip ghost"><Tag size={14} /> {getCategoryName(newsData.categoryId)}</span>
          </div>
          <nav className="breadcrumb-nav">
            <a href="/">Trang chủ</a>
            <span className="separator">/</span>
            <a href={basePath}>Tin tức</a>
            <span className="separator">/</span>
            <span className="current">{newsData.title}</span>
          </nav>
        </div>
      </div>

      {/* Main Content */}
      <div className="news-detail-container">
        <div className="news-detail-layout">
          {/* Left Column - Main Content */}
          <div className="news-main-content">
            <div className="news-cover">
              <img src={newsData.imageUrl || 'https://via.placeholder.com/1200x500'} alt={newsData.title} />
              <div className="cover-gradient" />
            </div>

            <div className="news-header">
              <div className="eyebrow">
                <span className="chip ghost">ID #{newsData.id}</span>
                <span className="chip ghost">{newsData.viewCount || 0} lượt xem</span>
                <span className="chip ghost">{formatDate(newsData.createdAt)}</span>
              </div>
              <h2 className="news-title">{newsData.title}</h2>
              <p className="news-subtitle">{newsData.summary || 'Không có mô tả ngắn'}</p>
            </div>

            <article className="news-article">
              <div
                className="news-content"
                dangerouslySetInnerHTML={{ __html: newsData.content }}
              />
            </article>

            <div className="news-actions-bar actions-bottom">
              <div className="actions-left">
                <button
                  onClick={() => navigate(basePath)}
                  className="btn-action btn-back"
                >
                  <ArrowLeft size={16} />
                  Quay lại
                </button>
              </div>
              <div className="actions-right">
                <button
                  onClick={() => navigate(`${basePath}/edit/${id}`)}
                  className="btn-action btn-edit"
                >
                  <Pencil size={16} />
                  Chỉnh sửa
                </button>
                <button
                  onClick={handleDelete}
                  className="btn-action btn-delete"
                >
                  <Trash2 size={16} />
                  Xóa
                </button>
              </div>
            </div>
          </div>

          {/* Right Sidebar */}
          <aside className="news-sidebar">
            {/* Related News */}
            <div className="sidebar-section related-news-section">
              <h3 className="sidebar-title">Tin tức</h3>
              <div className="related-news-list">
                {relatedNews.map((item) => (
                  <div
                    key={item.id}
                    className="related-news-item"
                    onClick={() => navigate(`${basePath}/view/${item.id}`)}
                  >
                    <div className="related-news-image">
                      <img
                        src={item.imageUrl || 'https://via.placeholder.com/150'}
                        alt={item.title}
                      />
                    </div>
                    <div className="related-news-content">
                      <h4 className="related-news-title">{item.title}</h4>
                      <p className="related-meta">{formatDate(item.publishedAt || item.createdAt)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="sidebar-section">
              <h3 className="sidebar-title">Thông tin bài viết</h3>

              <div className="info-card">
                <div className="info-row">
                  <span className="info-label">ID:</span>
                  <span className="info-value">{newsData.id}</span>
                </div>

                <div className="info-row">
                  <span className="info-label">Chủ đề:</span>
                  <span className="info-value">
                    {getCategoryName(newsData.categoryId)}
                  </span>
                </div>

                <div className="info-row">
                  <span className="info-label">Trạng thái:</span>
                  <span className="info-value">
                    {newsData.isPublished ? '✓ Đã đăng' : '📝 Nháp'}
                  </span>
                </div>

                <div className="info-row">
                  <span className="info-label">Ghim:</span>
                  <span className="info-value">
                    {newsData.isPinned ? 'Có' : 'Không'}
                  </span>
                </div>

                <div className="info-row">
                  <span className="info-label">Lượt xem:</span>
                  <span className="info-value">{newsData.viewCount || 0}</span>
                </div>

                <div className="info-row">
                  <span className="info-label">Ngày tạo:</span>
                  <span className="info-value">{formatDate(newsData.createdAt)}</span>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default NewsDetailView;
