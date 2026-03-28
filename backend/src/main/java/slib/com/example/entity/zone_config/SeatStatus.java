package slib.com.example.entity.zone_config;

public enum SeatStatus {
    AVAILABLE,
    UNAVAILABLE,
    BOOKED,
    CONFIRMED, // Sinh viên đã quét NFC, đang ngồi tại ghế
    HOLDING // Ghế đang được giữ tạm thời (5 phút)
}
