import React from 'react';

const PageLayout = ({ children, maxWidth = '1400px' }) => {
  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#f9fafb'
    }}>
      <div style={{
        maxWidth: maxWidth,
        margin: '0 auto',
        padding: '2rem',
        paddingTop: '1.5rem'
      }}>
        {children}
      </div>
    </div>
  );
};

export default PageLayout;
