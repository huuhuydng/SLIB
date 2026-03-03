import React from 'react';
import { Delete, X } from 'lucide-react';
import './VirtualKeyboard.css';

/**
 * VirtualKeyboard - Bàn phím ảo cho Kiosk MSSV input
 * Luôn luôn chữ hoa, chỉ có số và chữ DExxxxxx
 */
const VirtualKeyboard = ({ value, onChange, onSubmit, placeholder = '' }) => {
  // Bố cục bàn phím
  const rows = [
    ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'],
    ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'],
    ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'],
    ['Z', 'X', 'C', 'V', 'B', 'N', 'M'],
  ];

  const handleKeyPress = (key) => {
    if (key === 'BACKSPACE') {
      onChange(value.slice(0, -1));
    } else if (key === 'CLEAR') {
      onChange('');
    } else if (key === 'SUBMIT') {
      onSubmit?.();
    } else {
      // Luôn luôn chữ hoa cho MSSV
      onChange(value + key.toUpperCase());
    }
  };

  return (
    <div className="virtualKeyboard__wrapper">
      {/* Display */}
      <div className="virtualKeyboard__display">
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className="virtualKeyboard__input"
          readOnly
        />
      </div>

      {/* Keyboard */}
      <div className="virtualKeyboard__container">
        {/* Number Row */}
        <div className="virtualKeyboard__row">
          {rows[0].map((key) => (
            <button
              key={key}
              className="virtualKeyboard__key virtualKeyboard__key--number"
              onClick={() => handleKeyPress(key)}
            >
              {key}
            </button>
          ))}
          <button
            className="virtualKeyboard__key virtualKeyboard__key--delete"
            onClick={() => handleKeyPress('BACKSPACE')}
            title="Delete"
          >
            <X size={20} />
          </button>
        </div>

        {/* Letter Row 1 */}
        <div className="virtualKeyboard__row">
          {rows[1].map((key) => (
            <button
              key={key}
              className="virtualKeyboard__key"
              onClick={() => handleKeyPress(key)}
            >
              {key}
            </button>
          ))}
        </div>

        {/* Letter Row 2 */}
        <div className="virtualKeyboard__row">
          {rows[2].map((key) => (
            <button
              key={key}
              className="virtualKeyboard__key"
              onClick={() => handleKeyPress(key)}
            >
              {key}
            </button>
          ))}
        </div>

        {/* Letter Row 3 + Action Keys */}
        <div className="virtualKeyboard__row">
          {rows[3].map((key) => (
            <button
              key={key}
              className="virtualKeyboard__key"
              onClick={() => handleKeyPress(key)}
            >
              {key}
            </button>
          ))}
          <button
            className="virtualKeyboard__key virtualKeyboard__key--clear"
            onClick={() => handleKeyPress('CLEAR')}
            title="Clear"
          >
            <Delete size={20} />
          </button>
        </div>

        {/* Submit Button */}
        <div className="virtualKeyboard__row">
          <button
            className="virtualKeyboard__key virtualKeyboard__key--submit"
            onClick={() => handleKeyPress('SUBMIT')}
          >
            XÁC NHẬN
          </button>
        </div>
      </div>
    </div>
  );
};

export default VirtualKeyboard;
