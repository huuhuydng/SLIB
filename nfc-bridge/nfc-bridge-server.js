/**
 * CLI wrapper để giữ tương thích với cách chạy cũ: `npm start`.
 */
const { startBridgeServer } = require('./index');

startBridgeServer().catch((error) => {
    console.error('[Bridge] Không thể khởi động NFC Bridge:', error);
    process.exit(1);
});
