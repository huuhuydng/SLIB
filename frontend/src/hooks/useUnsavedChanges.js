import { useEffect } from 'react';

/**
 * Hook để cảnh báo người dùng khi có thay đổi chưa lưu
 * Hiển thị dialog xác nhận khi close/reload trang
 * @param {boolean} hasChanges - Có thay đổi chưa lưu hay không
 */
export function useUnsavedChanges(hasChanges) {
    useEffect(() => {
        const handler = (e) => {
            if (!hasChanges) return;

            // Cách chuẩn để trigger dialog trong modern browsers
            e.preventDefault();
            // Chrome requires returnValue to be set
            e.returnValue = '';
            // Return message (không được hiển thị trong modern browsers nhưng cần cho compatibility)
            return 'Bạn có thay đổi chưa lưu. Bạn có chắc muốn rời đi?';
        };

        window.addEventListener('beforeunload', handler);
        return () => window.removeEventListener('beforeunload', handler);
    }, [hasChanges]);
}

