package slib.com.example.entity.users;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) {
            return null;
        }
        // Always store as uppercase in database
        return role.name().toUpperCase();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        // Handle both uppercase and lowercase from database
        try {
            return Role.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log warning and return default
            System.err.println("Unknown role value in database: " + dbData + ". Defaulting to STUDENT.");
            return Role.STUDENT;
        }
    }
}
