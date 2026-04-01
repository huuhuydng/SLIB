const path = require('path');
const { app, BrowserWindow, Tray, Menu, nativeImage, ipcMain, shell } = require('electron');
const { startBridgeServer } = require('slib-nfc-bridge');

const ADMIN_URL = 'https://slibsystem.site/admin/nfc-management';
const PROTOCOL_PREFIX = 'slib-nfc-bridge://';

let tray = null;
let statusWindow = null;
let bridgeRuntime = null;
let isQuitting = false;
let currentStatus = {
    online: false,
    readerConnected: false,
    readerName: null,
    bridgeUrl: 'http://127.0.0.1:5050',
};

function log(...args) {
    console.log('[SLIB NFC Bridge App]', ...args);
}

function buildTrayIcon() {
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">
            <rect x="6" y="6" width="52" height="52" rx="14" fill="#EA580C"/>
            <path d="M22 24c4-4 8-6 10-6s6 2 10 6m-16 6c3-3 5-4 6-4s3 1 6 4m-8 6c1-1 1.5-1.5 2-1.5s1 .5 2 1.5" stroke="#fff" stroke-width="4" stroke-linecap="round" fill="none"/>
            <circle cx="32" cy="44" r="3.5" fill="#fff"/>
        </svg>
    `;

    return nativeImage.createFromDataURL(`data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}`);
}

function getStatusLabel(status = currentStatus) {
    if (status.readerConnected) return 'Sẵn sàng';
    if (status.online) return 'Đang chạy';
    return 'Chưa khởi động';
}

function updateStatus(nextStatus) {
    currentStatus = {
        ...currentStatus,
        ...nextStatus,
    };
    refreshTray();
    if (statusWindow && !statusWindow.isDestroyed()) {
        statusWindow.webContents.send('bridge:status-updated', currentStatus);
    }
}

function refreshTray() {
    if (!tray) return;

    tray.setToolTip(`SLIB NFC Bridge - ${getStatusLabel()}`);
    tray.setContextMenu(Menu.buildFromTemplate([
        {
            label: `Trạng thái: ${getStatusLabel()}`,
            enabled: false,
        },
        {
            label: currentStatus.readerName
                ? `Đầu đọc: ${currentStatus.readerName}`
                : 'Chưa phát hiện đầu đọc',
            enabled: false,
        },
        { type: 'separator' },
        {
            label: 'Mở bảng trạng thái',
            click: () => showStatusWindow(),
        },
        {
            label: 'Mở trang quản lý NFC',
            click: () => shell.openExternal(ADMIN_URL),
        },
        {
            label: 'Khởi động lại Bridge',
            click: () => restartBridge(),
        },
        { type: 'separator' },
        {
            label: 'Thoát',
            click: () => {
                isQuitting = true;
                app.quit();
            },
        },
    ]));
}

function showStatusWindow() {
    if (statusWindow && !statusWindow.isDestroyed()) {
        statusWindow.show();
        statusWindow.focus();
        return;
    }

    statusWindow = new BrowserWindow({
        width: 420,
        height: 520,
        show: false,
        resizable: false,
        autoHideMenuBar: true,
        title: 'SLIB NFC Bridge',
        webPreferences: {
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
        },
    });

    statusWindow.loadFile(path.join(__dirname, 'status.html'));
    statusWindow.once('ready-to-show', () => statusWindow.show());
    statusWindow.on('close', (event) => {
        if (!isQuitting) {
            event.preventDefault();
            statusWindow.hide();
        }
    });
    statusWindow.on('closed', () => {
        statusWindow = null;
    });
}

async function startBridge() {
    bridgeRuntime = await startBridgeServer({
        onStatusChange: updateStatus,
        logger: {
            log,
            error: (...args) => console.error('[SLIB NFC Bridge App]', ...args),
        },
    });

    updateStatus(bridgeRuntime.getStatus());
}

async function restartBridge() {
    try {
        if (bridgeRuntime) {
            await bridgeRuntime.stop();
            bridgeRuntime = null;
        }
        await startBridge();
    } catch (error) {
        console.error('[SLIB NFC Bridge App] Restart failed:', error);
        updateStatus({
            online: false,
            readerConnected: false,
        });
    }
}

function handleProtocolUrl(url) {
    if (!url || !url.startsWith(PROTOCOL_PREFIX)) return;

    log('Protocol invoked:', url);
    showStatusWindow();
}

const gotLock = app.requestSingleInstanceLock();

if (!gotLock) {
    app.quit();
} else {
    app.on('second-instance', (event, commandLine) => {
        const protocolUrl = commandLine.find((value) => value.startsWith(PROTOCOL_PREFIX));
        handleProtocolUrl(protocolUrl);
    });
}

ipcMain.handle('bridge:get-status', async () => currentStatus);
ipcMain.handle('bridge:restart', async () => {
    await restartBridge();
    return currentStatus;
});
ipcMain.handle('bridge:open-admin', async () => {
    await shell.openExternal(ADMIN_URL);
    return true;
});

app.on('open-url', (event, url) => {
    event.preventDefault();
    handleProtocolUrl(url);
});

app.whenReady().then(async () => {
    app.setAppUserModelId('site.slib.nfcbridge');
    app.setLoginItemSettings({
        openAtLogin: true,
    });
    app.setAsDefaultProtocolClient('slib-nfc-bridge');

    if (process.platform === 'darwin' && app.dock) {
        app.dock.hide();
    }

    tray = new Tray(buildTrayIcon());
    tray.on('double-click', showStatusWindow);
    refreshTray();

    await startBridge();
    showStatusWindow();
});

app.on('window-all-closed', (event) => {
    event.preventDefault();
});

app.on('before-quit', async () => {
    isQuitting = true;
    if (bridgeRuntime) {
        try {
            await bridgeRuntime.stop();
        } catch (error) {
            console.error('[SLIB NFC Bridge App] Failed to stop bridge:', error);
        }
    }
});
