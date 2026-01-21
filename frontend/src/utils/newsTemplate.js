// Template HTML cho news content
export const createNewsTemplate = (data) => {
  const { title, summary, content, categoryName, imageUrl } = data;
  
  return `
    <article class="news-article">
      <header class="news-header">
        <div class="news-meta">
          <span class="news-category">${categoryName || 'Triển lãm'}</span>
          <time class="news-date">${new Date().toLocaleDateString('vi-VN')}</time>
        </div>
        
        <h1 class="news-title">${title}</h1>
        
        ${summary ? `
          <p class="news-summary">${summary}</p>
        ` : ''}
      </header>
      
      <div class="news-content">
        ${content}
      </div>
      
      <style>
        .news-article {
          max-width: 900px;
          margin: 0 auto;
          padding: 2rem;
          background: white;
          border-radius: 12px;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        
        .news-meta {
          display: flex;
          gap: 1rem;
          align-items: center;
          margin-bottom: 1rem;
          color: #6b7280;
          font-size: 0.875rem;
        }
        
        .news-category {
          background: #dbeafe;
          color: #1e40af;
          padding: 0.25rem 0.75rem;
          border-radius: 4px;
          font-weight: 500;
        }
        
        .news-title {
          font-size: 2.5rem;
          font-weight: 700;
          color: #111827;
          margin-bottom: 1rem;
          line-height: 1.2;
        }
        
        .news-summary {
          font-size: 1.125rem;
          color: #4b5563;
          line-height: 1.6;
          margin-bottom: 2rem;
          padding-bottom: 2rem;
          border-bottom: 1px solid #e5e7eb;
        }
        
        .news-content {
          font-size: 1rem;
          line-height: 1.8;
          color: #374151;
        }
        
        .news-content p {
          margin-bottom: 1.5rem;
        }
        
        .news-content h2 {
          font-size: 1.5rem;
          font-weight: 600;
          margin-top: 2rem;
          margin-bottom: 1rem;
          color: #111827;
        }
        
        .news-content h3 {
          font-size: 1.25rem;
          font-weight: 600;
          margin-top: 1.5rem;
          margin-bottom: 0.75rem;
          color: #111827;
        }
        
        .news-content ul, .news-content ol {
          margin-bottom: 1.5rem;
          padding-left: 2rem;
        }
        
        .news-content li {
          margin-bottom: 0.5rem;
        }
        
        .news-content img {
          max-width: 100%;
          height: auto;
          border-radius: 8px;
          margin: 1.5rem 0;
        }
        
        .news-content blockquote {
          border-left: 4px solid #3b82f6;
          padding-left: 1rem;
          margin: 1.5rem 0;
          font-style: italic;
          color: #6b7280;
        }
        
        @media (max-width: 768px) {
          .news-article {
            padding: 1rem;
          }
          
          .news-title {
            font-size: 1.75rem;
          }
          
          .news-cover-image {
            height: 250px;
          }
        }
      </style>
    </article>
  `;
};
