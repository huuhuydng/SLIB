import { useRef, useState } from 'react';
import { useLayout } from '../../../context/admin/area_management/LayoutContext';
import { Rnd } from 'react-rnd';

// Shape types available
export const SHAPE_TYPES = [
    { id: 'rectangle', name: 'Hình chữ nhật' },
    { id: 'oval', name: 'Hình oval' },
    { id: 'line', name: 'Đường kẻ' },
];

function Shape({ factory }) {
    const { state, dispatch, actions } = useLayout();
    const { selectedItem, factories, zones, canvas } = state;

    const isSelected = selectedItem?.type === 'factory' && selectedItem?.id === factory.factoryId;
    const [collidingWith, setCollidingWith] = useState(null);
    const [resetKey, setResetKey] = useState(0);
    const [isEditing, setIsEditing] = useState(false);
    const [localName, setLocalName] = useState(factory.factoryName || '');
    const inputRef = useRef(null);

    // Get shape type (default to rectangle)
    const shapeType = factory.shapeType || 'rectangle';

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
        dispatch({
            type: actions.SELECT_ITEM,
            payload: {
                type: 'factory',
                id: factory.factoryId,
            },
        });
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

        // Update UI immediately (optimistic update - no API call)
        dispatch({
            type: actions.UPDATE_FACTORY,
            payload: {
                ...factory,
                positionX: d.x,
                positionY: d.y,
            },
        });

        // Mark as having unsaved changes
        dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    };

    const handleDragStop = (e, d) => {
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
            return;
        }

        setCollidingWith(null);

        // Just update state, no API call (will be saved when user clicks Save)
        dispatch({
            type: actions.UPDATE_FACTORY,
            payload: {
                ...factory,
                positionX: d.x,
                positionY: d.y,
            },
        });

        dispatch({ type: actions.SET_UNSAVED_CHANGES, payload: true });
    };

    const handleResizeStop = (e, direction, ref, delta, position) => {
        const newWidth = parseInt(ref.style.width);
        const newHeight = parseInt(ref.style.height);

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
        const baseStyle = {
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#9CA3AF',  // Fixed gray for obstacles
            border: isSelected ? '3px solid #3B82F6' : '2px solid #9CA3AF',
            cursor: 'move',
            position: 'relative',
            overflow: 'hidden',
            fontSize: '13px',
            fontWeight: '500',
            color: '#374151',
            textAlign: 'center',
            padding: '8px',
            boxSizing: 'border-box',
        };

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

    return (
        <>
            {collidingWith && (
                <div
                    style={{
                        position: 'absolute',
                        top: factory.positionY - 24,
                        left: factory.positionX,
                        backgroundColor: '#DC2626',
                        color: 'white',
                        padding: '3px 8px',
                        borderRadius: '4px',
                        fontSize: '11px',
                        whiteSpace: 'nowrap',
                        zIndex: 200,
                        pointerEvents: 'none',
                    }}
                >
                    Trùng vị trí
                </div>
            )}

            <Rnd
                key={`shape-${factory.factoryId}-${resetKey}`}
                scale={canvas?.zoom || 1}
                default={{
                    x: factory.positionX || 0,
                    y: factory.positionY || 0,
                    width: factory.width || 160,
                    height: displayHeight,
                }}
                minWidth={shapeType === 'line' ? 40 : 60}
                minHeight={shapeType === 'line' ? 8 : 40}
                bounds="parent"
                onDrag={handleDrag}
                onDragStop={handleDragStop}
                onResizeStop={handleResizeStop}
                enableResizing={shapeType === 'line' ? { left: true, right: true } : true}
                style={{
                    position: 'absolute',
                    boxSizing: 'border-box',
                    zIndex: isSelected ? 100 : 10,
                }}
            >
                {renderShapeContent()}
            </Rnd>
        </>
    );
}

export default Shape;
