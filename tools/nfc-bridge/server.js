/**
 * SLIB NFC Bridge — ACR122U NFC Tag UID Reader
 * 
 * This tool runs on a computer connected to an ACR122U NFC reader.
 * It exposes a simple HTTP API for the admin frontend to scan NFC tags.
 * 
 * Endpoints:
 *   GET /scan-uid   — Wait for NFC tag, return UID
 *   GET /status     — Check reader status
 * 
 * Usage:
 *   npm install
 *   npm start
 */

const express = require('express');
const cors = require('cors');
const { NFC } = require('nfc-pcsc');

const app = express();
const PORT = process.env.PORT || 5050;

app.use(cors());
app.use(express.json());

// NFC reader state
let nfcReader = null;
let readerReady = false;
let lastError = null;

// Initialize NFC
const nfc = new NFC();

nfc.on('reader', (reader) => {
    console.log(`[NFC Bridge] Reader connected: ${reader.reader.name}`);
    nfcReader = reader;
    readerReady = true;
    lastError = null;

    reader.on('card', (card) => {
        console.log(`[NFC Bridge] Card detected — UID: ${card.uid}`);
        // Store the latest card UID for pending scan requests
        if (pendingScanResolve) {
            pendingScanResolve({ uid: card.uid.toUpperCase() });
            pendingScanResolve = null;
            pendingScanReject = null;
            clearTimeout(pendingScanTimeout);
        }
    });

    reader.on('error', (err) => {
        console.error(`[NFC Bridge] Reader error:`, err.message);
        lastError = err.message;
    });

    reader.on('end', () => {
        console.log('[NFC Bridge] Reader disconnected');
        nfcReader = null;
        readerReady = false;
    });
});

nfc.on('error', (err) => {
    console.error('[NFC Bridge] NFC error:', err.message);
    lastError = err.message;
});

// Pending scan promise
let pendingScanResolve = null;
let pendingScanReject = null;
let pendingScanTimeout = null;

/**
 * GET /scan-uid
 * Wait for NFC tag to be scanned, return UID.
 * Timeout: 30 seconds
 */
app.get('/scan-uid', async (req, res) => {
    if (!readerReady) {
        return res.status(503).json({
            error: 'NFC reader not connected',
            message: 'Vui lòng kết nối đầu đọc NFC (ACR122U)'
        });
    }

    // Cancel any existing pending scan
    if (pendingScanReject) {
        pendingScanReject(new Error('Cancelled'));
        clearTimeout(pendingScanTimeout);
    }

    try {
        const result = await new Promise((resolve, reject) => {
            pendingScanResolve = resolve;
            pendingScanReject = reject;

            // Timeout after 30 seconds
            pendingScanTimeout = setTimeout(() => {
                pendingScanResolve = null;
                pendingScanReject = null;
                reject(new Error('Timeout — không phát hiện thẻ NFC'));
            }, 30000);
        });

        res.json(result);
    } catch (err) {
        if (err.message === 'Cancelled') {
            return; // New request took over
        }
        res.status(408).json({
            error: 'timeout',
            message: err.message
        });
    }
});

/**
 * GET /status
 * Check NFC reader status
 */
app.get('/status', (req, res) => {
    res.json({
        readerConnected: readerReady,
        readerName: nfcReader?.reader?.name || null,
        lastError: lastError,
        port: PORT
    });
});

/**
 * GET /health
 * Health check
 */
app.get('/health', (req, res) => {
    res.json({ status: 'ok', service: 'slib-nfc-bridge' });
});

app.listen(PORT, () => {
    console.log(`\n🔌 SLIB NFC Bridge running on http://localhost:${PORT}`);
    console.log(`   Endpoints:`);
    console.log(`     GET /scan-uid  — Scan NFC tag UID`);
    console.log(`     GET /status    — Reader status`);
    console.log(`     GET /health    — Health check\n`);
    console.log(`   Waiting for ACR122U NFC reader...\n`);
});
