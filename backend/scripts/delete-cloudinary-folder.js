/**
 * delete-cloudinary-folder.js
 * 
 * Script xóa toàn bộ ảnh trong một folder Cloudinary sử dụng Admin API.
 * Xử lý batch + async, có retry và log tiến trình chi tiết.
 * 
 * Cách chạy:
 *   node delete-cloudinary-folder.js
 * 
 * Hoặc với biến môi trường:
 *   FOLDER_PREFIX=users/avatars/ node delete-cloudinary-folder.js
 */

const cloudinary = require('cloudinary').v2;

// ========================================
// CẤU HÌNH - Thay đổi theo môi trường
// ========================================
const CONFIG = {
    // Cloudinary credentials (lấy từ Dashboard)
    CLOUDINARY_CLOUD_NAME: process.env.CLOUDINARY_CLOUD_NAME || 'dsupnjcqy',
    CLOUDINARY_API_KEY: process.env.CLOUDINARY_API_KEY || '356659581747979',
    CLOUDINARY_API_SECRET: process.env.CLOUDINARY_API_SECRET || 'HlvOnlX7oJNDnoGEmWm-Y6_EPlQ',

    // Folder prefix cần xóa (VD: 'slib_avatars/')
    FOLDER_PREFIX: process.env.FOLDER_PREFIX || 'slib_avatars/',

    // Số ảnh lấy mỗi lần từ API (max 500)
    FETCH_BATCH_SIZE: parseInt(process.env.FETCH_BATCH_SIZE) || 500,

    // So anh xoa moi batch (giam xuong 100 de tranh rate limit)
    DELETE_BATCH_SIZE: parseInt(process.env.DELETE_BATCH_SIZE) || 100,

    // Số lần retry tối đa khi xóa thất bại
    MAX_RETRIES: parseInt(process.env.MAX_RETRIES) || 3,

    // Delay giua cac batch (ms) - tang len de tranh rate limit
    DELAY_BETWEEN_BATCHES: parseInt(process.env.DELAY_BETWEEN_BATCHES) || 2000,

    // Delay sau mỗi lần retry (ms)
    RETRY_DELAY: parseInt(process.env.RETRY_DELAY) || 2000,
};

// ========================================
// KHỞI TẠO CLOUDINARY
// ========================================
cloudinary.config({
    cloud_name: CONFIG.CLOUDINARY_CLOUD_NAME,
    api_key: CONFIG.CLOUDINARY_API_KEY,
    api_secret: CONFIG.CLOUDINARY_API_SECRET,
});

// ========================================
// UTILITY FUNCTIONS
// ========================================

/**
 * Delay helper - Promise-based sleep
 */
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Log với timestamp
 */
function log(message, type = 'INFO') {
    const timestamp = new Date().toISOString();
    const prefix = {
        'INFO': '[ INFO]',
        'WARN': '[ WARN]',
        'ERROR': '[ERROR]',
        'SUCCESS': '[  OK ]',
        'PROGRESS': '[PROG ]',
    }[type] || '[INFO]';

    console.log(`${timestamp} ${prefix} ${message}`);
}

/**
 * Chia array thành các batch nhỏ
 */
function chunkArray(array, chunkSize) {
    const chunks = [];
    for (let i = 0; i < array.length; i += chunkSize) {
        chunks.push(array.slice(i, i + chunkSize));
    }
    return chunks;
}

// ========================================
// STEP 1: LẤY DANH SÁCH ẢNH THEO PREFIX
// ========================================

/**
 * Lấy tất cả public_id của ảnh trong folder (xử lý pagination)
 * @param {string} prefix - Folder prefix (VD: 'users/avatars/')
 * @returns {Promise<string[]>} - Array các public_id
 */
async function fetchAllResourcesByPrefix(prefix) {
    log(`Bắt đầu lấy danh sách ảnh với prefix: "${prefix}"`);

    const allPublicIds = [];
    let nextCursor = null;
    let pageCount = 0;

    do {
        pageCount++;
        log(`Đang lấy trang ${pageCount}...`, 'PROGRESS');

        try {
            const options = {
                type: 'upload',
                prefix: prefix,
                max_results: CONFIG.FETCH_BATCH_SIZE,
            };

            // Thêm cursor nếu có (pagination)
            if (nextCursor) {
                options.next_cursor = nextCursor;
            }

            const result = await cloudinary.api.resources(options);

            // Lấy public_id từ resources
            const publicIds = result.resources.map(r => r.public_id);
            allPublicIds.push(...publicIds);

            log(`Trang ${pageCount}: Lấy được ${publicIds.length} ảnh (Tổng: ${allPublicIds.length})`, 'PROGRESS');

            // Cập nhật cursor cho trang tiếp theo
            nextCursor = result.next_cursor;

            // Delay nhỏ giữa các request để tránh rate limit
            if (nextCursor) {
                await delay(500);
            }

        } catch (error) {
            log(`Lỗi khi lấy danh sách ảnh trang ${pageCount}: ${error.message}`, 'ERROR');
            throw error;
        }

    } while (nextCursor);

    log(`Hoàn tất! Tổng số ảnh cần xóa: ${allPublicIds.length}`, 'SUCCESS');
    return allPublicIds;
}

// ========================================
// STEP 2 & 3: XÓA ẢNH THEO BATCH
// ========================================

/**
 * Xóa một batch ảnh với retry
 * @param {string[]} publicIds - Array public_id cần xóa
 * @param {number} batchIndex - Số thứ tự batch (để log)
 * @param {number} totalBatches - Tổng số batch (để log)
 * @returns {Promise<{deleted: number, failed: string[]}>}
 */
async function deleteBatchWithRetry(publicIds, batchIndex, totalBatches) {
    let lastError = null;

    for (let attempt = 1; attempt <= CONFIG.MAX_RETRIES; attempt++) {
        try {
            log(`Batch ${batchIndex}/${totalBatches}: Xóa ${publicIds.length} ảnh (lần thử ${attempt})...`, 'PROGRESS');

            const result = await cloudinary.api.delete_resources(publicIds, {
                type: 'upload',
                resource_type: 'image',
            });

            // Đếm số ảnh đã xóa thành công
            const deletedCount = Object.values(result.deleted).filter(status => status === 'deleted').length;
            const notFoundCount = Object.values(result.deleted).filter(status => status === 'not_found').length;
            const failedPublicIds = Object.entries(result.deleted)
                .filter(([_, status]) => status !== 'deleted' && status !== 'not_found')
                .map(([id, _]) => id);

            if (notFoundCount > 0) {
                log(`Batch ${batchIndex}/${totalBatches}: ${notFoundCount} ảnh không tồn tại (đã bị xóa trước đó)`, 'WARN');
            }

            log(`Batch ${batchIndex}/${totalBatches}: Xóa thành công ${deletedCount}/${publicIds.length} ảnh`, 'SUCCESS');

            return {
                deleted: deletedCount + notFoundCount, // Coi not_found là thành công
                failed: failedPublicIds,
            };

        } catch (error) {
            lastError = error;
            log(`Batch ${batchIndex}/${totalBatches}: Lỗi lần thử ${attempt}: ${error.message}`, 'ERROR');

            if (attempt < CONFIG.MAX_RETRIES) {
                log(`Đợi ${CONFIG.RETRY_DELAY}ms trước khi thử lại...`, 'WARN');
                await delay(CONFIG.RETRY_DELAY);
            }
        }
    }

    // Hết số lần retry
    log(`Batch ${batchIndex}/${totalBatches}: THẤT BẠI sau ${CONFIG.MAX_RETRIES} lần thử`, 'ERROR');
    return {
        deleted: 0,
        failed: publicIds,
        error: lastError?.message,
    };
}

/**
 * Xóa tất cả ảnh theo batch
 * @param {string[]} allPublicIds - Tất cả public_id cần xóa
 * @returns {Promise<{totalDeleted: number, totalFailed: number, failedIds: string[]}>}
 */
async function deleteAllByBatches(allPublicIds) {
    // Chia thành các batch
    const batches = chunkArray(allPublicIds, CONFIG.DELETE_BATCH_SIZE);
    const totalBatches = batches.length;

    log(`Chia thành ${totalBatches} batch (mỗi batch ${CONFIG.DELETE_BATCH_SIZE} ảnh)`);

    let totalDeleted = 0;
    const allFailedIds = [];

    for (let i = 0; i < batches.length; i++) {
        const batch = batches[i];
        const batchIndex = i + 1;

        const result = await deleteBatchWithRetry(batch, batchIndex, totalBatches);

        totalDeleted += result.deleted;
        allFailedIds.push(...result.failed);

        // Delay giữa các batch để tránh rate limit
        if (i < batches.length - 1) {
            await delay(CONFIG.DELAY_BETWEEN_BATCHES);
        }
    }

    return {
        totalDeleted,
        totalFailed: allFailedIds.length,
        failedIds: allFailedIds,
    };
}

// ========================================
// STEP 4: XÓA FOLDER (nếu rỗng)
// ========================================

/**
 * Xóa folder rỗng (Cloudinary chỉ cho phép xóa folder rỗng)
 * @param {string} folderPath - Đường dẫn folder
 */
async function deleteEmptyFolder(folderPath) {
    try {
        // Bỏ trailing slash nếu có
        const cleanPath = folderPath.replace(/\/$/, '');

        log(`Đang xóa folder rỗng: "${cleanPath}"...`);

        await cloudinary.api.delete_folder(cleanPath);

        log(`Đã xóa folder: "${cleanPath}"`, 'SUCCESS');
    } catch (error) {
        if (error.message.includes('not empty')) {
            log(`Folder "${folderPath}" vẫn còn ảnh, không thể xóa`, 'WARN');
        } else if (error.message.includes('not found')) {
            log(`Folder "${folderPath}" không tồn tại hoặc đã được xóa`, 'WARN');
        } else {
            log(`Không thể xóa folder: ${error.message}`, 'ERROR');
        }
    }
}

// ========================================
// MAIN FUNCTION
// ========================================

async function main() {
    console.log('\n========================================');
    console.log('   CLOUDINARY FOLDER DELETION SCRIPT   ');
    console.log('========================================\n');

    log(`Cloud Name: ${CONFIG.CLOUDINARY_CLOUD_NAME}`);
    log(`Folder Prefix: ${CONFIG.FOLDER_PREFIX}`);
    log(`Delete Batch Size: ${CONFIG.DELETE_BATCH_SIZE}`);
    log(`Max Retries: ${CONFIG.MAX_RETRIES}`);
    console.log('');

    const startTime = Date.now();

    try {
        // Step 1: Lấy danh sách ảnh
        const allPublicIds = await fetchAllResourcesByPrefix(CONFIG.FOLDER_PREFIX);

        if (allPublicIds.length === 0) {
            log('Không có ảnh nào trong folder. Kiểm tra lại prefix.', 'WARN');

            // Vẫn thử xóa folder (có thể folder rỗng)
            await deleteEmptyFolder(CONFIG.FOLDER_PREFIX);
            return;
        }

        console.log('');

        // Step 2 & 3: Xóa theo batch
        const result = await deleteAllByBatches(allPublicIds);

        console.log('');

        // Step 4: Xóa folder rỗng
        if (result.totalFailed === 0) {
            await deleteEmptyFolder(CONFIG.FOLDER_PREFIX);
        }

        // Summary
        const duration = ((Date.now() - startTime) / 1000).toFixed(2);

        console.log('\n========================================');
        console.log('            KẾT QUẢ TỔNG HỢP           ');
        console.log('========================================');
        console.log(`Tổng số ảnh:        ${allPublicIds.length}`);
        console.log(`Đã xóa thành công:  ${result.totalDeleted}`);
        console.log(`Thất bại:           ${result.totalFailed}`);
        console.log(`Thời gian:          ${duration}s`);
        console.log('========================================\n');

        if (result.failedIds.length > 0) {
            log(`Danh sách ảnh xóa thất bại:`, 'ERROR');
            result.failedIds.forEach(id => console.log(`  - ${id}`));
            console.log('');
            log('Bạn có thể chạy lại script để thử xóa các ảnh này.', 'WARN');
        }

    } catch (error) {
        log(`Script thất bại: ${error.message}`, 'ERROR');
        console.error(error);
        process.exit(1);
    }
}

// ========================================
// RUN SCRIPT
// ========================================
main();
