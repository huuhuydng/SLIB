package slib.com.example.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SeatStatusConverter implements AttributeConverter<SeatStatus, String> {

    @Override
    public String convertToDatabaseColumn(SeatStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public SeatStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return SeatStatus.valueOf(dbData);
    }
}
