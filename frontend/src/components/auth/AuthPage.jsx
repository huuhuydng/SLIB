import { useState } from "react";
import Login from "./Login";
import ForgotPassword from "./ForgotPassword";
import "../../styles/Auth.css";

function AuthPage({ onLogin }) {
  const [showForgotPassword, setShowForgotPassword] = useState(false);

  if (showForgotPassword) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="panel panel-forgot" style={{ opacity: 1, width: '100%', position: 'relative' }}>
            <ForgotPassword
              onSwitch={() => setShowForgotPassword(false)}
            />
          </div>
        </div>
      </div>
    );
  }

  return (
    <Login
      onLogin={onLogin}
      onForgotPassword={() => setShowForgotPassword(true)}
    />
  );
}

export default AuthPage;
