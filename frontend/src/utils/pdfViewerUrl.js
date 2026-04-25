import { API_BASE_URL } from '../config/apiConfig';

export const buildPdfViewerUrl = (pdfUrl, fileName = 'slib-news.pdf') => {
  if (!pdfUrl) return '';

  const params = new URLSearchParams({
    url: pdfUrl,
    fileName: fileName || 'slib-news.pdf',
  });

  return `${API_BASE_URL}/slib/files/proxy-pdf?${params.toString()}`;
};

export default buildPdfViewerUrl;
