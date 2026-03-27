const BLOCKED_TAGS = new Set([
  'script',
  'style',
  'object',
  'embed',
  'link',
  'meta',
  'base',
  'form',
  'input',
  'button',
]);

const SAFE_IFRAME_HOSTS = new Set([
  'www.youtube.com',
  'youtube.com',
  'www.youtube-nocookie.com',
  'player.vimeo.com',
]);

const isUnsafeUrl = (value) => {
  if (!value) return true;
  const normalized = value.trim().toLowerCase();
  return (
    normalized.startsWith('javascript:') ||
    normalized.startsWith('vbscript:') ||
    normalized.startsWith('data:text/html') ||
    normalized.startsWith('file:')
  );
};

const isSafeIframe = (value) => {
  if (!value || isUnsafeUrl(value)) return false;
  try {
    const { host } = new URL(value, window.location.origin);
    return SAFE_IFRAME_HOSTS.has(host.toLowerCase());
  } catch {
    return false;
  }
};

export const sanitizeHtml = (html) => {
  if (!html) return '';

  const parser = new DOMParser();
  const doc = parser.parseFromString(html, 'text/html');

  BLOCKED_TAGS.forEach((tag) => {
    doc.querySelectorAll(tag).forEach((node) => node.remove());
  });

  doc.querySelectorAll('*').forEach((element) => {
    [...element.attributes].forEach((attribute) => {
      const key = attribute.name.toLowerCase();
      const value = attribute.value;

      if (key.startsWith('on')) {
        element.removeAttribute(attribute.name);
        return;
      }

      if ((key === 'href' || key === 'src') && isUnsafeUrl(value)) {
        element.removeAttribute(attribute.name);
      }
    });

    if (element.tagName.toLowerCase() === 'iframe' && !isSafeIframe(element.getAttribute('src'))) {
      element.remove();
      return;
    }

    if (element.tagName.toLowerCase() === 'a') {
      element.setAttribute('rel', 'noopener noreferrer');
      element.setAttribute('target', '_blank');
    }
  });

  return doc.body.innerHTML;
};

export default sanitizeHtml;
