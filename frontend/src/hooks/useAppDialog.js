import { useConfirm } from '../components/common/ConfirmDialog';
import { useModal } from '../components/shared/ModalContext';

const normalizeOptions = (options, fallbackTitle) => {
    if (typeof options === 'string') {
        return {
            title: fallbackTitle,
            message: options,
        };
    }

    return {
        title: options?.title || fallbackTitle,
        message: options?.message || '',
        ...options,
    };
};

export const useAppDialog = () => {
    const { confirm } = useConfirm();
    const { showAlert } = useModal();

    return {
        confirm: (options) => confirm(normalizeOptions(options, 'Xác nhận')),
        alert: (options) => showAlert(
            typeof options === 'string' ? options : options?.message || '',
            normalizeOptions(options, 'Thông báo')
        ),
    };
};

export default useAppDialog;
