import React from "react";
import { TrendingUp, TrendingDown, Minus } from "lucide-react";

/**
 * StatCard - Enhanced statistics card for SLIB Dashboard
 */
const StatCard = ({ 
  icon, 
  value, 
  label, 
  bg, 
  color, 
  trend,
  trendValue,
  variant = "default"
}) => {
  const variantStyles = {
    default: {
      iconBg: bg || 'var(--slib-bg-main, #F7FAFC)',
      iconColor: color || 'var(--slib-text-secondary, #4A5568)',
      cardBg: 'var(--slib-bg-card, #ffffff)'
    },
    primary: {
      iconBg: 'var(--slib-primary-subtle, #FFF7F2)',
      iconColor: 'var(--slib-primary, #FF751F)',
      cardBg: 'var(--slib-bg-card, #ffffff)'
    },
    success: {
      iconBg: 'var(--slib-status-success-bg, #E8F5E9)',
      iconColor: 'var(--slib-status-success, #388E3C)',
      cardBg: 'var(--slib-bg-card, #ffffff)'
    },
    warning: {
      iconBg: 'var(--slib-status-warning-bg, #FFF3E0)',
      iconColor: 'var(--slib-status-warning, #FF9800)',
      cardBg: 'var(--slib-bg-card, #ffffff)'
    },
    error: {
      iconBg: 'var(--slib-status-error-bg, #FFEBEE)',
      iconColor: 'var(--slib-status-error, #D32F2F)',
      cardBg: 'var(--slib-bg-card, #ffffff)'
    }
  };

  const styles = variantStyles[variant] || variantStyles.default;
  const finalIconBg = bg || styles.iconBg;
  const finalIconColor = color || styles.iconColor;

  const getTrendIcon = () => {
    if (!trend) return null;
    switch (trend) {
      case 'up': return <TrendingUp size={14} />;
      case 'down': return <TrendingDown size={14} />;
      default: return <Minus size={14} />;
    }
  };

  const getTrendColor = () => {
    switch (trend) {
      case 'up': return 'var(--slib-status-success, #388E3C)';
      case 'down': return 'var(--slib-status-error, #D32F2F)';
      default: return 'var(--slib-text-muted, #A0AEC0)';
    }
  };

  return (
    <div 
      style={{
        background: styles.cardBg,
        borderRadius: '16px',
        boxShadow: '0 4px 20px rgba(26, 26, 26, 0.06)',
        padding: '24px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '16px',
        transition: 'all 0.3s ease',
        cursor: 'default',
        border: '1px solid transparent',
        position: 'relative',
        overflow: 'hidden'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.boxShadow = '0 8px 30px rgba(26, 26, 26, 0.12)';
        e.currentTarget.style.transform = 'translateY(-2px)';
        e.currentTarget.style.borderColor = 'var(--slib-border-light, #E2E8F0)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.boxShadow = '0 4px 20px rgba(26, 26, 26, 0.06)';
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.borderColor = 'transparent';
      }}
    >
      {/* Decorative gradient */}
      <div style={{
        position: 'absolute',
        top: 0,
        right: 0,
        width: '120px',
        height: '120px',
        background: `linear-gradient(135deg, ${finalIconBg}40 0%, transparent 60%)`,
        borderRadius: '0 16px 0 100%',
        pointerEvents: 'none'
      }} />

      {/* Icon */}
      <div style={{ 
        background: finalIconBg, 
        color: finalIconColor,
        width: '56px',
        height: '56px',
        borderRadius: '14px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        boxShadow: `0 4px 12px ${finalIconBg}80`
      }}>
        {icon}
      </div>

      {/* Content */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: '28px',
          fontWeight: '700',
          color: 'var(--slib-text-primary, #1A1A1A)',
          lineHeight: '1.2',
          marginBottom: '4px'
        }}>{value}</div>
        
        <div style={{
          fontSize: '13px',
          color: 'var(--slib-text-secondary, #4A5568)',
          fontWeight: '500',
          lineHeight: '1.4'
        }}>{label}</div>

        {trend && trendValue && (
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px',
            marginTop: '12px',
            padding: '4px 10px',
            borderRadius: '20px',
            fontSize: '12px',
            fontWeight: '600',
            color: getTrendColor(),
            background: trend === 'up' 
              ? 'var(--slib-status-success-bg, #E8F5E9)'
              : trend === 'down'
              ? 'var(--slib-status-error-bg, #FFEBEE)'
              : 'var(--slib-bg-main, #F7FAFC)'
          }}>
            {getTrendIcon()}
            <span>{trendValue}</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCard;
