import { useState, useRef } from "react";
import { useToast } from "../../components/common/ToastProvider";
import librarianService from "../../services/librarian/librarianService";
import "../../styles/Auth.css";

function ForgotPassword({ onSwitch }) {
  const toast = useToast();
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const otpInputs = useRef([]);

  // ============ XỬ LÝ OTP INPUT ============
  const handleOtpChange = (index, value) => {
    if (value && !/^\d$/.test(value)) return;

    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    if (value && index < 5) {
      otpInputs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === "Backspace" && !otp[index] && index > 0) {
      otpInputs.current[index - 1]?.focus();
    }
  };

  // ============ BƯỚC 1: GỬI OTP ============
  const handleSendOtp = async (e) => {
    e.preventDefault();

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !email.trim()) {
      toast.warning("Vui lòng nhập email!");
      return;
    }

    if (!emailRegex.test(email)) {
      toast.warning("Email không đúng định dạng!");
      return;
    }

    try {
      setLoading(true);

      const cleanEmail = email.trim().toLowerCase();
      console.log('🟡 [ForgotPassword] Sending OTP to:', cleanEmail);

      const response = await librarianService.forgotPassword(cleanEmail);

      console.log('✅ [ForgotPassword] Response:', response);
      toast.success(`${response.message || 'Mã OTP đã được gửi đến email của bạn!'}`);
      setStep(2);

    } catch (err) {
      console.error('❌ [ForgotPassword] Full error:', err);

      let errorMessage = "Không thể gửi OTP. Vui lòng thử lại.";

      if (err.response) {
        if (err.response.status === 400) {
          // Business errors from backend (email not found, no email, etc.)
          errorMessage = err.response.data?.message || "Email không hợp lệ";
        } else if (err.response.status === 403) {
          errorMessage = "Lỗi xác thực. Vui lòng thử lại.";
        } else if (err.response.status === 404) {
          errorMessage = "API không tồn tại. Vui lòng kiểm tra backend.";
        } else if (err.response.status === 500) {
          errorMessage = "Lỗi server. Vui lòng thử lại sau.";
        } else {
          errorMessage = err.response.data?.message ||
            err.response.data ||
            `Lỗi ${err.response.status}`;
        }
      } else if (err.request) {
        errorMessage = "Không kết nối được server. Vui lòng kiểm tra kết nối.";
      }

      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // ============ BƯỚC 2: XÁC THỰC OTP ============
  const handleVerifyOtp = async (e) => {
    e.preventDefault();

    const otpCode = otp.join("");

    if (otpCode.length !== 6) {
      toast.warning("Vui lòng nhập đủ 6 số OTP!");
      return;
    }

    try {
      setLoading(true);
      console.log('🟡 [ForgotPassword] Verifying OTP:', otpCode);

      const response = await librarianService.verifyOtp(
        email.trim().toLowerCase(),
        otpCode,
        'recovery'
      );

      console.log('✅ [ForgotPassword] Verify response:', response);
      toast.success("Xác thực thành công! Vui lòng đặt mật khẩu mới.");
      setStep(3);

    } catch (err) {
      console.error('❌ [ForgotPassword] Verify error:', err);

      const errorMessage = err.response?.data?.message ||
        err.response?.data ||
        "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại.";

      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // ============ GỬI LẠI OTP ============
  const handleResendOtp = async () => {
    try {
      setLoading(true);
      setOtp(["", "", "", "", "", ""]);

      console.log('🟡 [ForgotPassword] Resending OTP');

      const response = await librarianService.resendOtp(
        email.trim().toLowerCase(),
        'recovery'
      );

      console.log('✅ [ForgotPassword] Resend response:', response);
      toast.success(response.message || "Mã OTP mới đã được gửi!");

    } catch (err) {
      console.error('❌ [ForgotPassword] Resend error:', err);

      const errorMessage = err.response?.data?.message ||
        err.response?.data ||
        "Không thể gửi lại OTP. Vui lòng thử lại.";

      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // ============ BƯỚC 3: ĐẶT MẬT KHẨU MỚI ============
  const handleResetPassword = async (e) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      toast.warning("Mật khẩu xác nhận không khớp!");
      return;
    }

    if (newPassword.length < 6) {
      toast.warning("Mật khẩu phải có ít nhất 6 ký tự!");
      return;
    }

    try {
      setLoading(true);
      console.log('🟡 [ForgotPassword] Resetting password');

      const response = await librarianService.updatePassword(newPassword);

      console.log('✅ [ForgotPassword] Reset response:', response);
      toast.success("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.");
      onSwitch();

    } catch (err) {
      console.error('❌ [ForgotPassword] Reset error:', err);

      const errorMessage = err.response?.data?.message ||
        err.response?.data ||
        err.message ||
        "Không thể đặt lại mật khẩu. Vui lòng thử lại.";

      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-form-container">
      <div className="auth-form-box">

        {/* BƯỚC 1: NHẬP EMAIL */}
        {step === 1 && (
          <>
            <h2 className="form-title">Quên mật khẩu</h2>
            <p style={{ textAlign: 'center', fontSize: '12px', color: '#666', marginBottom: '15px' }}>
              Nhập email để nhận mã xác thực
            </p>

            <form onSubmit={handleSendOtp}>
              <div className="input-group">
                <input
                  className="input-field"
                  type="email"
                  placeholder="Email (username@fpt.edu.vn)"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={loading}
                  required
                />
              </div>

              <button
                type="submit"
                className="btn-primary"
                disabled={loading}
              >
                {loading ? "Đang gửi..." : "Gửi mã OTP"}
              </button>
            </form>

            <p className="switch-text">
              Đã có tài khoản?{" "}
              <span className="switch-link" onClick={onSwitch}>Đăng nhập</span>
            </p>
          </>
        )}

        {/* BƯỚC 2: XÁC THỰC OTP */}
        {step === 2 && (
          <>
            <h2 className="form-title">Xác thực OTP</h2>
            <p style={{ textAlign: 'center', fontSize: '12px', color: '#666', marginBottom: '5px' }}>
              Mã OTP đã được gửi đến
            </p>
            <p style={{ textAlign: 'center', fontSize: '13px', color: '#333', fontWeight: '600', marginBottom: '15px' }}>
              {email}
            </p>

            <form onSubmit={handleVerifyOtp}>
              <div className="otp-container">
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    ref={(el) => (otpInputs.current[index] = el)}
                    className="otp-input"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    maxLength="1"
                    value={digit}
                    onChange={(e) => handleOtpChange(index, e.target.value)}
                    onKeyDown={(e) => handleKeyDown(index, e)}
                    disabled={loading}
                    autoFocus={index === 0}
                  />
                ))}
              </div>

              <button
                type="submit"
                className="btn-primary"
                disabled={loading}
              >
                {loading ? "Đang xác thực..." : "Xác nhận"}
              </button>
            </form>

            <p className="switch-text" style={{ marginTop: "10px" }}>
              <span
                className="switch-link"
                onClick={handleResendOtp}
                style={{ cursor: loading ? 'not-allowed' : 'pointer' }}
              >
                Gửi lại OTP
              </span>
            </p>

            <p className="switch-text">
              <span className="switch-link" onClick={onSwitch}>Quay lại đăng nhập</span>
            </p>
          </>
        )}

        {/* BƯỚC 3: ĐẶT MẬT KHẨU MỚI */}
        {step === 3 && (
          <>
            <h2 className="form-title">Đặt mật khẩu mới</h2>
            <p style={{ textAlign: 'center', fontSize: '12px', color: '#666', marginBottom: '15px' }}>
              Nhập mật khẩu mới của bạn
            </p>

            <form onSubmit={handleResetPassword}>
              <div className="input-group">
                <input
                  className="input-field"
                  type="password"
                  placeholder="Mật khẩu mới (tối thiểu 6 ký tự)"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  disabled={loading}
                  required
                  minLength={6}
                />
              </div>

              <div className="input-group">
                <input
                  className="input-field"
                  type="password"
                  placeholder="Xác nhận mật khẩu"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  disabled={loading}
                  required
                  minLength={6}
                />
              </div>

              <button
                type="submit"
                className="btn-primary"
                disabled={loading}
              >
                {loading ? "Đang cập nhật..." : "Đặt lại mật khẩu"}
              </button>
            </form>
          </>
        )}

      </div>
    </div>
  );
}

export default ForgotPassword;