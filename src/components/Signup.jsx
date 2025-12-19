import "../styles/signup.css";
import googleLogo from "../assets/google.png";
import { useState } from "react";

function Signup({ switchToLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const isFptEmail = email.endsWith("@fpt.edu.vn");

  return (
    <div className="login-card">
      <h2 className="title">Đăng ký tài khoản</h2>

      <div className="row-2">
        <input type="text" placeholder="Tên" />
        <input type="text" placeholder="Họ (tuỳ chọn)" />
      </div>

      <div className="input-icon">
        <input type="date" />
      </div>

      <input
        className="full-input"
        type="text"
        placeholder="MSSV (VD: DE170706)"
      />

      <input
        className={`full-input ${email && !isFptEmail ? "error" : ""}`}
        type="email"
        placeholder="Email FPT (fpt.edu.vn)"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      {email && !isFptEmail && (
        <p className="error-text">Email phải là @fpt.edu.vn</p>
      )}

      <div className="input-icon">
        <input
          type={showPassword ? "text" : "password"}
          placeholder="Mật khẩu"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <span
          className="icon"
          onClick={() => setShowPassword(!showPassword)}
        >
          👁
        </span>
      </div>

      <button className="btn-login" disabled={!isFptEmail || !password}>
        Đăng ký
      </button>

      <div className="divider">Hoặc</div>

      <button className="btn-google">
        <img src={googleLogo} alt="Google" />
        <span>Đăng ký bằng Google</span>
      </button>

      <p className="register">
        Đã có tài khoản?{" "}
        <span onClick={switchToLogin}>Đăng nhập</span>
      </p>
    </div>
  );
}

export default Signup;
