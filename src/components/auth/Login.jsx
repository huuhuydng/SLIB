import "../styles/login.css";
import googleLogo from "../assets/google.png";
import { useState } from "react";

function Login({ switchToSignup }) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="login-card">
      <h2 className="title">Login</h2>

      <div className="form-group">
        <label>Email</label>
        <input type="email" placeholder="username@fpt.edu.vn" />
      </div>

      <div className="form-group">
        <label>Password</label>

        <div className="input-icon">
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Password"
          />
          <span
            className="icon"
            onClick={() => setShowPassword(!showPassword)}
          >
            👁
          </span>
        </div>
      </div>

      <div className="forgot">Quên mật khẩu</div>

      <button className="btn-login">Sign in</button>

      <div className="divider">Hoặc tiếp tục bằng</div>

      <button className="btn-google">
        <img src={googleLogo} alt="Google" />
        <span>Google</span>
      </button>

      <p className="register">
        Chưa có tài khoản?{" "}
        <span onClick={switchToSignup}>Đăng ký ngay</span>
      </p>
    </div>
  );
}

export default Login;
