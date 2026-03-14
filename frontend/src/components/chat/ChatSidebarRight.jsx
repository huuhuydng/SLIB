import React, { useState, useEffect } from 'react';
import '../../styles/ChatManagement.css';
import { highlightText } from '../../utils/dateUtils';
import { getConversationMedia } from '../../services/admin/apiChat';
import '../../styles/ChatSidebarRight.css';

const ChatSidebarRight = ({ 
    isOpen, 
    onClose, 
    currentPartner, 
    myId,           
    onSearch,       
    searchResults,  
    isSearching,    
    onResultClick 
}) => {
    const [activeTab, setActiveTab] = useState('MENU'); 
    const [localSearchTerm, setLocalSearchTerm] = useState("");
    const [mediaList, setMediaList] = useState([]);
    const [isLoadingMedia, setIsLoadingMedia] = useState(false);
    
    // State quản lý phóng to ảnh
    const [selectedFullImage, setSelectedFullImage] = useState(null);

    useEffect(() => {
        if (!isOpen) {
            setTimeout(() => {
                setActiveTab('MENU');
                setLocalSearchTerm("");
                setMediaList([]);
                setSelectedFullImage(null);
            }, 300);
        }
    }, [isOpen, currentPartner]);

    useEffect(() => {
        if (isOpen && currentPartner) {
            if (activeTab === 'IMAGES') loadMediaData('IMAGE');
            if (activeTab === 'FILES') loadMediaData('FILE');
        }
    }, [activeTab, currentPartner, isOpen]);

    const loadMediaData = async (type) => {
        setIsLoadingMedia(true);
        try {
            const res = await getConversationMedia(currentPartner.id, type);
            setMediaList(res.data);
        } catch (e) {
            console.error("Lỗi tải kho lưu trữ:", e);
        } finally {
            setIsLoadingMedia(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') onSearch(localSearchTerm);
    };

    const getFileIcon = (url) => {
        const ext = url.split('.').pop().toLowerCase();
        if (['pdf'].includes(ext)) return '📕';
        if (['doc', 'docx'].includes(ext)) return '📘';
        if (['xls', 'xlsx'].includes(ext)) return '📗';
        return '📄';
    };

    if (!isOpen) return null;

    return (
        <div className="search-sidebar open">
            {/* --- HEADER --- */}
            <div className="search-sidebar-header">
                {activeTab === 'MENU' ? (
                    <span>Thông tin hội thoại</span>
                ) : (
                    <div className="header-back-group" style={{display:'flex', alignItems:'center', gap: '10px'}}>
                        <button className="btn-icon-small" onClick={() => setActiveTab('MENU')}>⬅</button>
                        <span>
                            {activeTab === 'SEARCH' ? "Tìm kiếm" : 
                             activeTab === 'IMAGES' ? "Ảnh & Video" : "File tài liệu"}
                        </span>
                    </div>
                )}
                <button className="btn-icon-small" onClick={onClose}>✕</button>
            </div>

            <div className="search-sidebar-body">
                {/* 1. MENU CHÍNH */}
                {activeTab === 'MENU' && (
                    <div className="sidebar-menu-list">
                        <div className="partner-info-summary">
                            <div className="big-avatar">
                                {currentPartner?.fullName?.charAt(0).toUpperCase() || "U"}
                            </div>
                            <div className="partner-name">{currentPartner?.fullName}</div>
                            <div className="partner-email-sub">{currentPartner?.email}</div>
                        </div>

                        <div className="menu-group">
                            <button className="menu-item" onClick={() => setActiveTab('SEARCH')}>
                                <span className="icon">🔍</span>
                                <span className="text">Tìm kiếm tin nhắn</span>
                            </button>
                            <button className="menu-item" onClick={() => setActiveTab('IMAGES')}>
                                <span className="icon">🖼️</span>
                                <span className="text">Ảnh & Video</span>
                            </button>
                            <button className="menu-item" onClick={() => setActiveTab('FILES')}>
                                <span className="icon">📁</span>
                                <span className="text">File tài liệu</span>
                            </button>
                        </div>
                    </div>
                )}

                {/* 2. GIAO DIỆN TÌM KIẾM */}
                {activeTab === 'SEARCH' && (
                    <div className="search-ui-container">
                        <div className="search-input-wrapper">
                            <input autoFocus type="text" className="sidebar-search-input"
                                placeholder="Nhập từ khóa & Enter..." value={localSearchTerm}
                                onChange={(e) => setLocalSearchTerm(e.target.value)} onKeyDown={handleKeyDown} />
                        </div>
                        <div className="search-results-list">
                            {isSearching ? <div className="loading-text">⏳ Đang tìm...</div> : (
                                <>
                                    <div className="result-count">{searchResults.length} kết quả</div>
                                    {searchResults.map((msg) => (
                                        <div key={msg.id} className="search-result-item" onClick={() => onResultClick(msg.id)}>
                                            <div className="search-avatar">{msg.senderId === myId ? "B" : currentPartner?.fullName?.charAt(0)}</div>
                                            <div className="search-content">
                                                <div className="search-sender-name">
                                                    {msg.senderId === myId ? "Bạn" : currentPartner?.fullName}
                                                    <span className="search-date">{new Date(msg.createdAt).toLocaleDateString()}</span>
                                                </div>
                                                <div className="search-text-preview">{highlightText(msg.content, localSearchTerm)}</div>
                                            </div>
                                        </div>
                                    ))}
                                </>
                            )}
                        </div>
                    </div>
                )}

                {/* 3. KHO ẢNH (IMAGES) - Đã sửa Grid */}
                {activeTab === 'IMAGES' && (
                    <div className="media-gallery-container">
                        {isLoadingMedia ? <div className="loading-text">Đang tải...</div> : (
                            mediaList.length > 0 ? (
                                <div className="media-grid">
                                    {mediaList.map(item => (
                                        <div key={item.id} className="media-item" onClick={() => setSelectedFullImage(item.attachmentUrl)}>
                                            <img src={item.attachmentUrl} alt="media" />
                                        </div>
                                    ))}
                                </div>
                            ) : <div className="empty-media">Chưa có ảnh nào</div>
                        )}
                    </div>
                )}

                {/* 4. KHO FILE (FILES) */}
                {activeTab === 'FILES' && (
                    <div className="file-archive-list">
                        {isLoadingMedia ? <div className="loading-text">Đang tải...</div> : (
                            mediaList.length > 0 ? mediaList.map(file => (
                                <div key={file.id} className="file-item-card" onClick={() => window.open(file.attachmentUrl, '_blank')}>
                                    <div className="file-icon-box">{getFileIcon(file.attachmentUrl)}</div>
                                    <div className="file-info-box">
                                        <div className="file-name-text">{file.content || "Tài liệu"}</div>
                                        <div className="file-date-sub">{new Date(file.createdAt).toLocaleDateString()}</div>
                                    </div>
                                    <button className="file-jump-btn" onClick={(e) => { e.stopPropagation(); onResultClick(file.id); }}>🎯</button>
                                </div>
                            )) : <div className="empty-media">Chưa có tài liệu nào</div>
                        )}
                    </div>
                )}
            </div>

            {/* --- LIGHTBOX OVERLAY --- */}
            {selectedFullImage && (
                <div className="image-lightbox-overlay" onClick={() => setSelectedFullImage(null)}>
                    <button className="close-lightbox">✕</button>
                    <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
                        <img src={selectedFullImage} alt="Full size" />
                        <a href={selectedFullImage} download target="_blank" rel="noreferrer" className="download-full-btn">
                            Mở ảnh gốc ↗
                        </a>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ChatSidebarRight;