const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('slibNfcBridge', {
    getStatus: () => ipcRenderer.invoke('bridge:get-status'),
    restart: () => ipcRenderer.invoke('bridge:restart'),
    openAdmin: () => ipcRenderer.invoke('bridge:open-admin'),
    onStatusUpdated: (callback) => {
        const listener = (event, payload) => callback(payload);
        ipcRenderer.on('bridge:status-updated', listener);
        return () => ipcRenderer.removeListener('bridge:status-updated', listener);
    },
});
