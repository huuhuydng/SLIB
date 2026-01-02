package vn.edu.dlu.slib


import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import java.nio.charset.Charset

class MyHostApduService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            Log.e("HCE", "Received NULL APDU")
            showToast("❌ NFC Error: NULL APDU")
            return hexStringToByteArray("6F00")
        }

        Log.d("HCE", "Received APDU: " + toHex(commandApdu))
        
        // Kiểm tra xem có phải SELECT AID không (00 A4 04 00)
        if (commandApdu.size >= 4 && 
            commandApdu[0] == 0x00.toByte() && 
            commandApdu[1] == 0xA4.toByte() && 
            commandApdu[2] == 0x04.toByte()) {
            
            Log.d("HCE", "✅ SELECT AID Command detected!")

            // Đọc mã sinh viên từ SharedPreferences
            val prefs = getSharedPreferences("hce_prefs", Context.MODE_PRIVATE)
            val studentCode = prefs.getString("HCE_STUDENT_CODE", null)
            
            if (studentCode == null || studentCode.isEmpty()) {
                Log.e("HCE", "❌ No student code found!")
                showToast("❌ NFC: Chưa đăng nhập")
                return hexStringToByteArray("6A82") // File not found
            }

            showToast("✅ NFC: Gửi token $studentCode")
            
            // Trả về Token + Status OK (90 00)
            val tokenBytes = studentCode.toByteArray(Charset.forName("UTF-8"))
            val statusBytes = hexStringToByteArray("9000")
            
            val response = ByteArray(tokenBytes.size + statusBytes.size)
            System.arraycopy(tokenBytes, 0, response, 0, tokenBytes.size)
            System.arraycopy(statusBytes, 0, response, tokenBytes.size, statusBytes.size)
            
            Log.d("HCE", "Sending Token: $studentCode")
            Log.d("HCE", "Response HEX: " + toHex(response))
            return response
        }
        
        // Lệnh không được hỗ trợ
        Log.w("HCE", "Unsupported APDU command")
        showToast("⚠️ NFC: Unknown command")
        return hexStringToByteArray("6D00") // Instruction not supported
    }
    
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d("HCE", "Deactivated: $reason")
    }

    // --- Các hàm tiện ích ---
    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}