import { AlertTriangle, RefreshCw } from 'lucide-react';
import './LoadErrorState.css';

function LoadErrorState({
  title = 'Không thể tải dữ liệu',
  message = 'Vui lòng thử lại sau.',
  onRetry,
  retryLabel = 'Thử lại',
  className = '',
  compact = false,
}) {
  return (
    <div className={`load-error-state${compact ? ' load-error-state--compact' : ''} ${className}`.trim()}>
      <div className="load-error-state__icon">
        <AlertTriangle size={compact ? 28 : 42} />
      </div>
      <div className="load-error-state__title">{title}</div>
      <div className="load-error-state__message">{message}</div>
      {onRetry && (
        <button type="button" className="load-error-state__retry" onClick={onRetry}>
          <RefreshCw size={16} />
          {retryLabel}
        </button>
      )}
    </div>
  );
}

export default LoadErrorState;
