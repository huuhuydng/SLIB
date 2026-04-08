import React, { useState, useEffect, useCallback, useRef, useMemo } from "react";
import {
  Nfc, RefreshCw, CheckCircle, XCircle, Plus, Trash2, Wifi, Download,
  ZoomIn, ZoomOut, Maximize2, Info, X, ChevronDown
} from "lucide-react";
import { useConfirm } from "../../../components/common/ConfirmDialog";
import { getAreas, getZonesByArea, getSeats, getAreaFactoriesByArea, getSeatByNfcUid } from "../../../services/admin/area_management/api";
import { calculateDynamicSeatLayout } from "../../../utils/admin/seatLayout";
import nfcManagementService from "../../../services/admin/nfcManagementService";
import { NFC_BRIDGE_URL } from "../../../config/apiConfig";
import LoadErrorState from "../../../components/common/LoadErrorState";
import "./NfcManagement.css";

const NfcManagement = () => {
  const { confirm } = useConfirm();

  // ===== DATA STATE =====
  const [areas, setAreas] = useState([]);
  const [zones, setZones] = useState([]);
  const [seats, setSeats] = useState([]);
  const [factories, setFactories] = useState([]);
  const [nfcMap, setNfcMap] = useState({}); // seatId -> nfc mapping data
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  // ===== UI STATE =====
  const [selectedAreaId, setSelectedAreaId] = useState(null);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [seatNfcInfo, setSeatNfcInfo] = useState(null);
  const [infoLoading, setInfoLoading] = useState(false);
  const [scanLoading, setScanLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [bridgeStatus, setBridgeStatus] = useState({
    online: false,
    readerConnected: false,
    status: "checking",
    bridgeUrl: NFC_BRIDGE_URL,
    readerName: null,
    message: "Đang kiểm tra NFC Bridge..."
  });
  const [bridgeChecking, setBridgeChecking] = useState(false);
  const [bridgeLaunching, setBridgeLaunching] = useState(false);
  const [bridgeDownloading, setBridgeDownloading] = useState(false);
  const [showBridgeGuide, setShowBridgeGuide] = useState(false);
  const [showBridgeDropdown, setShowBridgeDropdown] = useState(false);
  const bridgeDropdownRef = useRef(null);

  // ===== NFC CHECK STATE =====
  const [showNfcCheckModal, setShowNfcCheckModal] = useState(false);
  const [nfcCheckScanning, setNfcCheckScanning] = useState(false);
  const [nfcCheckResult, setNfcCheckResult] = useState(null);
  const [nfcCheckError, setNfcCheckError] = useState(null);

  // ===== CANVAS STATE =====
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });
  const boardRef = useRef(null);
  const didAutoFit = useRef(false);

  const showToast = (message, type = "success") => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const checkBridgeConnection = useCallback(async (notify = false) => {
    setBridgeChecking(true);
    try {
      const status = await nfcManagementService.checkBridgeConnection();
      setBridgeStatus(status);

      if (notify) {
        showToast(
          status.message,
          status.online && status.readerConnected ? "success" : "error"
        );
      }
    } finally {
      setBridgeChecking(false);
    }
  }, []);

  const handleOpenBridgeApp = useCallback(() => {
    nfcManagementService.openBridgeApp();
    setBridgeLaunching(true);
    showToast("Đã gửi yêu cầu mở công cụ NFC trên máy này.");
    setShowBridgeDropdown(false);

    window.setTimeout(() => checkBridgeConnection(false), 1500);
    window.setTimeout(() => checkBridgeConnection(false), 4000);
    window.setTimeout(() => setBridgeLaunching(false), 4500);
  }, [checkBridgeConnection]);

  const triggerDownload = useCallback((url) => {
    if (typeof window === "undefined") return;
    const link = document.createElement("a");
    link.href = url;
    link.download = "";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, []);

  // ===== LOAD ALL DATA =====
  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      // 1. Load areas
      const areasRes = await getAreas();
      const raw = Array.isArray(areasRes?.data) ? areasRes.data : [];
      const areasNorm = raw.map(a => ({
        areaId: a.area_id ?? a.areaId,
        areaName: a.area_name ?? a.areaName,
        positionX: a.position_x ?? a.positionX ?? 0,
        positionY: a.position_y ?? a.positionY ?? 0,
        width: a.width ?? 300,
        height: a.height ?? 250,
      }));
      setAreas(areasNorm);

      if (areasNorm.length > 0) {
        setSelectedAreaId(prev => prev || areasNorm[0].areaId);
      }

      // 2. Load zones & factories for all areas
      const allZones = [];
      const allFactories = [];
      for (const area of areasNorm) {
        try {
          const zRes = await getZonesByArea(area.areaId);
          const zRaw = Array.isArray(zRes?.data) ? zRes.data : [];
          zRaw.forEach(z => allZones.push({
            zoneId: z.zone_id ?? z.zoneId,
            zoneName: z.zone_name ?? z.zoneName,
            areaId: z.area_id ?? z.areaId ?? area.areaId,
            positionX: z.position_x ?? z.positionX ?? 0,
            positionY: z.position_y ?? z.positionY ?? 0,
            width: z.width ?? 120,
            height: z.height ?? 100,
            color: z.color ?? '#d1f7d8',
          }));
        } catch (e) { console.error("Load zones failed", e); }

        try {
          const fRes = await getAreaFactoriesByArea(area.areaId);
          (fRes.data || []).forEach(f => allFactories.push({
            factoryId: f.factory_id ?? f.factoryId,
            factoryName: f.factory_name ?? f.factoryName,
            areaId: f.area_id ?? f.areaId,
            positionX: f.position_x ?? f.positionX ?? 0,
            positionY: f.position_y ?? f.positionY ?? 0,
            width: f.width ?? 120,
            height: f.height ?? 80,
            color: f.color ?? "#9CA3AF",
          }));
        } catch (e) { console.error("Load factories failed", e); }
      }
      setZones(allZones);
      setFactories(allFactories);

      // 3. Load seats for all zones
      const allSeats = [];
      for (const zone of allZones) {
        try {
          const sRes = await getSeats(zone.zoneId);
          (sRes.data || []).forEach(s => allSeats.push({
            ...s,
            positionY: s.positionY !== null ? s.positionY : 0,
          }));
        } catch (e) { console.error("Load seats failed", e); }
      }
      setSeats(allSeats);

      // 4. Load NFC mappings
      try {
        const mappings = await nfcManagementService.getNfcMappings();
        const map = {};
        (mappings || []).forEach(m => {
          map[m.seatId] = {
            ...m,
            nfcUpdatedAt: m.updatedAt ?? m.nfcUpdatedAt ?? null,
            lastUpdated: m.updatedAt ?? m.lastUpdated ?? m.nfcUpdatedAt ?? null,
          };
        });
        setNfcMap(map);
      } catch (e) { console.error("Load NFC mappings failed", e); }

    } catch (e) {
      const message = e?.message || "Lỗi tải dữ liệu";
      setLoadError(message);
      showToast(message, "error");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);
  useEffect(() => { checkBridgeConnection(false); }, [checkBridgeConnection]);

  // ===== STATS =====
  const stats = useMemo(() => {
    const total = seats.length;
    const mapped = seats.filter(s => nfcMap[s.seatId]?.hasNfcTag).length;
    return { total, mapped, unmapped: total - mapped };
  }, [seats, nfcMap]);

  // ===== ZOOM & PAN =====
  const handleWheel = useCallback((e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.05 : 0.05;
    setZoom(prev => Math.min(Math.max(prev + delta, 0.15), 3));
  }, []);

  const handleMouseDown = (e) => {
    if (e.button !== 0) return;
    setIsPanning(true);
    setPanStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
  };

  const handleMouseMove = useCallback((e) => {
    if (!isPanning) return;
    setPan({ x: e.clientX - panStart.x, y: e.clientY - panStart.y });
  }, [isPanning, panStart]);

  const handleMouseUp = () => setIsPanning(false);

  useEffect(() => {
    if (!isPanning) return;
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [isPanning, handleMouseMove]);

  // ===== FIT TO VIEW =====
  const handleFitToView = useCallback(() => {
    if (!areas.length || !boardRef.current) return;
    const rect = boardRef.current.getBoundingClientRect();
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    areas.forEach(a => {
      minX = Math.min(minX, a.positionX || 0);
      minY = Math.min(minY, a.positionY || 0);
      maxX = Math.max(maxX, (a.positionX || 0) + (a.width || 300));
      maxY = Math.max(maxY, (a.positionY || 0) + (a.height || 250));
    });
    const cW = maxX - minX + 100, cH = maxY - minY + 100;
    const scale = Math.min(rect.width / cW, rect.height / cH, 1);
    setZoom(scale);
    setPan({
      x: rect.width / 2 - ((minX + maxX) / 2) * scale,
      y: rect.height / 2 - ((minY + maxY) / 2) * scale,
    });
  }, [areas]);

  // Auto fit on first load
  useEffect(() => {
    if (!didAutoFit.current && !loading && areas.length > 0 && boardRef.current) {
      requestAnimationFrame(() => {
        handleFitToView();
        didAutoFit.current = true;
      });
    }
  }, [areas, loading, handleFitToView]);

  // Close bridge dropdown on outside click
  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (bridgeDropdownRef.current && !bridgeDropdownRef.current.contains(e.target)) {
        setShowBridgeDropdown(false);
      }
    };
    if (showBridgeDropdown) {
      document.addEventListener("mousedown", handleOutsideClick);
    }
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [showBridgeDropdown]);

  // ===== NFC ACTIONS =====
  const handleViewInfo = async (seat) => {
    setSelectedSeat(seat);
    setInfoLoading(true);
    try {
      const info = await nfcManagementService.getNfcInfo(seat.seatId);
      setSeatNfcInfo(info);
    } catch (e) {
      // Build info from what we have
      const mapping = nfcMap[seat.seatId];
      setSeatNfcInfo({
        seatId: seat.seatId,
        seatCode: seat.seatCode,
        areaName: mapping?.areaName || "—",
        zoneName: mapping?.zoneName || "—",
        nfcMapped: mapping?.hasNfcTag || false,
        nfcUidMasked: mapping?.maskedNfcUid || null,
        updatedAt: mapping?.updatedAt || mapping?.nfcUpdatedAt || null,
        lastUpdated: mapping?.lastUpdated || mapping?.updatedAt || mapping?.nfcUpdatedAt || null,
      });
    } finally {
      setInfoLoading(false);
    }
  };

  const handleScanAndAssign = async (seatId) => {
    setScanLoading(true);
    try {
      const scanResult = await nfcManagementService.scanNfcFromBridge();
      if (scanResult.uid) {
        setActionLoading(true);
        await nfcManagementService.assignNfcUid(seatId, scanResult.uid);
        showToast("Đã gán NFC UID thành công");
        // Refresh NFC data
        const mappings = await nfcManagementService.getNfcMappings();
        const map = {};
        (mappings || []).forEach(m => { map[m.seatId] = m; });
        setNfcMap(map);
        // Refresh detail
        if (selectedSeat?.seatId === seatId) {
          handleViewInfo(selectedSeat);
        }
      }
    } catch (e) {
      showToast(e.message || "Lỗi quét/gán NFC", "error");
    } finally {
      setScanLoading(false);
      setActionLoading(false);
    }
  };

  const handleClearNfc = async (seatId) => {
    const confirmed = await confirm({
      title: 'Xóa NFC UID',
      message: 'Bạn có chắc muốn xóa NFC UID khỏi ghế này?',
      variant: 'danger',
      confirmText: 'Xoá',
    });
    if (!confirmed) return;
    setActionLoading(true);
    try {
      await nfcManagementService.clearNfcUid(seatId);
      showToast("Đã xóa NFC UID thành công");
      const mappings = await nfcManagementService.getNfcMappings();
      const map = {};
      (mappings || []).forEach(m => { map[m.seatId] = m; });
      setNfcMap(map);
      if (selectedSeat?.seatId === seatId) {
        handleViewInfo(selectedSeat);
      }
    } catch (e) {
      showToast(e.message || "Lỗi xóa NFC", "error");
    } finally {
      setActionLoading(false);
    }
  };

  // ===== FILTERED DATA =====
  const currentArea = areas.find(a => a.areaId === selectedAreaId);
  const areaZones = zones.filter(z => z.areaId === selectedAreaId);
  const areaFactories = factories.filter(f => f.areaId === selectedAreaId);

  const bridgeInstallerUrl = "/downloads/slib-nfc-bridge-app/SLIB-NFC-Bridge-Setup.exe";
  const bridgeMacInstallerUrl = "/downloads/slib-nfc-bridge-app/SLIB-NFC-Bridge.dmg";
  const bridgeSourceZipUrl = "/downloads/slib-nfc-bridge.zip";
  const bridgeStatusLabel = bridgeStatus.status === "ready"
    ? "Sẵn sàng"
    : bridgeStatus.status === "bridge_only"
      ? "Chưa thấy đầu đọc"
      : bridgeStatus.status === "offline"
        ? "Chưa chạy"
        : "Đang kiểm tra";

  const handleDownloadBridge = useCallback(async (platform = "windows") => {
    const preferredUrl = platform === "mac" ? bridgeMacInstallerUrl : bridgeInstallerUrl;
    setBridgeDownloading(true);
    try {
      try {
        const response = await fetch(preferredUrl, { method: "HEAD", cache: "no-store" });
        if (response.ok) {
          triggerDownload(preferredUrl);
          setShowBridgeDropdown(false);
          return;
        }
      } catch (error) {
        console.warn("Không kiểm tra được file cài NFC Bridge:", error);
      }

      showToast("Server chưa có bộ cài phát hành. Đang chuyển sang gói source zip dự phòng.", "error");
      triggerDownload(bridgeSourceZipUrl);
      setShowBridgeDropdown(false);
    } finally {
      setBridgeDownloading(false);
    }
  }, [bridgeInstallerUrl, bridgeMacInstallerUrl, bridgeSourceZipUrl, triggerDownload]);

  // ===== RENDER =====
  return (
    <div className="nfc-map-page">
      {/* HEADER */}
      <div className="nfc-map-header">
        <div className="nfc-map-header__left">
          <Nfc size={22} color="#e8600a" />
          <h1 className="nfc-map-header__title">Quản lý NFC Tag</h1>
          <div className="nfc-map-header__stats">
            <span className="nfc-map-stat">{stats.total} ghế</span>
            <span className="nfc-map-stat nfc-map-stat--mapped">{stats.mapped} đã gán</span>
            <span className="nfc-map-stat nfc-map-stat--unmapped">{stats.unmapped} chưa gán</span>
          </div>
        </div>
        <div className="nfc-map-header__right">
          <div className="nfc-bridge-wrapper" ref={bridgeDropdownRef}>
            <button
              className={`nfc-bridge-chip nfc-bridge-chip--${bridgeStatus.status}`}
              onClick={() => setShowBridgeDropdown(prev => !prev)}
            >
              <span className={`nfc-bridge-chip__dot nfc-bridge-chip__dot--${bridgeStatus.status}`}></span>
              <span className="nfc-bridge-chip__text">NFC Bridge: {bridgeStatusLabel}</span>
              <ChevronDown size={14} className={`nfc-bridge-chip__arrow ${showBridgeDropdown ? "nfc-bridge-chip__arrow--open" : ""}`} />
            </button>
            {showBridgeDropdown && (
              <div className="nfc-bridge-dropdown">
                <div className="nfc-bridge-dropdown__header">
                  <span className={`nfc-bridge-card__dot nfc-bridge-card__dot--${bridgeStatus.status}`}></span>
                  <span className="nfc-bridge-dropdown__title">NFC Bridge: {bridgeStatusLabel}</span>
                </div>
                <p className="nfc-bridge-dropdown__message">{bridgeStatus.message}</p>
                <p className="nfc-bridge-dropdown__meta">Địa chỉ: {bridgeStatus.bridgeUrl}</p>
                {bridgeStatus.readerName && (
                  <p className="nfc-bridge-dropdown__meta">Đầu đọc: {bridgeStatus.readerName}</p>
                )}
                <div className="nfc-bridge-dropdown__divider"></div>
                <div className="nfc-bridge-dropdown__actions">
                  <button
                    className="nfc-bridge-btn nfc-bridge-btn--primary"
                    onClick={handleOpenBridgeApp}
                    disabled={bridgeLaunching}
                  >
                    <Wifi size={14} />
                    {bridgeLaunching ? "Đang mở công cụ..." : "Mở công cụ NFC"}
                  </button>
                  <button
                    className="nfc-bridge-btn"
                    onClick={() => handleDownloadBridge("windows")}
                    disabled={bridgeDownloading}
                  >
                    <Download size={14} />
                    {bridgeDownloading ? "Đang kiểm tra..." : "Tải app Windows"}
                  </button>
                  <button
                    className="nfc-bridge-btn"
                    onClick={() => { setShowBridgeGuide(true); setShowBridgeDropdown(false); }}
                  >
                    <Info size={14} />
                    Hướng dẫn cài
                  </button>
                  <button
                    className="nfc-bridge-btn"
                    onClick={() => checkBridgeConnection(true)}
                    disabled={bridgeChecking}
                  >
                    <RefreshCw size={14} className={bridgeChecking ? "spin" : ""} />
                    Kiểm tra kết nối
                  </button>
                </div>
              </div>
            )}
          </div>
          <button
            className="nfc-check-btn"
            onClick={() => { setShowNfcCheckModal(true); setNfcCheckResult(null); setNfcCheckError(null); }}
          >
            <Nfc size={16} />
            Check NFC
          </button>
          <button className="nfc-map-btn" onClick={loadData} disabled={loading}>
            <RefreshCw size={16} className={loading ? "spin" : ""} />
          </button>
          <div className="nfc-map-zoom">
            <button className="nfc-map-btn" onClick={() => setZoom(z => Math.max(z - 0.1, 0.15))}>
              <ZoomOut size={16} />
            </button>
            <span className="nfc-map-zoom__label">{Math.round(zoom * 100)}%</span>
            <button className="nfc-map-btn" onClick={() => setZoom(z => Math.min(z + 0.1, 3))}>
              <ZoomIn size={16} />
            </button>
            <button className="nfc-map-btn" onClick={handleFitToView}>
              <Maximize2 size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* AREA TABS */}
      {areas.length > 1 && (
        <div className="nfc-map-tabs">
          {areas.map(a => (
            <button
              key={a.areaId}
              className={`nfc-map-tab ${a.areaId === selectedAreaId ? "nfc-map-tab--active" : ""}`}
              onClick={() => setSelectedAreaId(a.areaId)}
            >
              {a.areaName}
            </button>
          ))}
        </div>
      )}

      {/* LEGEND */}
      <div className="nfc-map-legend">
        <span className="nfc-map-legend__item">
          <span className="nfc-map-legend__dot nfc-map-legend__dot--mapped"></span>
          Đã gán NFC
        </span>
        <span className="nfc-map-legend__item">
          <span className="nfc-map-legend__dot nfc-map-legend__dot--unmapped"></span>
          Chưa gán NFC
        </span>
        <span className="nfc-map-legend__item">
          <span className="nfc-map-legend__dot nfc-map-legend__dot--selected"></span>
          Đang chọn
        </span>
      </div>

      <div className="nfc-map-body">
        {/* CANVAS */}
        <div
          className="nfc-map-canvas"
          ref={boardRef}
          onWheel={handleWheel}
          onMouseDown={handleMouseDown}
          style={{ cursor: isPanning ? "grabbing" : "grab" }}
        >
          {loading ? (
            <div className="nfc-map-loading">
              <RefreshCw size={24} className="spin" />
              <span>Đang tải sơ đồ...</span>
            </div>
          ) : loadError ? (
            <LoadErrorState
              title="Không thể tải dữ liệu NFC"
              message={loadError}
              onRetry={loadData}
              compact
            />
          ) : (
            <div
              className="nfc-map-transform"
              style={{
                transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
                transformOrigin: "0 0",
              }}
            >
              {/* Render current area */}
              {currentArea && (
                <div
                  className="nfc-area"
                  style={{
                    left: currentArea.positionX || 0,
                    top: currentArea.positionY || 0,
                    width: currentArea.width || 300,
                    height: currentArea.height || 250,
                  }}
                >
                  <div className="nfc-area__header">
                    {currentArea.areaName}
                  </div>
                  <div className="nfc-area__content">
                    {/* Factories */}
                    {areaFactories.map(f => (
                      <div
                        key={f.factoryId}
                        className="nfc-factory"
                        style={{
                          left: f.positionX || 0,
                          top: f.positionY || 0,
                          width: f.width || 120,
                          height: f.height || 80,
                          backgroundColor: f.color || "#9CA3AF",
                        }}
                      >
                        <span className="nfc-factory__name">{f.factoryName}</span>
                      </div>
                    ))}

                    {/* Zones */}
                    {areaZones.map(zone => {
                      const zoneSeats = seats.filter(s => String(s.zoneId) === String(zone.zoneId));
                      return (
                        <div
                          key={zone.zoneId}
                          className="nfc-zone"
                          style={{
                            left: zone.positionX || 0,
                            top: zone.positionY || 0,
                            width: zone.width || 120,
                            height: zone.height || 100,
                          }}
                        >
                          <div className="nfc-zone__header">{zone.zoneName}</div>
                          <div className="nfc-zone__seats">
                            {zoneSeats.map(seat => {
                              const layout = calculateDynamicSeatLayout(
                                seat,
                                zone.width || 120,
                                zone.height || 100,
                                zoneSeats
                              );
                              const hasNfc = nfcMap[seat.seatId]?.hasNfcTag;
                              const isSelected = selectedSeat?.seatId === seat.seatId;

                              return (
                                <div
                                  key={seat.seatId}
                                  className={`nfc-seat ${hasNfc ? "nfc-seat--mapped" : "nfc-seat--unmapped"} ${isSelected ? "nfc-seat--selected" : ""}`}
                                  style={{
                                    position: "absolute",
                                    left: layout.positionX,
                                    top: layout.positionY,
                                    width: layout.width,
                                    height: layout.height,
                                  }}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleViewInfo(seat);
                                  }}
                                  title={`${seat.seatCode} — ${hasNfc ? "Đã gán NFC" : "Chưa gán NFC"}`}
                                >
                                  {seat.seatCode || "S"}
                                </div>
                              );
                            })}
                          </div>
                          <div className="nfc-zone__footer">{zoneSeats.length} ghế</div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* DETAIL PANEL */}
        {selectedSeat && (
          <div className="nfc-detail-panel">
            <div className="nfc-detail-panel__header">
              <h3>
                <Nfc size={18} />
                Ghế {selectedSeat.seatCode}
              </h3>
              <button className="nfc-detail-panel__close" onClick={() => { setSelectedSeat(null); setSeatNfcInfo(null); }}>
                <X size={18} />
              </button>
            </div>

            {infoLoading ? (
              <div className="nfc-detail-panel__loading">
                <RefreshCw size={18} className="spin" /> Đang tải...
              </div>
            ) : seatNfcInfo ? (
              <div className="nfc-detail-panel__body">
                <div className="nfc-detail-row">
                  <span className="nfc-detail-row__label">Mã ghế</span>
                  <span className="nfc-detail-row__value">{seatNfcInfo.seatCode}</span>
                </div>
                <div className="nfc-detail-row">
                  <span className="nfc-detail-row__label">Khu vực</span>
                  <span className="nfc-detail-row__value">{seatNfcInfo.areaName || "—"}</span>
                </div>
                <div className="nfc-detail-row">
                  <span className="nfc-detail-row__label">Vùng</span>
                  <span className="nfc-detail-row__value">{seatNfcInfo.zoneName || "—"}</span>
                </div>
                <div className="nfc-detail-row">
                  <span className="nfc-detail-row__label">Trạng thái</span>
                  <span className="nfc-detail-row__value">
                    {seatNfcInfo.nfcMapped ? (
                      <span className="nfc-badge nfc-badge--mapped">
                        <CheckCircle size={12} /> Đã gán
                      </span>
                    ) : (
                      <span className="nfc-badge nfc-badge--unmapped">
                        <XCircle size={12} /> Chưa gán
                      </span>
                    )}
                  </span>
                </div>
                {seatNfcInfo.nfcUidMasked && (
                  <div className="nfc-detail-row">
                    <span className="nfc-detail-row__label">UID</span>
                    <span className="nfc-detail-row__value nfc-detail-row__uid">
                      {seatNfcInfo.nfcUidMasked}
                    </span>
                  </div>
                )}
                {(seatNfcInfo.lastUpdated || seatNfcInfo.updatedAt || seatNfcInfo.nfcUpdatedAt) && (
                  <div className="nfc-detail-row">
                    <span className="nfc-detail-row__label">Cập nhật</span>
                    <span className="nfc-detail-row__value">
                      {new Date(seatNfcInfo.lastUpdated || seatNfcInfo.updatedAt || seatNfcInfo.nfcUpdatedAt).toLocaleString("vi-VN")}
                    </span>
                  </div>
                )}

                <div className="nfc-detail-panel__actions">
                  {seatNfcInfo.nfcMapped ? (
                    <>
                      <button
                        className="nfc-action-btn nfc-action-btn--scan"
                        onClick={() => handleScanAndAssign(seatNfcInfo.seatId)}
                        disabled={scanLoading}
                      >
                        <Wifi size={16} />
                        {scanLoading ? "Đang quét..." : "Quét lại"}
                      </button>
                      <button
                        className="nfc-action-btn nfc-action-btn--danger"
                        onClick={() => handleClearNfc(seatNfcInfo.seatId)}
                        disabled={actionLoading}
                      >
                        <Trash2 size={16} />
                        Xóa NFC
                      </button>
                    </>
                  ) : (
                    <button
                      className="nfc-action-btn nfc-action-btn--primary"
                      onClick={() => handleScanAndAssign(seatNfcInfo.seatId)}
                      disabled={scanLoading}
                    >
                      <Plus size={16} />
                      {scanLoading ? "Đang quét..." : "Quét & Gán NFC"}
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <div className="nfc-detail-panel__loading">Không có dữ liệu</div>
            )}
          </div>
        )}
      </div>

      {/* NFC CHECK MODAL */}
      {showNfcCheckModal && (
        <div className="nfc-check-overlay" onClick={() => { setShowNfcCheckModal(false); setNfcCheckResult(null); setNfcCheckError(null); }}>
          <div className="nfc-check-modal" onClick={(e) => e.stopPropagation()}>
            <h3 className="nfc-check-modal__title">
              <Nfc size={20} color="#e8600a" />
              Check Thông tin NFC
            </h3>

            {/* Scan Button */}
            {!nfcCheckResult && (
              <button
                className={`nfc-check-modal__scan-btn ${nfcCheckScanning ? 'nfc-check-modal__scan-btn--scanning' : ''}`}
                onClick={async () => {
                  setNfcCheckScanning(true);
                  setNfcCheckError(null);
                  setNfcCheckResult(null);
                  try {
                    const scanData = await nfcManagementService.scanNfcFromBridge();
                    if (scanData.success && scanData.uid) {
                      try {
                        const seatResponse = await getSeatByNfcUid(scanData.uid);
                        const seat = seatResponse.data;
                        const zone = zones.find(z => z.zoneId === seat.zoneId);
                        const area = zone ? areas.find(a => a.areaId === zone.areaId) : null;
                        setNfcCheckResult({
                          ...seat,
                          zoneName: zone?.zoneName || 'Không rõ',
                          areaName: area?.areaName || 'Không rõ'
                        });
                      } catch (err) {
                        if (err.response?.status === 404) {
                          setNfcCheckError('Thẻ NFC này chưa được gán cho ghế nào');
                        } else {
                          setNfcCheckError(err.response?.data?.error || 'Lỗi khi tra cứu thông tin ghế');
                        }
                      }
                    } else {
                      setNfcCheckError(scanData.error || 'Không đọc được thẻ NFC');
                    }
                  } catch (err) {
                    setNfcCheckError(err.message || 'Lỗi quét NFC');
                  } finally {
                    setNfcCheckScanning(false);
                  }
                }}
                disabled={nfcCheckScanning}
              >
                {nfcCheckScanning ? 'Đang chờ thẻ...' : 'Quét thẻ NFC'}
              </button>
            )}

            {/* Result */}
            {nfcCheckResult && (
              <div className="nfc-check-result">
                <div className="nfc-check-result__seat-code">{nfcCheckResult.seatCode}</div>
                <div className="nfc-check-result__grid">
                  <div className="nfc-check-result__item">
                    <span className="nfc-check-result__label">ID ghế</span>
                    <span className="nfc-check-result__value">{nfcCheckResult.seatId}</span>
                  </div>
                  <div className="nfc-check-result__item">
                    <span className="nfc-check-result__label">Trạng thái</span>
                    <span className={`nfc-check-result__value ${nfcCheckResult.isActive ? 'nfc-check-result__value--active' : 'nfc-check-result__value--inactive'}`}>
                      {nfcCheckResult.isActive ? 'Hoạt động' : 'Bảo trì'}
                    </span>
                  </div>
                  <div className="nfc-check-result__item">
                    <span className="nfc-check-result__label">Khu vực</span>
                    <span className="nfc-check-result__value">{nfcCheckResult.zoneName}</span>
                  </div>
                  <div className="nfc-check-result__item">
                    <span className="nfc-check-result__label">Phòng</span>
                    <span className="nfc-check-result__value">{nfcCheckResult.areaName}</span>
                  </div>
                </div>
              </div>
            )}

            {/* Error */}
            {nfcCheckError && (
              <div className="nfc-check-error">{nfcCheckError}</div>
            )}

            {/* Actions */}
            <div className="nfc-check-modal__actions">
              <button
                className="nfc-check-modal__close-btn"
                onClick={() => { setShowNfcCheckModal(false); setNfcCheckResult(null); setNfcCheckError(null); }}
              >
                Đóng
              </button>
              {nfcCheckResult && (
                <button
                  className="nfc-check-modal__rescan-btn"
                  onClick={() => { setNfcCheckResult(null); setNfcCheckError(null); }}
                >
                  Quét thẻ khác
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {showBridgeGuide && (
        <div className="nfc-check-overlay" onClick={() => setShowBridgeGuide(false)}>
          <div className="nfc-check-modal nfc-bridge-guide" onClick={(e) => e.stopPropagation()}>
            <h3 className="nfc-check-modal__title">
              <Info size={20} color="#e8600a" />
              Hướng dẫn cài NFC Bridge
            </h3>

            <div className="nfc-bridge-guide__body">
              <p className="nfc-bridge-guide__intro">
                Máy nào cần quét thẻ NFC thì máy đó phải cài <strong>SLIB NFC Bridge</strong> trên chính máy đó. Cách khuyến nghị là cài app desktop sẵn, không còn đi theo flow source zip và <code>npm install</code> như trước.
              </p>

              <div className="nfc-bridge-guide__box nfc-bridge-guide__box--highlight">
                <div className="nfc-bridge-guide__box-title">Khuyến nghị cho máy mới</div>
                <ul className="nfc-bridge-guide__list">
                  <li>Dùng <strong>bản cài đặt Windows</strong> nếu máy là máy thủ thư/kiosk mới.</li>
                  <li>Chỉ dùng <strong>gói source zip</strong> khi team kỹ thuật cần chạy thủ công hoặc debug bridge.</li>
                  <li>Sau khi cài app thành công, web có thể gọi trực tiếp nút <strong>Mở công cụ NFC</strong>.</li>
                </ul>
              </div>

              <ol className="nfc-bridge-guide__steps">
                <li>Tải bộ cài <strong>SLIB NFC Bridge</strong> bằng nút <strong>Tải app Windows</strong>.</li>
                <li>Mở file cài đặt và hoàn tất wizard cài app trên máy thủ thư/kiosk.</li>
                <li>Cắm đầu đọc <strong>ACR122U</strong> vào máy.</li>
                <li>Mở app lần đầu hoặc bấm trực tiếp <strong>Mở công cụ NFC</strong> trên web để kích hoạt bridge.</li>
                <li>Quay lại trang này và bấm <strong>Kiểm tra kết nối</strong>.</li>
              </ol>

              <div className="nfc-bridge-guide__box">
                <div className="nfc-bridge-guide__box-title">Khi nào bridge sẵn sàng?</div>
                <ul className="nfc-bridge-guide__list">
                  <li>Bridge chạy ở <strong>{NFC_BRIDGE_URL}</strong>.</li>
                  <li>Trạng thái hiển thị <strong>Sẵn sàng</strong>.</li>
                  <li>Nếu hiện <strong>Chưa thấy đầu đọc</strong>, hãy kiểm tra lại cáp USB hoặc driver ACR122U.</li>
                </ul>
              </div>

              <div className="nfc-bridge-guide__box">
                <div className="nfc-bridge-guide__box-title">Phương án dự phòng cho team kỹ thuật</div>
                <ul className="nfc-bridge-guide__list">
                  <li>Nếu máy chưa có bộ cài phát hành, có thể dùng <strong>gói source zip</strong> để chạy bridge thủ công.</li>
                  <li>Flow cũ yêu cầu cài <strong>Node 20 LTS</strong> và môi trường native module cho đầu đọc NFC.</li>
                  <li>Chỉ nên dùng cách này cho máy kỹ thuật, không khuyến nghị cho máy mới ngoài hiện trường.</li>
                </ul>
              </div>

            </div>

            <div className="nfc-bridge-guide__footer">
              <div className="nfc-bridge-guide__footer-links">
                <button
                  className="nfc-bridge-guide__footer-btn nfc-bridge-guide__footer-btn--primary"
                  onClick={handleOpenBridgeApp}
                  disabled={bridgeLaunching}
                >
                  <Wifi size={14} />
                  {bridgeLaunching ? "Đang mở..." : "Mở công cụ NFC"}
                </button>
                <button
                  className="nfc-bridge-guide__footer-btn"
                  onClick={() => handleDownloadBridge("windows")}
                  disabled={bridgeDownloading}
                >
                  <Download size={14} />
                  {bridgeDownloading ? "Đang kiểm tra..." : "Tải app Windows"}
                </button>
                <button
                  className="nfc-bridge-guide__footer-btn"
                  onClick={() => handleDownloadBridge("mac")}
                  disabled={bridgeDownloading}
                >
                  <Download size={14} />
                  Tải app macOS
                </button>
                <a
                  className="nfc-bridge-guide__footer-btn"
                  href={bridgeSourceZipUrl}
                  download
                >
                  <Info size={14} />
                  Tải source zip dự phòng
                </a>
              </div>
              <button
                className="nfc-bridge-guide__close-btn"
                onClick={() => setShowBridgeGuide(false)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* TOAST */}
      {toast && (
        <div className={`nfc-toast nfc-toast--${toast.type}`}>
          {toast.type === "success" ? <CheckCircle size={16} /> : <XCircle size={16} />}
          {toast.message}
        </div>
      )}
    </div>
  );
};

export default NfcManagement;
