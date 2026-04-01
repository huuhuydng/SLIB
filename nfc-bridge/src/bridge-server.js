const express = require('express');
const cors = require('cors');
const { NFC } = require('nfc-pcsc');

const DEFAULT_ALLOWED_ORIGINS = [
    'http://localhost:3000',
    'http://localhost:5173',
    'http://127.0.0.1:5173',
    'https://slibsystem.site'
];

function createLogger(logger = console) {
    return {
        info: logger.info ? logger.info.bind(logger) : console.log.bind(console),
        warn: logger.warn ? logger.warn.bind(logger) : console.warn.bind(console),
        error: logger.error ? logger.error.bind(logger) : console.error.bind(console)
    };
}

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

function startBridgeServer(options = {}) {
    const {
        port = Number(process.env.PORT || 5050),
        scanTimeoutMs = 15000,
        allowedOrigins = DEFAULT_ALLOWED_ORIGINS,
        logger = console,
        onStatusChange
    } = options;

    const log = createLogger(logger);
    const bridgeApp = express();
    const nfc = new NFC();

    let server = null;
    let currentReader = null;
    let readerReady = false;
    let lastError = null;

    const state = {
        port,
        readerConnected: false,
        readerName: null,
        lastError: null,
        startedAt: null
    };

    const emitStatus = () => {
        const snapshot = {
            ...state,
            readerConnected: readerReady,
            readerName: currentReader?.name || null,
            lastError,
            url: `http://127.0.0.1:${port}`
        };

        if (typeof onStatusChange === 'function') {
            onStatusChange(snapshot);
        }

        return snapshot;
    };

    bridgeApp.use(cors({
        origin: allowedOrigins,
        methods: ['GET', 'POST'],
        credentials: true
    }));

    bridgeApp.use(express.json());

    nfc.on('reader', (reader) => {
        log.info(`[NFC] Reader detected: ${reader.name}`);
        currentReader = reader;
        readerReady = true;
        lastError = null;
        emitStatus();

        reader.on('card', (card) => {
            log.info(`[NFC] Card detected - UID: ${card.uid}`);
        });

        reader.on('card.off', () => {
            log.info('[NFC] Card removed');
        });

        reader.on('error', (err) => {
            lastError = err.message;
            log.error(`[NFC] Reader error: ${err.message}`);
            emitStatus();
        });

        reader.on('end', () => {
            log.warn(`[NFC] Reader disconnected: ${reader.name}`);
            if (currentReader === reader) {
                currentReader = null;
                readerReady = false;
                emitStatus();
            }
        });
    });

    nfc.on('error', (err) => {
        lastError = err.message;
        log.error(`[NFC] NFC error: ${err.message}`);
        emitStatus();
    });

    bridgeApp.get('/health', (req, res) => {
        res.json({
            status: 'ok',
            readerConnected: readerReady,
            readerName: currentReader?.name || null,
            timestamp: new Date().toISOString(),
            lastError
        });
    });

    bridgeApp.get('/reader-status', (req, res) => {
        res.json({
            connected: readerReady,
            readerName: currentReader?.name || null,
            message: readerReady
                ? 'Đầu đọc NFC đã sẵn sàng'
                : 'Chưa kết nối đầu đọc NFC',
            lastError
        });
    });

    bridgeApp.get('/scan-uid', async (req, res) => {
        log.info('[API] Scan request received');

        if (!readerReady || !currentReader) {
            return res.status(503).json({
                success: false,
                error: 'Không tìm thấy đầu đọc NFC, hãy kiểm tra lại kết nối USB của ACR122U.',
                errorCode: 'READER_NOT_FOUND'
            });
        }

        try {
            const uid = await waitForCard(currentReader, scanTimeoutMs);
            const formattedUid = formatUid(uid);

            log.info(`[API] Scan successful - UID: ${formattedUid}`);

            return res.json({
                success: true,
                uid: formattedUid,
                message: 'Quét thẻ thành công!'
            });
        } catch (error) {
            log.error(`[API] Scan error: ${error.message}`);

            if (error.message === 'TIMEOUT') {
                return res.status(408).json({
                    success: false,
                    error: 'Hết thời gian chờ, vui lòng quét lại.',
                    errorCode: 'TIMEOUT'
                });
            }

            return res.status(500).json({
                success: false,
                error: `Lỗi đọc thẻ: ${error.message}`,
                errorCode: 'READ_ERROR'
            });
        }
    });

    server = bridgeApp.listen(port, () => {
        state.startedAt = new Date().toISOString();
        log.info('========================================');
        log.info('  SLIB NFC Bridge Server');
        log.info('========================================');
        log.info(`  Server running on: http://127.0.0.1:${port}`);
        log.info(`  Scan timeout: ${scanTimeoutMs / 1000}s`);
        log.info('  Waiting for ACR122U reader...');
        log.info('========================================');
        emitStatus();
    });

    return {
        getStatus: () => emitStatus(),
        close: () => new Promise((resolve, reject) => {
            if (!server) {
                resolve();
                return;
            }

            try {
                nfc.removeAllListeners();
                if (currentReader) {
                    currentReader.removeAllListeners();
                }
            } catch (error) {
                log.warn(`[NFC] Cleanup warning: ${error.message}`);
            }

            server.close((error) => {
                if (error) {
                    reject(error);
                    return;
                }

                server = null;
                currentReader = null;
                readerReady = false;
                lastError = null;
                resolve();
            });
        })
    };
}

module.exports = {
    startBridgeServer,
    formatUid,
    DEFAULT_ALLOWED_ORIGINS
};
