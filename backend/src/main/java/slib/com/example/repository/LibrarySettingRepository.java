package slib.com.example.repository;

/**
 * Stub repository to avoid JPA scanning for a library_settings table.
 * All library settings are now kept in-memory via LibrarySettingService.
 */
public final class LibrarySettingRepository {
    private LibrarySettingRepository() {
    }
}
