const EventEmitter = require('events');
const express = require('express');
const cors = require('cors');
const { NFC } = require('nfc-pcsc');

const DEFAULT_PORT = Number(process.env.PORT || 5050);
const DEFAULT_SCAN_TIMEOUT_MS = 15000;
const DEFAULT_ALLOWED_ORIGINS = [
    'http://localhost:3000',
    'http://localhost:5173',
    'http://127.0.0.1:5173',
    'https://slibsystem.site',
];

function formatUid(uid) {
    if (!uid) return null;

    if (typeof uid === 'string') {
        return uid.replace(/[:\-\s]/g, '').toUpperCase();
    }

    if (Buffer.isBuffer(uid)) {
        return uid.toString('hex').toUpperCase();
    }

    return null;
}

function createBridgeServer(options = {}) {
    const logger = options.logger || console;
    const port = Number(options.port || DEFAULT_PORT);
    const scanTimeoutMs = Number(options.scanTimeoutMs || DEFAULT_SCAN_TIMEOUT_MS);
    const allowedOrigins = options.allowedOrigins || DEFAULT_ALLOWED_ORIGINS;
    const events = new EventEmitter();

    const app = express();
    const nfc = new NFC();

    let server = null;
    let currentReader = null;
    let readerReady = false;

    const getStatus = () => ({
        status: 'ok',
        online: Boolean(server),
        port,
        readerConnected: readerReady,
        readerName: currentReader?.name || null,
        bridgeUrl: `http://127.0.0.1:${port}`,
        timestamp: new Date().toISOString(),
    });

    const emitStatus = () => {
        const status = getStatus();
        events.emit('status', status);
        if (typeof options.onStatusChange === 'function') {
            options.onStatusChange(status);
        }
        return status;
    };

    app.use(cors({
        origin: allowedOrigins,
        methods: ['GET', 'POST'],
        credentials: true,
    }));

    app.use(express.json());

    nfc.on('reader', (reader) => {
        logger.log(`[NFC] Reader detected: ${reader.name}`);
        currentReader = reader;
        readerReady = true;
        emitStatus();

        reader.on('card', (card) => {
            logger.log(`[NFC] Card detected - UID: ${card.uid}`);
        });

        reader.on('card.off', () => {
            logger.log('[NFC] Card removed');
        });

        reader.on('error', (error) => {
            logger.error(`[NFC] Reader error: ${error.message}`);
            events.emit('reader-error', error);
        });

        reader.on('end', () => {
            logger.log(`[NFC] Reader disconnected: ${reader.name}`);
            if (currentReader === reader) {
                currentReader = null;
                readerReady = false;
                emitStatus();
            }
        });
    });

    nfc.on('error', (error) => {
        logger.error('[NFC] NFC error:', error.message);
        events.emit('nfc-error', error);
    });

    app.get('/health', (req, res) => {
        res.json(getStatus());
    });

    app.get('/scan-uid', async (req, res) => {
        logger.log('[API] Scan request received');

        if (!readerReady || !currentReader) {
            logger.log('[API] No reader connected');
            return res.status(503).json({
                success: false,
                error: 'Không tìm thấy đầu đọc NFC, hãy kiểm tra lại kết nối USB của ACR122U.',
                errorCode: 'READER_NOT_FOUND',
            });
        }

        try {
            const uid = await waitForCard(currentReader, scanTimeoutMs);

            if (uid) {
                const formattedUid = formatUid(uid);
                logger.log(`[API] Scan successful - UID: ${formattedUid}`);

                return res.json({
                    success: true,
                    uid: formattedUid,
                    message: 'Quét thẻ thành công!',
                });
            }

            return res.status(408).json({
                success: false,
                error: 'Không phát hiện thẻ NFC, vui lòng quét lại.',
                errorCode: 'NO_CARD_DETECTED',
            });
        } catch (error) {
            logger.error('[API] Scan error:', error.message);

            if (error.message === 'TIMEOUT') {
                return res.status(408).json({
                    success: false,
                    error: 'Hết thời gian chờ, vui lòng quét lại.',
                    errorCode: 'TIMEOUT',
                });
            }

            return res.status(500).json({
                success: false,
                error: `Lỗi đọc thẻ: ${error.message}`,
                errorCode: 'READ_ERROR',
            });
        }
    });

    app.get('/reader-status', (req, res) => {
        res.json({
            connected: readerReady,
            readerName: currentReader?.name || null,
            message: readerReady
                ? 'Đầu đọc NFC đã sẵn sàng'
                : 'Chưa kết nối đầu đọc NFC',
        });
    });

    const start = () => new Promise((resolve) => {
        server = app.listen(port, () => {
            logger.log('========================================');
            logger.log('  SLIB NFC Bridge Server');
            logger.log('========================================');
            logger.log(`  Server running on: http://localhost:${port}`);
            logger.log(`  Scan timeout: ${scanTimeoutMs / 1000}s`);
            logger.log('');
            logger.log('  Endpoints:');
            logger.log('    GET /health        - Health check');
            logger.log('    GET /scan-uid      - Scan NFC tag UID');
            logger.log('    GET /reader-status - Reader connection status');
            logger.log('');
            logger.log('  Waiting for ACR122U reader...');
            logger.log('========================================');
            emitStatus();
            resolve(api);
        });
    });

    const stop = () => new Promise((resolve, reject) => {
        const finish = () => {
            server = null;
            currentReader = null;
            readerReady = false;
            emitStatus();
            resolve();
        };

        if (typeof nfc.close === 'function') {
            try {
                nfc.close();
            } catch (error) {
                logger.error('[NFC] Failed to close NFC instance:', error.message);
            }
        }

        if (!server) {
            finish();
            return;
        }

        server.close((error) => {
            if (error) {
                reject(error);
                return;
            }
            finish();
        });
    });

    const api = {
        app,
        events,
        getStatus,
        start,
        stop,
    };

    return api;
}

async function startBridgeServer(options = {}) {
    const bridge = createBridgeServer(options);
    await bridge.start();
    return bridge;
}

function waitForCard(reader, timeoutMs) {
    return new Promise((resolve, reject) => {
        let cardDetected = false;
        let timeoutHandle = null;

        const cardHandler = (card) => {
            if (!cardDetected) {
                cardDetected = true;
                clearTimeout(timeoutHandle);
                reader.removeListener('card', cardHandler);
                resolve(card.uid);
            }
        };

        reader.on('card', cardHandler);

        timeoutHandle = setTimeout(() => {
            if (!cardDetected) {
                reader.removeListener('card', cardHandler);
                reject(new Error('TIMEOUT'));
            }
        }, timeoutMs);
    });
}

module.exports = {
    createBridgeServer,
    startBridgeServer,
    formatUid,
};
