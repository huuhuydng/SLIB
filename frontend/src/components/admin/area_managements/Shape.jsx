import { useRef, useState, useEffect } from 'react';
import { useLayout } from '../../../context/admin/area_management/LayoutContext';
import { Rnd } from 'react-rnd';

// Shape types available
export const SHAPE_TYPES = [
    { id: 'rectangle', name: 'Hình chữ nhật' },
    { id: 'oval', name: 'Hình oval' },
    { id: 'line', name: 'Đường kẻ' },
];

function Shape({ factory, area }) {
    const { state, dispatch, actions } = useLayout();
    const { selectedItem, selectedItems, factories, zones, canvas, isPreviewMode } = state;

    // Check if this factory is selected (either single or multi-select)
    const isSelected = (selectedItem?.type === 'factory' && selectedItem?.id === factory.factoryId) ||
        (selectedItems || []).some(item => item.type === 'factory' && item.id === factory.factoryId);
    const [collidingWith, setCollidingWith] = useState(null);
    const [resetKey, setResetKey] = useState(0);
    const [isEditing, setIsEditing] = useState(false);
    const [localName, setLocalName] = useState(factory.factoryName || '');
    const inputRef = useRef(null);
    const dragStartPos = useRef({ x: 0, y: 0 }); // Track start position for multi-select drag

    // Get shape type (default to rectangle)
    const shapeType = factory.shapeType || 'rectangle';

    // Delay rendering until after initial render cycle to ensure position and zoom are stable
    // react-rnd has issues reading controlled position correctly when scale changes on mount
    const [isReady, setIsReady] = useState(false);

    useEffect(() => {
        // Wait for canvas zoom to stabilize after handleFitToView
        const timer = setTimeout(() => {
            setIsReady(true);
        }, 350);
        return () => clearTimeout(timer);
    }, []);


    // Helper: Check if two rectangles overlap
    const isColliding = (rect1, rect2, padding = 0) => {
        return (
            rect1.left < rect2.right - padding &&
            rect1.right + padding > rect2.left &&
            rect1.top < rect2.bottom - padding &&
            rect1.bottom + padding > rect2.top
        );
    };

    // Helper: Check if factory collides with any other factory in same area
    const getCollisionInfo = (factoryId, newX, newY, newWidth, newHeight) => {
        const newRect = {
            left: newX,
            right: newX + newWidth,
            top: newY,
            bottom: newY + newHeight,
        };

        // Check against other factories
        for (const otherFactory of (factories || [])) {
            if (otherFactory.factoryId === factoryId) continue;
            if (String(otherFactory.areaId) !== String(factory?.areaId)) continue;

            const otherRect = {
                left: otherFactory.positionX || 0,
                right: (otherFactory.positionX || 0) + (otherFactory.width || 160),
                top: otherFactory.positionY || 0,
                bottom: (otherFactory.positionY || 0) + (otherFactory.height || 120),
            };

            if (isColliding(newRect, otherRect, -2)) {
                return { hasCollision: true, collidingFactory: otherFactory };
            }
        }

        // Check against zones in same area
        for (const zone of (zones || [])) {
            if (String(zone.areaId) !== String(factory?.areaId)) continue;

            const zoneRect = {
                left: zone.positionX || 0,
                right: (zone.positionX || 0) + (zone.width || 200),
                top: zone.positionY || 0,
                bottom: (zone.positionY || 0) + (zone.height || 150),
            };

            if (isColliding(newRect, zoneRect, -2)) {
                return { hasCollision: true, collidingFactory: { factoryName: `Zone: ${zone.zoneName}`, isZone: true } };
            }
        }

        return { hasCollision: false, collidingFactory: null };
    };

    const handleSelectFactory = (e) => {
        if (e) e.stopPropagation();
        const isMac = navigator.platform.toUpperCase().includes('MAC');
        const isMultiSelectKey = isMac ? e.metaKey : e.ctrlKey;

        if (isMultiSelectKey) {
            // Ctrl+Click: Toggle this factory in/out of selection
            dispatch({
                type: actions.TOGGLE_SELECT,
                payload: { type: 'factory', id: factory.factoryId },
            });
        } else if (isSelected) {
            // Clicking on already selected item - keep selection, just update selectedItem
            dispatch({
                type: actions.SELECT_ITEM,
                payload: { type: 'factory', id: factory.factoryId },
            });
        } else {
            // Normal click on non-selected item: Select only this factory, clear others
            dispatch({ type: actions.DESELECT });
            dispatch({
                type: actions.SELECT_ITEM,
                payload: { type: 'factory', id: factory.factoryId },
            });
        }
    };

    // Push history when starting drag/resize for undo
    const handleDragStart = (e, d) => {
        dispatch({ type: actions.PUSH_HISTORY });
        // Save start position for multi-select drag delta calculation
        dragStartPos.current = { x: factory.positionX || 0, y: factory.positionY || 0 };
    };

    const handleResizeStart = () => {
        dispatch({ type: actions.PUSH_HISTORY });
    };

    const handleDrag = (e, d) => {
        // Check for collision
        const { hasCollision, collidingFactory } = getCollisionInfo(
            factory.factoryId,
            d.x,
            d.y,
            factory.width || 160,
            factory.height || 120
        );

        if (hasCollision) {
            setCollidingWith(collidingFactory);
            return;
        }

        setCollidingWith(null);

        // Check if this factory is part of multi-select
        const isMultiSelected = (selectedItems || []).length > 1 &&
            (selectedItems || []).some(item => item.type === 'factory' && item.id === factory.factoryId);

        if (isMultiSelected) {
            // Calculate delta from last known position
            const dx = d.x - (factory.positionX || 0);
            const dy = d.y - (factory.positionY || 0);

            if (dx !== 0 || dy !== 0) {
                // Move all selected items by delta (realtime)
                dispatch({ type: actions.MOVE_ALL_SELECTED, payload: { dx, dy } });
            }
        } else {
            // Update only this factory
            dispatch({
                type: actions.UPDATE_FACTORY,
                payload: {
                    ...factory,
                    positionX: d.x,
                    positionY: d.y,
                },
            });
        }

        // Mark as having unsaved changes
        dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    };

    const handleDragStop = (e, d) => {
        // Check if position actually changed (user dragged, not just clicked)
        const startX = dragStartPos.current.x;
        const startY = dragStartPos.current.y;
        const hasPositionChanged = Math.abs(d.x - startX) > 1 || Math.abs(d.y - startY) > 1;

        // If no actual movement, do nothing (prevents cache pollution on click)
        if (!hasPositionChanged) {
            return;
        }

        // Final collision check
        const { hasCollision, collidingFactory } = getCollisionInfo(
            factory.factoryId,
            d.x,
            d.y,
            factory.width || 160,
            factory.height || 120
        );

        if (hasCollision) {
            setCollidingWith(collidingFactory);
            setResetKey(prev => prev + 1);
            // Clear warning after reset animation
            setTimeout(() => setCollidingWith(null), 300);
            return;
        }

        setCollidingWith(null);

        // Check if this factory is part of multi-select
        const isMultiSelected = (selectedItems || []).length > 1 &&
            (selectedItems || []).some(item => item.type === 'factory' && item.id === factory.factoryId);

        // For single selection, update the factory position 
        // (for multi-select, realtime drag already updated all positions)
        if (!isMultiSelected) {
            dispatch({
                type: actions.UPDATE_FACTORY,
                payload: {
                    ...factory,
                    positionX: d.x,
                    positionY: d.y,
                },
            });
        }

        // REMOVED: cacheFactoryPosition - don't cache unsaved changes to localStorage
        dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    };

    const handleResizeStop = (e, direction, ref, delta, position) => {
        const newWidth = parseInt(ref.style.width);
        const newHeight = parseInt(ref.style.height);

        // Check if size or position actually changed
        const hasSizeChanged = Math.abs(newWidth - (factory.width || 160)) > 1 ||
            Math.abs(newHeight - (factory.height || 120)) > 1;
        const hasPositionChanged = Math.abs(position.x - (factory.positionX || 0)) > 1 ||
            Math.abs(position.y - (factory.positionY || 0)) > 1;

        // If no actual change, do nothing
        if (!hasSizeChanged && !hasPositionChanged) {
            return;
        }

        // Check for collision
        const { hasCollision, collidingFactory } = getCollisionInfo(
            factory.factoryId,
            position.x,
            position.y,
            newWidth,
            newHeight
        );

        if (hasCollision) {
            setCollidingWith(collidingFactory);
            setResetKey(prev => prev + 1);
            // Clear warning after reset animation
            setTimeout(() => setCollidingWith(null), 300);
            return;
        }

        setCollidingWith(null);

        // Update state only (optimistic update)
        dispatch({
            type: actions.UPDATE_FACTORY,
            payload: {
                ...factory,
                positionX: position.x,
                positionY: position.y,
                width: newWidth,
                height: newHeight,
            },
        });

        // REMOVED: cacheFactoryPosition - don't cache unsaved changes to localStorage
        dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    };

    const handleDoubleClick = (e) => {
        e.stopPropagation();
        setIsEditing(true);
        setLocalName(factory.factoryName || '');
        setTimeout(() => inputRef.current?.focus(), 50);
    };

    const handleNameChange = (e) => {
        setLocalName(e.target.value);
    };

    const handleNameBlur = () => {
        setIsEditing(false);
        if (localName !== factory.factoryName) {
            dispatch({
                type: actions.UPDATE_FACTORY,
                payload: { ...factory, factoryName: localName },
            });
            dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            handleNameBlur();
        } else if (e.key === 'Escape') {
            setIsEditing(false);
            setLocalName(factory.factoryName || '');
        }
    };

    // Render shape based on type
    const renderShapeContent = () => {
        const isLockedState = factory.isLocked || area?.locked;

        const baseStyle = {
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: collidingWith ? '#fee8e8' : '#9CA3AF',
            border: collidingWith
                ? '3px solid #dc2626'
                : isSelected
                    ? '3px solid #3B82F6'
                    : isLockedState
                        ? '2px solid #fca5a5'
                        : '2px solid #9CA3AF',
            cursor: isLockedState ? 'default' : 'move',
            position: 'relative',
            overflow: 'visible',
            fontSize: '13px',
            fontWeight: '500',
            color: '#374151',
            textAlign: 'center',
            padding: '8px',
            boxSizing: 'border-box',
            boxShadow: collidingWith
                ? '0 0 15px rgba(220, 38, 38, 0.6), inset 0 0 10px rgba(220, 38, 38, 0.1)'
                : isLockedState
                    ? '0 0 10px rgba(220, 38, 38, 0.2)'
                    : 'none',
            transition: 'all 0.2s ease',
        };

        // Warning badge component - only show for collision, not for locked state
        const WarningBadge = collidingWith ? (
            <div style={{
                position: 'absolute',
                top: '-8px',
                right: '-8px',
                backgroundColor: '#dc2626',
                color: 'white',
                padding: '4px 8px',
                borderRadius: '4px',
                fontSize: '11px',
                fontWeight: '600',
                zIndex: 100,
                whiteSpace: 'nowrap',
            }}>
                ⚠️ Chồng lấp
            </div>
        ) : null;

        switch (shapeType) {
            case 'oval':
                return (
                    <div
                        style={{
                            ...baseStyle,
                            borderRadius: '50%',
                        }}
                        onClick={handleSelectFactory}
                        onDoubleClick={handleDoubleClick}
                    >
                        {WarningBadge}
                        {isEditing ? (
                            <input
                                ref={inputRef}
                                value={localName}
                                onChange={handleNameChange}
                                onBlur={handleNameBlur}
                                onKeyDown={handleKeyDown}
                                style={{
                                    background: 'white',
                                    border: '1px solid #3B82F6',
                                    borderRadius: '4px',
                                    padding: '4px 8px',
                                    fontSize: '12px',
                                    width: '80%',
                                    textAlign: 'center',
                                }}
                                onClick={(e) => e.stopPropagation()}
                            />
                        ) : (
                            <span>{factory.factoryName}</span>
                        )}
                    </div>
                );

            case 'line':
                return (
                    <div
                        style={{
                            ...baseStyle,
                            borderRadius: '0',
                            height: '4px',
                            minHeight: '4px',
                            position: 'relative',
                        }}
                        onClick={handleSelectFactory}
                        onDoubleClick={handleDoubleClick}
                    >
                        {WarningBadge}
                        {isEditing ? (
                            <input
                                ref={inputRef}
                                value={localName}
                                onChange={handleNameChange}
                                onBlur={handleNameBlur}
                                onKeyDown={handleKeyDown}
                                style={{
                                    position: 'absolute',
                                    top: '8px',
                                    left: '50%',
                                    transform: 'translateX(-50%)',
                                    background: 'white',
                                    border: '1px solid #3B82F6',
                                    borderRadius: '4px',
                                    padding: '4px 8px',
                                    fontSize: '11px',
                                    width: '80px',
                                    textAlign: 'center',
                                }}
                                onClick={(e) => e.stopPropagation()}
                            />
                        ) : (
                            factory.factoryName && (
                                <span style={{
                                    position: 'absolute',
                                    top: '8px',
                                    left: '50%',
                                    transform: 'translateX(-50%)',
                                    background: 'white',
                                    padding: '2px 6px',
                                    borderRadius: '3px',
                                    fontSize: '10px',
                                    whiteSpace: 'nowrap',
                                }}>
                                    {factory.factoryName}
                                </span>
                            )
                        )}
                    </div>
                );

            case 'rectangle':
            default:
                return (
                    <div
                        style={{
                            ...baseStyle,
                            borderRadius: '6px',
                        }}
                        onClick={handleSelectFactory}
                        onDoubleClick={handleDoubleClick}
                    >
                        {WarningBadge}
                        {isEditing ? (
                            <input
                                ref={inputRef}
                                value={localName}
                                onChange={handleNameChange}
                                onBlur={handleNameBlur}
                                onKeyDown={handleKeyDown}
                                style={{
                                    background: 'white',
                                    border: '1px solid #3B82F6',
                                    borderRadius: '4px',
                                    padding: '4px 8px',
                                    fontSize: '12px',
                                    width: '80%',
                                    textAlign: 'center',
                                }}
                                onClick={(e) => e.stopPropagation()}
                            />
                        ) : (
                            <span>{factory.factoryName}</span>
                        )}
                    </div>
                );
        }
    };

    // For line type, use different dimensions
    const displayHeight = shapeType === 'line' ? Math.max(20, factory.height || 20) : (factory.height || 120);

    // Don't render until we're ready (position data is stable)
    if (!isReady) {
        return null;
    }

    return (
        <>
            <Rnd
                key={`shape-${factory.factoryId}-${resetKey}`}
                scale={canvas?.zoom || 1}
                position={{
                    x: factory.positionX || 0,
                    y: factory.positionY || 0,
                }}
                size={{
                    width: factory.width || 160,
                    height: displayHeight,
                }}
                minWidth={shapeType === 'line' ? 40 : 60}
                minHeight={shapeType === 'line' ? 8 : 40}
                bounds="parent"
                onDragStart={(isPreviewMode || area?.locked || factory.isLocked) ? undefined : handleDragStart}
                onDrag={(isPreviewMode || area?.locked || factory.isLocked) ? undefined : handleDrag}
                onDragStop={(isPreviewMode || area?.locked || factory.isLocked) ? undefined : handleDragStop}
                onResizeStart={(isPreviewMode || area?.locked || factory.isLocked) ? undefined : handleResizeStart}
                onResizeStop={(isPreviewMode || area?.locked || factory.isLocked) ? undefined : handleResizeStop}
                disableDragging={isPreviewMode || !!area?.locked || !!factory.isLocked}
                enableResizing={(isPreviewMode || area?.locked || factory.isLocked) ? false : (shapeType === 'line' ? { left: true, right: true } : true)}
                style={{
                    position: 'absolute',
                    boxSizing: 'border-box',
                    zIndex: isSelected ? 200 : (factory.isPending ? 150 : 10),
                    cursor: (isPreviewMode || area?.locked || factory.isLocked) ? 'default' : 'move',
                }}
            >
                {renderShapeContent()}
            </Rnd>
        </>
    );
}

export default Shape;
