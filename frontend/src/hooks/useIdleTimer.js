import { useEffect, useState, useCallback, useRef } from 'react';

export const useIdleTimer = (idleTime = 5000) => { // Thời gian mặc định khi không tương tác
  const [isIdle, setIsIdle] = useState(false);
  const idleTimer = useRef(null);

  const resetTimer = useCallback(() => {
    if (idleTimer.current) clearTimeout(idleTimer.current);
    setIsIdle(false);

    idleTimer.current = setTimeout(() => {
      setIsIdle(true);
    }, idleTime);
  }, [idleTime]);

  useEffect(() => {
    // Các sự kiện để detect tương tác
    const events = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'];

    // Set up initial timeout
    idleTimer.current = setTimeout(() => {
      setIsIdle(true);
    }, idleTime);

    events.forEach(event => {
      // Sử dụng capture: true để bắt sự kiện scroll tốt hơn
      window.addEventListener(event, resetTimer, { capture: true });
    });

    return () => {
      events.forEach(event => {
        window.removeEventListener(event, resetTimer, { capture: true });
      });
      if (idleTimer.current) clearTimeout(idleTimer.current);
    };
  }, [resetTimer, idleTime]);

  return isIdle;
};