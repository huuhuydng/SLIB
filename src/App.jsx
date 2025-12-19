import { useState } from "react";
import Login from "./components/Login";
import Signup from "./components/Signup";
import "./styles/auth.css";

function App() {
  const [isSignup, setIsSignup] = useState(false);

  return (
    <div className={`container ${isSignup ? "right-panel-active" : ""}`}>
      {/* SIGN UP */}
      <div className="form-container sign-up-container">
        <Signup switchToLogin={() => setIsSignup(false)} />
      </div>

      {/* SIGN IN */}
      <div className="form-container sign-in-container">
        <Login switchToSignup={() => setIsSignup(true)} />
      </div>

      {/* OVERLAY */}
      <div className="overlay-container">
        <div className="overlay">
          <div className="overlay-panel overlay-left">
            <h1>Welcome Back!</h1>
            <p>Đã có tài khoản? Đăng nhập tại đây</p>
            <button onClick={() => setIsSignup(false)}>
              Đăng nhập
            </button>
          </div>

          <div className="overlay-panel overlay-right">
            <h1>Hello, Friend!</h1>
            <p>Chưa có tài khoản? Đăng ký ngay</p>
            <button onClick={() => setIsSignup(true)}>
              Đăng ký
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
