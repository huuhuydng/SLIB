import { useState } from "react";
import Login from "./auth/Login";
import ForgotPassword from "./auth/ForgotPassword";
import logo from "../assets/logo.png";
import "../styles/Auth.css";

function AuthPage({ onLogin }) {
  const [showForgotPassword, setShowForgotPassword] = useState(false);

  return (
    <div className={`auth-page ${showForgotPassword ? "forgot-password-mode" : ""}`}>
      <div className="auth-card">

        {/* ============ LOGIN PANEL ============ */}
        <div className="panel panel-login">
          <Login
            onLogin={onLogin}
            onForgotPassword={() => setShowForgotPassword(true)}
          />
        </div>

        {/* ============ FORGOT PASSWORD PANEL ============ */}
        <div className="panel panel-forgot">
          <ForgotPassword
            onSwitch={() => setShowForgotPassword(false)}
          />
        </div>

        {/* ============ OVERLAY LOGO ============ */}
        <div className="overlay">
          <img src={logo} alt="Logo" className="slib-logo-img" />
        </div>

      </div>
    </div>
  );
}

export default AuthPage;