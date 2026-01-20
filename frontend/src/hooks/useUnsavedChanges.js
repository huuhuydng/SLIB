import { useEffect } from 'react';

/**
 * Hook để cảnh báo người dùng khi có thay đổi chưa lưu
 * @param {boolean} hasChanges - Có thay đổi chưa lưu hay không
 */
export function useUnsavedChanges(hasChanges) {
    useEffect(() => {
        const handler = (e) => {
            if (hasChanges) {
                e.preventDefault();
                e.returnValue = 'Bạn có thay đổi chưa lưu. Bạn có chắc muốn rời đi?';
                return e.returnValue;
            }
        };

        window.addEventListener('beforeunload', handler);
        return () => window.removeEventListener('beforeunload', handler);
    }, [hasChanges]);
}
