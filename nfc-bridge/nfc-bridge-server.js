/**
 * SLIB NFC Bridge Server
 * 
 * Local server that bridges the ACR122U USB NFC reader with the Admin Portal.
 * Uses nfc-pcsc library to communicate with the reader hardware.
 * 
 * Usage:
 *   1. Connect ACR122U reader to PC via USB
 *   2. Run: npm start
 *   3. Admin Portal calls GET /scan-uid to initiate a scan
 *   4. Place NFC tag on reader within 15 seconds
 *   5. Server returns the tag's UID in uppercase HEX format
 */

const express = require('express');
const cors = require('cors');
const { NFC } = require('nfc-pcsc');

// Configuration
const PORT = process.env.PORT || 5050;
const SCAN_TIMEOUT_MS = 15000; // 15 seconds timeout for card detection

const app = express();

// Enable CORS for Admin Portal access
app.use(cors({
    origin: ['http://localhost:3000', 'http://localhost:5173', 'http://127.0.0.1:5173', 'https://slibsystem.site'],
    methods: ['GET', 'POST'],
    credentials: true
}));

app.use(express.json());

// NFC instance
const nfc = new NFC();

// Track current reader state
let currentReader = null;
let readerReady = false;

// NFC Reader event handlers
nfc.on('reader', (reader) => {
    console.log(`[NFC] Reader detected: ${reader.name}`);
    currentReader = reader;
    readerReady = true;

    reader.on('card', (card) => {
        // Card detected - this is handled in the scan endpoint
        console.log(`[NFC] Card detected - UID: ${card.uid}`);
    });

    reader.on('card.off', (card) => {
        console.log('[NFC] Card removed');
    });

    reader.on('error', (err) => {
        console.error(`[NFC] Reader error: ${err.message}`);
    });

    reader.on('end', () => {
        console.log(`[NFC] Reader disconnected: ${reader.name}`);
        if (currentReader === reader) {
            currentReader = null;
            readerReady = false;
        }
    });
});

nfc.on('error', (err) => {
    console.error('[NFC] NFC error:', err.message);
});

/**
 * Convert UID bytes to uppercase HEX string
 * @param {string|Buffer} uid - UID from nfc-pcsc (already hex string or buffer)
 * @returns {string} Uppercase HEX string without separators
 */
function formatUid(uid) {
    if (!uid) return null;

    // nfc-pcsc returns UID as uppercase hex string by default
    // But we ensure consistency by removing any separators and uppercasing
    if (typeof uid === 'string') {
        return uid.replace(/[:\-\s]/g, '').toUpperCase();
    }

    if (Buffer.isBuffer(uid)) {
        return uid.toString('hex').toUpperCase();
    }

    return null;
}

/**
 * Health check endpoint
 */
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        readerConnected: readerReady,
        readerName: currentReader?.name || null,
        timestamp: new Date().toISOString()
    });
});

/**
 * GET /scan-uid
 * 
 * Initiates an NFC tag scan. Wait for a card to be placed on the reader.
 * Returns the UID in uppercase HEX format.
 * 
 * Response:
 *   Success: { success: true, uid: "04A23C91" }
 *   Error: { success: false, error: "Error message" }
 */
app.get('/scan-uid', async (req, res) => {
    console.log('[API] Scan request received');

    // Check if reader is connected
    if (!readerReady || !currentReader) {
        console.log('[API] No reader connected');
        return res.status(503).json({
            success: false,
            error: 'Không tìm thấy đầu đọc NFC, hãy kiểm tra lại kết nối USB của ACR122U.',
            errorCode: 'READER_NOT_FOUND'
        });
    }

    try {
        // Wait for card detection with timeout
        const uid = await waitForCard(currentReader, SCAN_TIMEOUT_MS);

        if (uid) {
            const formattedUid = formatUid(uid);
            console.log(`[API] Scan successful - UID: ${formattedUid}`);

            return res.json({
                success: true,
                uid: formattedUid,
                message: 'Quét thẻ thành công!'
            });
        } else {
            return res.status(408).json({
                success: false,
                error: 'Không phát hiện thẻ NFC, vui lòng quét lại.',
                errorCode: 'NO_CARD_DETECTED'
            });
        }
    } catch (error) {
        console.error('[API] Scan error:', error.message);

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

/**
 * Wait for a card to be placed on the reader
 * @param {Object} reader - The nfc-pcsc reader instance
 * @param {number} timeoutMs - Timeout in milliseconds
 * @returns {Promise<string|null>} Card UID or null if timeout
 */
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

        // Listen for card event
        reader.on('card', cardHandler);

        // Set timeout
        timeoutHandle = setTimeout(() => {
            if (!cardDetected) {
                reader.removeListener('card', cardHandler);
                reject(new Error('TIMEOUT'));
            }
        }, timeoutMs);
    });
}

/**
 * GET /reader-status
 * 
 * Returns the current status of the NFC reader
 */
app.get('/reader-status', (req, res) => {
    res.json({
        connected: readerReady,
        readerName: currentReader?.name || null,
        message: readerReady
            ? 'Đầu đọc NFC đã sẵn sàng'
            : 'Chưa kết nối đầu đầu NFC'
    });
});

// Start server
app.listen(PORT, () => {
    console.log('========================================');
    console.log('  SLIB NFC Bridge Server');
    console.log('========================================');
    console.log(`  Server running on: http://localhost:${PORT}`);
    console.log(`  Scan timeout: ${SCAN_TIMEOUT_MS / 1000}s`);
    console.log('');
    console.log('  Endpoints:');
    console.log('    GET /health       - Health check');
    console.log('    GET /scan-uid     - Scan NFC tag UID');
    console.log('    GET /reader-status - Reader connection status');
    console.log('');
    console.log('  Waiting for ACR122U reader...');
    console.log('========================================');
});
